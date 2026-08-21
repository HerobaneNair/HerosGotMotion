package hero.bane.render;

import hero.bane.HerosGotMotion;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class SprintBarRenderer {
    private static final Identifier SPRINT_BAR_BACKGROUND_TEXTURE = Identifier.fromNamespaceAndPath("herosgotmotion", "hud/sprint_bar_background");
    private static final Identifier SPRINT_BAR_PROGRESS_TEXTURE = Identifier.fromNamespaceAndPath("herosgotmotion", "hud/sprint_bar_progress");
    private static final Identifier SPRINT_BAR_REGRESS_TEXTURE = Identifier.fromNamespaceAndPath("herosgotmotion", "hud/sprint_bar_regress");
    private static final Identifier SPRINT_BAR_PROGRESS_2_TEXTURE = Identifier.fromNamespaceAndPath("herosgotmotion", "hud/sprint_bar_progress_2");
    private static final Identifier SPRINT_BAR_REGRESS_2_TEXTURE = Identifier.fromNamespaceAndPath("herosgotmotion", "hud/sprint_bar_regress_2");

    private static float mainBarFill = 0f;
    private static float secondBarFill = 0f;

    private SprintBarRenderer() {}

    public static boolean render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!HerosGotMotion.enabled) return false;
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) return false;

        if (player.tickCount < HerosGotMotion.hideBarUntilTick) {
            return false;
        }

        float yaw = player.getYRot() * ((float) Math.PI / 180F);
        double facingX = -Math.sin(yaw);
        double facingZ = Math.cos(yaw);

        double dx = player.getX() - player.xOld;
        double dz = player.getZ() - player.zOld;
        double dotProduct = dx * facingX + dz * facingZ;
        double blocksPerSecond = dotProduct * 20.0;

        int speedLevel = 0;
        if (player.hasEffect(MobEffects.SPEED)) {
            MobEffectInstance inst = player.getEffect(MobEffects.SPEED);
            if (inst != null) speedLevel = inst.getAmplifier() + 1;
        }

        int slowLevel = 0;
        if (player.hasEffect(MobEffects.SLOWNESS)) {
            MobEffectInstance inst = player.getEffect(MobEffects.SLOWNESS);
            if (inst != null) slowLevel = inst.getAmplifier() + 1;
        }

        float effectModifiers = (1F + 0.2F * speedLevel) * (1F - 0.15F * slowLevel);

        HerosGotMotion.effMods.set(effectModifiers);
        HerosGotMotion.speed.set(blocksPerSecond);

        if (!HerosGotMotion.showBar.get()) return false;

        float maxSprintSpeed = 5.61234F * effectModifiers;
        float scaled = (float) (blocksPerSecond / maxSprintSpeed);
        float fraction = Mth.clamp(scaled, -1.0F, 1.0F);

        int width = 182;
        int fill = (int) (Math.abs(fraction) * (width + 1));
        int x = (graphics.guiWidth() - 182) / 2;
        int y = graphics.guiHeight() - 29;

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPRINT_BAR_BACKGROUND_TEXTURE, x, y, width, 5);

        int target1 = Mth.clamp(fill, 0, width);
        int target2Raw = (int) ((Math.abs(scaled) - 1.0F) / 1.1815624F * 0.75 * width);
        int target2 = Mth.clamp(target2Raw, 0, width);

        float alpha = 1.0f;
        if (HerosGotMotion.smoothBar) {
            float tp = deltaTracker.getGameTimeDeltaPartialTick(true);
            alpha = Mth.clamp(tp, 0.0f, 1.0f);
        }
        mainBarFill = mainBarFill + (target1 - mainBarFill) * alpha;
        secondBarFill = secondBarFill + (target2 - secondBarFill) * alpha;

        boolean goingForwards = fraction >= 0f;
        int draw1 = Mth.floor(mainBarFill);
        if (draw1 > 0) {
            Identifier texture = goingForwards ? SPRINT_BAR_PROGRESS_TEXTURE : SPRINT_BAR_REGRESS_TEXTURE;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, texture, width, 5, 0, 0, x, y, draw1, 5);
        }

        int draw2 = Mth.floor(secondBarFill);
        if (draw2 > 0) {
            Identifier texture = goingForwards ? SPRINT_BAR_PROGRESS_2_TEXTURE : SPRINT_BAR_REGRESS_2_TEXTURE;
            int u2 = width - draw2;
            int x2 = x + (width - draw2);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, texture, width, 5, u2, 0, x2, y, draw2, 5);
        }

        return true;
    }
}
