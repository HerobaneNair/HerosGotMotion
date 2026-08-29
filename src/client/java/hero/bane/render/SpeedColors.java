package hero.bane.render;

import hero.bane.config.SpeedAnchors;
import hero.bane.config.SpeedAnchors.Anchor;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

public final class SpeedColors {
    private static final int BLUE = 0x0000FF;
    private static final int PINK = 0xFFAACC;
    private static final int PURPLE = 0x9900FF;
    private static final int RED = 0xFF0000;
    private static final int WHITE = 0xFFFFFF;
    private static final int ORANGE = 0xFFA500;
    private static final int YELLOW = 0xFFFF00;
    private static final int GREEN = 0x00FF00;
    private static final int CYAN = 0x00FFFF;

    private SpeedColors() {}

    public static int rgb(double blocksPerSecond, SpeedAnchors.Resolved anchors) {
        if (blocksPerSecond < 0.0) {
            double crouch = anchors.backward(Anchor.CROUCH);
            if (blocksPerSecond >= -crouch) {
                return lerp(blocksPerSecond, -crouch, RED, 0.0, WHITE, anchors.forward(Anchor.CROUCH), ORANGE);
            }

            double sprint = anchors.backward(Anchor.SPRINT);
            if (blocksPerSecond >= -sprint) {
                return lerp(blocksPerSecond, -sprint, PINK, -anchors.backward(Anchor.WALK), PURPLE, -crouch, RED);
            }

            double max = anchors.backward(Anchor.MAX);
            return blocksPerSecond > -max
                    ? lerp(blocksPerSecond, -max, BLUE, -sprint, PINK)
                    : BLUE;
        }

        double crouch = anchors.forward(Anchor.CROUCH);
        if (blocksPerSecond < crouch) {
            return lerp(blocksPerSecond, -anchors.backward(Anchor.CROUCH), RED, 0.0, WHITE, crouch, ORANGE);
        }

        double sprint = anchors.forward(Anchor.SPRINT);
        if (blocksPerSecond < sprint) {
            return lerp(blocksPerSecond, crouch, ORANGE, anchors.forward(Anchor.WALK), YELLOW, sprint, GREEN);
        }

        double max = anchors.forward(Anchor.MAX);
        return blocksPerSecond < max
                ? lerp(blocksPerSecond, sprint, GREEN, max, CYAN)
                : CYAN;
    }

    private static int lerp(double value, double min, int minColor, double max, int maxColor) {
        value = Mth.clamp(value, min, max);
        return ARGB.srgbLerp((float) ((value - min) / (max - min)), minColor, maxColor);
    }

    private static int lerp(double value, double min, int minColor, double median, int medianColor, double max, int maxColor) {
        value = Mth.clamp(value, min, max);
        return value < median
                ? ARGB.srgbLerp((float) ((value - min) / (median - min)), minColor, medianColor)
                : ARGB.srgbLerp((float) ((value - median) / (max - median)), medianColor, maxColor);
    }
}
