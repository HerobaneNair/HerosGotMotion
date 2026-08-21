package hero.bane;

import com.google.common.util.concurrent.AtomicDouble;
import hero.bane.command.HerosGotMotionCommand;
import hero.bane.config.HerosGotMotionConfig;
import hero.bane.config.HerosGotMotionConfig.Data;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public class HerosGotMotion implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("herosgotmotion");
    private static final Identifier MOTION_TEXT_ELEMENT = Identifier.fromNamespaceAndPath("herosgotmotion", "motion_text");
    public static boolean enabled = true;
    public static boolean smoothBar = true;
    public static final AtomicBoolean showBar = new AtomicBoolean(true);
    public static final AtomicBoolean showTexts = new AtomicBoolean(false);
    public static final AtomicDouble speed = new AtomicDouble(0.0);
    public static final AtomicDouble effMods = new AtomicDouble(0);
    public static int textOpacity = 255;
    public static final double DEFAULT_SPEED_OFFSET_X = 0, DEFAULT_SPEED_OFFSET_Y = 20;
    public static double speedOffsetX = DEFAULT_SPEED_OFFSET_X, speedOffsetY = DEFAULT_SPEED_OFFSET_Y;

    public static int hideTicksAfterXp = 25;
    public static int hideBarUntilTick = 0;
    public static int prevTotalExp = -1;

    @Override
    public void onInitializeClient() {
        applyConfig(HerosGotMotionConfig.load());

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            prevTotalExp = -1;
            hideBarUntilTick = 0;
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                if (prevTotalExp == Integer.MIN_VALUE) {
                    prevTotalExp = mc.player.totalExperience;
                } else {
                    int cur = mc.player.totalExperience;
                    if (cur > prevTotalExp && hideTicksAfterXp > 0) {
                        hideBarUntilTick = mc.player.tickCount + hideTicksAfterXp;
                    }
                    prevTotalExp = cur;
                }
            }
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> HerosGotMotionCommand.register(dispatcher));

        HudElementRegistry.addLast(MOTION_TEXT_ELEMENT, (GuiGraphics context, DeltaTracker tickCounter) -> {
            if (!enabled) return;
            Minecraft client = Minecraft.getInstance();
            if (client.options.hideGui) return;
            if (!showTexts.get()) return;

            int cx = context.guiWidth() / 2;
            int cy = context.guiHeight() / 2;

            Font tr = client.font;
            String speedText = String.format("%.2f bl/s", speed.get());
            int sw = tr.width(speedText);
            context.drawString(tr, Component.literal(speedText), cx - (sw / 2) + (int) speedOffsetX, cy + (int) speedOffsetY, getSpeedColor(speed.get() / 3.663, effMods.get()));
        });
    }

    public static void applyConfig(Data cfg) {
        speedOffsetX = cfg.speedOffsetX;
        speedOffsetY = cfg.speedOffsetY;
        smoothBar = cfg.smoothBar;
        textOpacity = cfg.textOpacity;
        hideTicksAfterXp = cfg.hideTicksAfterXp;
        HerosGotMotionCommand.setModeFromConfig(cfg.mode);
    }

    private static int opaque(int rgb) {
        int alpha = Math.clamp(HerosGotMotion.textOpacity, 26, 255);
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }


    private static int getSpeedColor(double value, double eff) {
        if (value <= -2.62 * eff) {
            return opaque(0x0000FF);
        } else if (value < -1.53 * eff) {
            return getColor(value,
                    -2.62 * eff, opaque(0x0000FF),
                    -2.62 * eff, opaque(0x0000FF),
                    -1.53 * eff, opaque(0xFFAACC));
        } else if (value < -0.35 * eff) {
            return getColor(value,
                    -1.53 * eff, opaque(0xFFAACC),
                    -1.18 * eff, opaque(0x9900FF),
                    -0.35 * eff, opaque(0xFF0000));
        } else if (value < 0.35 * eff) {
            return getColor(value,
                    -0.35 * eff, opaque(0xFF0000),
                    0.0, opaque(0xFFFFFF),
                    0.35 * eff, opaque(0xFFA500));
        } else if (value < 1.53 * eff) {
            return getColor(value,
                    0.35 * eff, opaque(0xFFA500),
                    1.18 * eff, opaque(0xFFFF00),
                    1.53 * eff, opaque(0x00FF00));
        } else if (value < 2.62 * eff) {
            return getColor(value,
                    1.53 * eff, opaque(0x00FF00),
                    2.62 * eff, opaque(0x00FFFF),
                    2.62 * eff, opaque(0x00FFFF));
        } else {
            return opaque(0x00FFFF);
        }
    }

    private static int getColor(double value, double min, int minColor, double median, int medianColor, double max, int maxColor) {
        value = Mth.clamp(value, min, max);
        return value < median
                ? ARGB.srgbLerp((float) ((value - min) / (median - min)), minColor, medianColor)
                : ARGB.srgbLerp((float) ((value - median) / (max - median)), medianColor, maxColor);
    }
}
