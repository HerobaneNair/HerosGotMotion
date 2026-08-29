package hero.bane;

import hero.bane.config.ChartPosition;
import hero.bane.config.HerosGotMotionConfig;
import hero.bane.config.HerosGotMotionConfig.Data;
import hero.bane.config.SpeedAnchors;
import hero.bane.render.SpeedColors;
import hero.bane.render.SprintBarRenderer;
import hero.bane.render.VelocityChart;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HerosGotMotion implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("herosgotmotion");
    private static final Identifier MOTION_TEXT_ELEMENT = Identifier.fromNamespaceAndPath("herosgotmotion", "motion_text");
    private static final Identifier MOTION_CHART_ELEMENT = Identifier.fromNamespaceAndPath("herosgotmotion", "motion_chart");
    public static boolean enabled = true;
    public static boolean smoothBar = true;
    public static boolean showBar = true;
    public static boolean showTexts = false;
    public static boolean showChart = false;
    public static double speed = 0.0;
    public static double speedScale = 1.0;
    public static int textOpacity = 255;
    public static final double DEFAULT_SPEED_OFFSET_X = 0, DEFAULT_SPEED_OFFSET_Y = 0;
    public static double speedOffsetX = DEFAULT_SPEED_OFFSET_X, speedOffsetY = DEFAULT_SPEED_OFFSET_Y;

    public static ChartPosition chartPosition = ChartPosition.DEFAULT;
    public static double chartPaddingX = 0, chartPaddingY = 0;

    public static boolean totalVelocity = false;
    public static boolean objectiveVelocity = false;
    public static boolean relativeToSpeed = true;
    public static final SpeedAnchors anchors = new SpeedAnchors();
    public static int hideTicksAfterXp = 25;
    public static int hideBarUntilTick = 0;
    public static int prevTotalExp = -1;

    @Override
    public void onInitializeClient() {
        applyConfig(HerosGotMotionConfig.load());

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            prevTotalExp = -1;
            hideBarUntilTick = 0;
            VelocityChart.reset();
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                if (showChart && !mc.isPaused()) {
                    VelocityChart.push(SprintBarRenderer.relativeVelocity(mc.player),
                            SprintBarRenderer.movementSpeedScale(mc.player));
                }
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
        HudElementRegistry.addLast(MOTION_TEXT_ELEMENT, (GuiGraphics context, DeltaTracker tickCounter) -> {
            if (!enabled || !showTexts) return;
            Minecraft client = Minecraft.getInstance();
            if (client.options.hideGui) return;

            int cx = context.guiWidth() / 2;
            int cy = context.guiHeight() / 2;

            Font tr = client.font;
            String speedText = String.format("%.2f bl/s", speed);
            int sw = tr.width(speedText);
            context.drawString(tr, Component.literal(speedText), cx - (sw / 2) + (int) speedOffsetX, cy + (int) speedOffsetY, getSpeedColor(speed, speedScale));
        });

        HudElementRegistry.addLast(MOTION_CHART_ELEMENT,
                (GuiGraphics context, DeltaTracker tickCounter) -> VelocityChart.renderHud(context));
    }

    public static void setShowChart(boolean show) {
        if (showChart == show) return;
        showChart = show;
        if (!show) VelocityChart.reset();
    }

    public static void applyConfig(Data cfg) {
        showBar = cfg.showBar;
        showTexts = cfg.showTexts;
        speedOffsetX = cfg.speedOffsetX;
        speedOffsetY = cfg.speedOffsetY;
        chartPosition = ChartPosition.byName(cfg.chartPosition);
        chartPaddingX = cfg.chartPaddingX;
        chartPaddingY = cfg.chartPaddingY;
        smoothBar = cfg.smoothBar;
        textOpacity = cfg.textOpacity;
        totalVelocity = cfg.totalVelocity;
        objectiveVelocity = cfg.objectiveVelocity;
        relativeToSpeed = cfg.relativeToSpeed;
        cfg.readAnchorsInto(anchors);
        hideTicksAfterXp = cfg.hideTicksAfterXp;
    }

    private static int opaque(int rgb) {
        int alpha = Math.clamp(HerosGotMotion.textOpacity, 26, 255);
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }


    private static int getSpeedColor(double blocksPerSecond, double scale) {
        return opaque(SpeedColors.rgb(blocksPerSecond / Math.max(scale, 0.05), anchors.resolved()));
    }
}
