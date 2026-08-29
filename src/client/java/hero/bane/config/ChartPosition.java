package hero.bane.config;

import java.util.Locale;

public enum ChartPosition {
    BOTTOM_LEFT(Horizontal.LEFT, Vertical.BOTTOM),
    CENTER_LEFT(Horizontal.LEFT, Vertical.CENTER),
    TOP_LEFT(Horizontal.LEFT, Vertical.TOP),
    TOP_MIDDLE(Horizontal.MIDDLE, Vertical.TOP),
    TOP_RIGHT(Horizontal.RIGHT, Vertical.TOP),
    CENTER_RIGHT(Horizontal.RIGHT, Vertical.CENTER),
    BOTTOM_RIGHT(Horizontal.RIGHT, Vertical.BOTTOM);

    public enum Horizontal { LEFT, MIDDLE, RIGHT }

    public enum Vertical { TOP, CENTER, BOTTOM }

    public static final ChartPosition DEFAULT = BOTTOM_LEFT;

    private static final ChartPosition[] ORDER = values();

    public final Horizontal horizontal;
    public final Vertical vertical;

    ChartPosition(Horizontal horizontal, Vertical vertical) {
        this.horizontal = horizontal;
        this.vertical = vertical;
    }

    public String display() {
        StringBuilder out = new StringBuilder();
        for (String word : name().split("_")) {
            if (!out.isEmpty()) out.append(' ');
            out.append(word.charAt(0)).append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return out.toString();
    }

    public ChartPosition next() {
        return ORDER[(ordinal() + 1) % ORDER.length];
    }

    public static ChartPosition byName(String name) {
        if (name == null) return DEFAULT;
        try {
            return valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return DEFAULT;
        }
    }
}
