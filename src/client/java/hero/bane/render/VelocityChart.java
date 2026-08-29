package hero.bane.render;

import hero.bane.HerosGotMotion;
import hero.bane.config.ChartPosition;
import hero.bane.config.SpeedAnchors;
import hero.bane.config.SpeedAnchors.Anchor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.util.Mth;

import java.util.Locale;

public final class VelocityChart {
    public static final int CAPACITY = 100;

    private static final int CHART_HEIGHT = 60;
    private static final int LABEL_HEIGHT = 9;
    private static final int ZERO_OFFSET = 20;
    private static final int ABOVE = CHART_HEIGHT - ZERO_OFFSET;
    private static final int BORDER_INSET = 1;
    private static final int MIN_WIDTH = 2 * BORDER_INSET + 1;

    private static final double MIN_SCALE = 0.05;
    private static final double STEADY_EPSILON = 1.0e-6;

    private static final int BACKGROUND = 0x90505050;
    private static final int BORDER = 0xFFFFFFFF;
    private static final int LABEL_COLOR = 0xFFE0E0E0;
    private static final int ZERO_LINE_COLOR = 0xFF000000;
    private static final int SPRINT_LINE_COLOR = 0xFF00FFFF;

    private static final History HISTORY = new History(CAPACITY);

    private static Stats cachedStats;
    private static long cachedRevision = -1;
    private static int cachedFrom = -1;
    private static int cachedCount = -1;

    private VelocityChart() {}

    public static void push(double blocksPerSecond, double scale) {
        HISTORY.push(blocksPerSecond, scale);
    }

    public static void reset() {
        HISTORY.clear();
    }

    public static void renderHud(GuiGraphics graphics) {
        if (!HerosGotMotion.showChart || !HerosGotMotion.enabled) return;

        Minecraft client = Minecraft.getInstance();
        if (client.options.hideGui) return;

        DebugScreenOverlay debug = client.getDebugOverlay();
        if (debug.showFpsCharts() || debug.showNetworkCharts() || debug.showProfilerChart()) return;

        int width = getWidth(graphics.guiWidth() / 2);
        ChartPosition position = HerosGotMotion.chartPosition;
        int left = leftFor(position, graphics.guiWidth(), width, (int) HerosGotMotion.chartPaddingX);
        int bottom = bottomFor(position, graphics.guiHeight(), (int) HerosGotMotion.chartPaddingY);
        drawChart(graphics, client.font, left, bottom, width);
    }

    public static int getWidth(int available) {
        return Math.min(CAPACITY + 2 * BORDER_INSET, available);
    }

    private static int leftFor(ChartPosition position, int guiWidth, int width, int padX) {
        return switch (position.horizontal) {
            case LEFT -> padX;
            case MIDDLE -> Math.max(0, (guiWidth - width) / 2) + padX;
            case RIGHT -> Math.max(0, guiWidth - width) - padX;
        };
    }

    private static int bottomFor(ChartPosition position, int guiHeight, int padY) {
        return switch (position.vertical) {
            case TOP -> LABEL_HEIGHT + CHART_HEIGHT + padY;
            case CENTER -> (guiHeight + CHART_HEIGHT) / 2 - padY;
            case BOTTOM -> guiHeight - padY;
        };
    }

    public static void drawChart(GuiGraphics graphics, Font font, int x, int bottom, int width) {
        if (font == null || width < MIN_WIDTH) return;

        Layout layout = Layout.anchoredTo(graphics, x, bottom, width);
        if (layout == null) return;

        SpeedAnchors.Resolved anchors = HerosGotMotion.anchors.resolved();
        Window window = HISTORY.lastColumns(layout.plotWidth());
        Stats stats = statsFor(window);
        int sprintY = sprintLineY(layout, anchors);

        drawBackground(graphics, layout);
        drawSprintLine(graphics, layout, sprintY);
        drawBars(graphics, layout, window, anchors);
        drawFrame(graphics, layout);
        drawStats(graphics, font, layout, stats);
        drawGuides(graphics, font, layout, anchors, sprintY);
    }

    private static void drawBackground(GuiGraphics graphics, Layout layout) {
        graphics.fill(layout.left(), layout.top(), layout.right() + 1, layout.bottom(), BACKGROUND);
    }

    private static void drawBars(GuiGraphics graphics, Layout layout, Window window, SpeedAnchors.Resolved anchors) {
        int count = window.count();
        int from = window.from();
        int plotLeft = layout.plotLeft();
        int baseline = layout.baseline();

        for (int i = 0; i < count; i++) {
            int slot = HISTORY.slot(from + i);
            drawBar(graphics, plotLeft + i, baseline, HISTORY.valueAtSlot(slot), HISTORY.scaleAtSlot(slot), anchors);
        }
    }

    private static Stats statsFor(Window window) {
        int from = window.from();
        int count = window.count();

        if (cachedStats != null
                && cachedRevision == HISTORY.revision()
                && cachedFrom == from
                && cachedCount == count) {
            return cachedStats;
        }

        double total = 0.0;
        double max = 0.0;
        double maxMagnitude = 0.0;

        for (int i = 0; i < count; i++) {
            double value = HISTORY.valueAtSlot(HISTORY.slot(from + i));
            double magnitude = Math.abs(value);

            if (magnitude > maxMagnitude) {
                maxMagnitude = magnitude;
                max = value;
            }
            total += value;
        }

        cachedStats = new Stats(count, total, max);
        cachedRevision = HISTORY.revision();
        cachedFrom = from;
        cachedCount = count;
        return cachedStats;
    }

    private static void drawBar(GuiGraphics graphics, int x, int baseline, double value, double scale,
                                SpeedAnchors.Resolved anchors) {
        double normalized = value / Math.max(scale, MIN_SCALE);
        int color = 0xFF000000 | SpeedColors.rgb(normalized, anchors);
        int height = heightFor(normalized, anchors);

        if (height >= 0) {
            graphics.fill(x, baseline - Math.max(height, 1), x + 1, baseline, color);
        } else {
            graphics.fill(x, baseline, x + 1, baseline - height, color);
        }
    }

    private static void drawFrame(GuiGraphics graphics, Layout layout) {
        graphics.hLine(layout.left(), layout.right(), layout.top(), BORDER);
        graphics.hLine(layout.left(), layout.right(), layout.bottom() - 1, BORDER);
        graphics.vLine(layout.left(), layout.top(), layout.bottom(), BORDER);
        graphics.vLine(layout.right(), layout.top(), layout.bottom(), BORDER);
    }

    private static void drawStats(GuiGraphics graphics, Font font, Layout layout, Stats stats) {
        if (stats.count() <= 0) return;

        String avgText = format(stats.average()) + " avg";
        String maxText = format(stats.max()) + " max";
        int y = layout.top() - LABEL_HEIGHT;

        graphics.drawString(font, avgText, layout.left() + 2, y, LABEL_COLOR);
        graphics.drawString(font, maxText, layout.right() - font.width(maxText) - 1, y, LABEL_COLOR);
    }

    private static void drawSprintLine(GuiGraphics graphics, Layout layout, int sprintY) {
        graphics.hLine(layout.left(), layout.right(), sprintY, SPRINT_LINE_COLOR);
    }

    private static void drawGuides(GuiGraphics graphics, Font font, Layout layout,
                                   SpeedAnchors.Resolved anchors, int sprintY) {
        graphics.hLine(layout.left() + 1, layout.right() - 1, layout.baseline() - 1, ZERO_LINE_COLOR);

        drawShadedString(graphics, font,
                format(anchors.forward(Anchor.SPRINT) * HISTORY.latestScale()) + " bl/s",
                layout.left() + 1, sprintY + 1);
        drawShadedString(graphics, font, "0 bl/s", layout.left() + 1, layout.baseline() + 1);
    }

    private static int sprintLineY(Layout layout, SpeedAnchors.Resolved anchors) {
        return layout.baseline() - heightFor(anchors.forward(Anchor.SPRINT), anchors);
    }

    private static void drawShadedString(GuiGraphics graphics, Font font, String text, int x, int y) {
        graphics.fill(x, y, x + font.width(text) + 1, y + LABEL_HEIGHT, BACKGROUND);
        graphics.drawString(font, text, x + 1, y + 1, LABEL_COLOR, false);
    }

    private static int heightFor(double blocksPerSecond, SpeedAnchors.Resolved anchors) {
        return blocksPerSecond >= 0.0
                ? (int) Math.round(blocksPerSecond * ABOVE / anchors.forward(Anchor.MAX))
                : -Mth.clamp((int) Math.round(-blocksPerSecond * ZERO_OFFSET / anchors.backward(Anchor.MAX)), 0, ZERO_OFFSET);
    }

    private static String format(double blocksPerSecond) {
        return String.format(Locale.ROOT, "%.2f", blocksPerSecond);
    }

    private record Layout(int left, int right, int top, int bottom, int baseline, int plotLeft, int plotWidth) {
        static Layout anchoredTo(GuiGraphics graphics, int x, int bottom, int width) {
            if (graphics.guiHeight() < CHART_HEIGHT + LABEL_HEIGHT || bottom < CHART_HEIGHT) return null;

            return new Layout(
                    x,
                    x + width - 1,
                    bottom - CHART_HEIGHT,
                    bottom,
                    bottom - ZERO_OFFSET,
                    x + BORDER_INSET,
                    width - 2 * BORDER_INSET);
        }
    }

    private record Window(int from, int count) {}

    private record Stats(int count, double total, double max) {
        double average() {
            return count > 0 ? total / count : 0.0;
        }
    }

    private static final class History {
        private final double[] values;
        private final double[] scales;
        private final int capacity;
        private int start;
        private int size;
        private int uniformRun;
        private long revision;

        History(int capacity) {
            this.capacity = capacity;
            this.values = new double[capacity];
            this.scales = new double[capacity];
        }

        void push(double value, double scale) {
            if (isSteady(value, scale)) return;

            uniformRun = matchesNewest(value, scale) ? Math.min(uniformRun + 1, capacity) : 1;

            int i = wrap(start + size);
            values[i] = value;
            scales[i] = scale;
            if (size < capacity) {
                size++;
            } else {
                start = wrap(start + 1);
            }
            revision++;
        }

        private boolean isSteady(double value, double scale) {
            return size == capacity && uniformRun >= capacity && matchesNewest(value, scale);
        }

        private boolean matchesNewest(double value, double scale) {
            if (size == 0) return false;
            int newest = slot(size - 1);
            return same(values[newest], value) && same(scales[newest], scale);
        }

        private static boolean same(double a, double b) {
            return Math.abs(a - b) < STEADY_EPSILON;
        }

        long revision() {
            return revision;
        }

        void clear() {
            start = 0;
            size = 0;
            uniformRun = 0;
            revision++;
        }

        Window lastColumns(int columns) {
            int skip = Math.max(0, capacity - columns);
            return new Window(skip, Math.max(0, size - skip));
        }

        int slot(int index) {
            return wrap(start + index);
        }

        double valueAtSlot(int slot) {
            return values[slot];
        }

        double scaleAtSlot(int slot) {
            return scales[slot];
        }

        double latestScale() {
            return size == 0 ? 1.0 : scaleAtSlot(slot(size - 1));
        }

        private int wrap(int index) {
            return index >= capacity ? index - capacity : index;
        }
    }
}
