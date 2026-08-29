package hero.bane.render;

import hero.bane.HerosGotMotion;
import hero.bane.config.SpeedAnchors;
import hero.bane.config.SpeedAnchors.Anchor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class SprintBarRenderer {
    private static final Identifier SPRINT_BAR_BACKGROUND_TEXTURE = Identifier.fromNamespaceAndPath("herosgotmotion", "hud/sprint_bar_background");
    private static final Identifier SPRINT_BAR_PROGRESS_TEXTURE = Identifier.fromNamespaceAndPath("herosgotmotion", "hud/sprint_bar_progress");
    private static final Identifier SPRINT_BAR_REGRESS_TEXTURE = Identifier.fromNamespaceAndPath("herosgotmotion", "hud/sprint_bar_regress");
    private static final Identifier SPRINT_BAR_PROGRESS_2_TEXTURE = Identifier.fromNamespaceAndPath("herosgotmotion", "hud/sprint_bar_progress_2");
    private static final Identifier SPRINT_BAR_REGRESS_2_TEXTURE = Identifier.fromNamespaceAndPath("herosgotmotion", "hud/sprint_bar_regress_2");

    private static final Identifier SPRINTING_MODIFIER_ID = Identifier.withDefaultNamespace("sprinting");
    private static final double MIN_SCALE = 0.05;

    private static final double FILL_AT_CROUCH = Anchor.CROUCH.defaultValue / Anchor.SPRINT.defaultValue;
    private static final double FILL_AT_WALK = Anchor.WALK.defaultValue / Anchor.SPRINT.defaultValue;
    private static final double FILL_AT_SPRINT = 1.0;
    private static final double FILL_AT_MAX = Anchor.MAX.defaultValue / Anchor.SPRINT.defaultValue;

    private static float mainBarFill = 0f;
    private static float secondBarFill = 0f;

    private SprintBarRenderer() {}

    public static double relativeVelocity(LocalPlayer player) {
        float yaw = player.getYRot() * ((float) Math.PI / 180F);
        double dx = player.getX() - player.xOld;
        double dz = player.getZ() - player.zOld;

        if (HerosGotMotion.objectiveVelocity) {
            double squared = dx * dx + dz * dz;
            if (HerosGotMotion.totalVelocity) {
                double dy = player.getY() - player.yOld;
                squared += dy * dy;
            }
            return Math.sqrt(squared) * 20.0;
        }

        if (!HerosGotMotion.totalVelocity) {
            double facingX = -Math.sin(yaw);
            double facingZ = Math.cos(yaw);
            return (dx * facingX + dz * facingZ) * 20.0;
        }

        float pitch = player.getXRot() * ((float) Math.PI / 180F);
        double cosPitch = Math.cos(pitch);
        double facingX = -Math.sin(yaw) * cosPitch;
        double facingY = -Math.sin(pitch);
        double facingZ = Math.cos(yaw) * cosPitch;
        double dy = player.getY() - player.yOld;
        return (dx * facingX + dy * facingY + dz * facingZ) * 20.0;
    }

    public static float movementSpeedScale(LocalPlayer player) {
        if (!HerosGotMotion.relativeToSpeed) return 1.0F;

        AttributeInstance instance = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (instance == null) return 1.0F;

        double base = instance.getBaseValue();
        if (base <= 0.0) return 1.0F;

        double value = instance.getValue();

        AttributeModifier sprinting = instance.getModifier(SPRINTING_MODIFIER_ID);
        if (sprinting != null && sprinting.amount() > -1.0) {
            value /= 1.0 + sprinting.amount();
        }

        return (float) Math.max(value / base, MIN_SCALE);
    }

    public static double barFill(double blocksPerSecond, double scale, SpeedAnchors.Resolved anchors) {
        boolean forward = blocksPerSecond >= 0.0;
        double magnitude = Math.abs(blocksPerSecond) / Math.max(scale, MIN_SCALE);

        double crouch = anchor(anchors, forward, Anchor.CROUCH);
        double walk = anchor(anchors, forward, Anchor.WALK);
        double sprint = anchor(anchors, forward, Anchor.SPRINT);
        double max = anchor(anchors, forward, Anchor.MAX);

        double fill;
        if (magnitude < crouch) {
            fill = segment(magnitude, 0.0, 0.0, crouch, FILL_AT_CROUCH);
        } else if (magnitude < walk) {
            fill = segment(magnitude, crouch, FILL_AT_CROUCH, walk, FILL_AT_WALK);
        } else if (magnitude < sprint) {
            fill = segment(magnitude, walk, FILL_AT_WALK, sprint, FILL_AT_SPRINT);
        } else {
            fill = segment(magnitude, sprint, FILL_AT_SPRINT, max, FILL_AT_MAX);
        }

        return forward ? fill : -fill;
    }

    private static double anchor(SpeedAnchors.Resolved anchors, boolean forward, Anchor anchor) {
        return forward ? anchors.forward(anchor) : anchors.backward(anchor);
    }

    private static double segment(double value, double fromSpeed, double fromFill, double toSpeed, double toFill) {
        return fromFill + (value - fromSpeed) / (toSpeed - fromSpeed) * (toFill - fromFill);
    }

    public static boolean render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!HerosGotMotion.enabled) return false;
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) return false;

        if (player.tickCount < HerosGotMotion.hideBarUntilTick) {
            return false;
        }

        double blocksPerSecond = relativeVelocity(player);
        float scale = movementSpeedScale(player);

        HerosGotMotion.speedScale = scale;
        HerosGotMotion.speed = blocksPerSecond;

        if (!HerosGotMotion.showBar) return false;

        float scaled = (float) barFill(blocksPerSecond, scale, HerosGotMotion.anchors.resolved());
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
