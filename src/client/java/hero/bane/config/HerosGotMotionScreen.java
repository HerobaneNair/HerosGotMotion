package hero.bane.config;

import hero.bane.HerosGotMotion;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public class HerosGotMotionScreen extends Screen {
    static final int COL_W = 150;
    static final int GAP = 10;
    static final int ROW_H = 20;

    private static final int FULL_W = COL_W * 2 + GAP;
    private static final int CONTENT_TOP = 32;
    private static final int FOOTER_H = 32;
    private static final int SCROLL_STEP = 12;
    private static final int SECTION_GAP = 12;
    private static final int SCROLLBAR_W = 4;

    private record Row(AbstractWidget widget, int baseY) {}

    private final Screen parent;
    private final List<Row> rows = new ArrayList<>();

    private int maxX;
    private int maxY;

    private int contentBottom;
    private int maxScroll;
    private int scroll;

    private Button anchorsButton;
    private Button doneButton;
    private ValueSlider paddingX;
    private ValueSlider paddingY;
    private ValueSlider offsetX;
    private ValueSlider offsetY;

    public HerosGotMotionScreen(Screen parent) {
        super(Component.literal("HerosGotMotion"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int leftX = this.width / 2 - COL_W - GAP / 2;
        int rightX = this.width / 2 + GAP / 2;

        maxX = this.width;
        maxY = this.height;

        contentBottom = Math.max(CONTENT_TOP + ROW_H, this.height - FOOTER_H);
        rows.clear();

        int y = 40;

        row(Button.builder(enabledText(), b -> {
            HerosGotMotion.enabled = !HerosGotMotion.enabled;
            b.setMessage(enabledText());
        }).bounds(leftX, y, FULL_W, ROW_H).build());

        y += ROW_H + SECTION_GAP;

        row(Button.builder(barText(), b -> {
            HerosGotMotion.showBar = !HerosGotMotion.showBar;
            b.setMessage(barText());
        }).bounds(leftX, y, COL_W, ROW_H).build());

        row(Button.builder(smoothText(), b -> {
            HerosGotMotion.smoothBar = !HerosGotMotion.smoothBar;
            b.setMessage(smoothText());
        }).bounds(rightX, y, COL_W, ROW_H).build());

        y += ROW_H + 4;

        row(Button.builder(velocityText(), b -> {
            HerosGotMotion.totalVelocity = !HerosGotMotion.totalVelocity;
            b.setMessage(velocityText());
        }).bounds(leftX, y, COL_W, ROW_H).build());

        row(Button.builder(velocityFrameText(), b -> {
            HerosGotMotion.objectiveVelocity = !HerosGotMotion.objectiveVelocity;
            b.setMessage(velocityFrameText());
        }).bounds(rightX, y, COL_W, ROW_H).build());

        y += ROW_H + 4;

        row(Button.builder(speedScaleText(), b -> {
            HerosGotMotion.relativeToSpeed = !HerosGotMotion.relativeToSpeed;
            b.setMessage(speedScaleText());
            anchorsButton.active = !HerosGotMotion.relativeToSpeed;
        }).bounds(leftX, y, COL_W, ROW_H).build());

        anchorsButton = row(Button.builder(Component.literal("Speed Anchors..."),
                b -> this.minecraft.setScreen(new SpeedAnchorsScreen(this)))
                .bounds(rightX, y, COL_W, ROW_H).build());
        anchorsButton.active = !HerosGotMotion.relativeToSpeed;

        y += ROW_H + 4;

        row(new ValueSlider(leftX, y, COL_W, ROW_H, 0, 100,
                HerosGotMotion.hideTicksAfterXp,
                v -> HerosGotMotion.hideTicksAfterXp = (int) Math.round(v),
                "XP Hide Ticks"));

        row(new ValueSlider(rightX, y, COL_W, ROW_H, 26, 255,
                HerosGotMotion.textOpacity,
                v -> HerosGotMotion.textOpacity = (int) Math.round(v),
                "Text Opacity"));

        y += ROW_H + SECTION_GAP;

        row(Button.builder(speedTextToggle(), b -> {
            HerosGotMotion.showTexts = !HerosGotMotion.showTexts;
            b.setMessage(speedTextToggle());
        }).bounds(leftX, y, COL_W, ROW_H).build());

        row(Button.builder(Component.literal("Reset Velocity Text Offset"), b -> {
            offsetX.set(0);
            offsetY.set(0);
        }).bounds(rightX, y, COL_W, ROW_H).build());

        y += ROW_H + 4;

        offsetX = row(new ValueSlider(leftX, y, COL_W, ROW_H,
                -maxX / 2.0, maxX / 2.0, HerosGotMotion.speedOffsetX,
                v -> HerosGotMotion.speedOffsetX = v, "Vel. Text Offset X"));
        offsetY = row(new ValueSlider(rightX, y, COL_W, ROW_H,
                -maxY / 2.0, maxY / 2.0, HerosGotMotion.speedOffsetY,
                v -> HerosGotMotion.speedOffsetY = v, "Vel. Text Offset Y"));

        y += ROW_H + SECTION_GAP;

        row(Button.builder(chartText(), b -> {
            HerosGotMotion.setShowChart(!HerosGotMotion.showChart);
            b.setMessage(chartText());
        }).bounds(this.width / 2 - 100, y, 200, ROW_H).build());

        y += ROW_H + 4;

        row(Button.builder(chartPositionText(), b -> {
            HerosGotMotion.chartPosition = HerosGotMotion.chartPosition.next();
            b.setMessage(chartPositionText());
            paddingX.setRange(paddingMinX(), paddingMaxX());
            paddingY.setRange(paddingMinY(), paddingMaxY());
        }).bounds(leftX, y, COL_W, ROW_H).build());

        row(Button.builder(Component.literal("Reset Chart Padding"), b -> {
            paddingX.set(0);
            paddingY.set(0);
        }).bounds(rightX, y, COL_W, ROW_H).build());

        y += ROW_H + 4;

        paddingX = row(new ValueSlider(leftX, y, COL_W, ROW_H,
                paddingMinX(), paddingMaxX(), HerosGotMotion.chartPaddingX,
                v -> HerosGotMotion.chartPaddingX = v, "Chart Padding X"));
        paddingY = row(new ValueSlider(rightX, y, COL_W, ROW_H,
                paddingMinY(), paddingMaxY(), HerosGotMotion.chartPaddingY,
                v -> HerosGotMotion.chartPaddingY = v, "Chart Padding Y"));

        doneButton = addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(this.width / 2 - 100, this.height - 26, 200, ROW_H).build());

        int contentHeight = y + ROW_H + 8 - CONTENT_TOP;
        maxScroll = Math.max(0, contentHeight - (contentBottom - CONTENT_TOP));
        setScroll(scroll);
    }

    private <T extends AbstractWidget> T row(T widget) {
        addRenderableWidget(widget);
        rows.add(new Row(widget, widget.getY()));
        return widget;
    }

    private void setScroll(int value) {
        scroll = Mth.clamp(value, 0, maxScroll);
        for (Row row : rows) {
            int y = row.baseY() - scroll;
            row.widget().setY(y);
            row.widget().visible = y >= CONTENT_TOP && y + ROW_H <= contentBottom;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (maxScroll > 0) {
            setScroll(scroll - (int) Math.round(scrollY * SCROLL_STEP));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private double paddingMinX() {
        return centeredX() ? -maxX / 2.0 : 0;
    }

    private double paddingMaxX() {
        return centeredX() ? maxX / 2.0 : maxX;
    }

    private double paddingMinY() {
        return centeredY() ? -maxY / 2.0 : 0;
    }

    private double paddingMaxY() {
        return centeredY() ? maxY / 2.0 : maxY;
    }

    private static boolean centeredX() {
        return HerosGotMotion.chartPosition.horizontal == ChartPosition.Horizontal.MIDDLE;
    }

    private static boolean centeredY() {
        return HerosGotMotion.chartPosition.vertical == ChartPosition.Vertical.CENTER;
    }

    @Override
    public void renderBackground(@NonNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        try {
            super.renderBackground(graphics, mouseX, mouseY, partialTick);
        } catch (IllegalStateException blurAlreadyUsed) {
            graphics.fill(0, 0, this.width, this.height, 0xB0101010);
        }
    }

    @Override
    public void render(@NonNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        graphics.enableScissor(0, CONTENT_TOP, this.width, contentBottom);
        for (Row row : rows) row.widget().render(graphics, mouseX, mouseY, partialTick);
        graphics.disableScissor();

        renderScrollbar(graphics);
        doneButton.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 18, 0xFFFFFFFF);
    }

    private void renderScrollbar(GuiGraphics graphics) {
        if (maxScroll <= 0) return;

        int track = contentBottom - CONTENT_TOP;
        int height = Math.max(16, track * track / (track + maxScroll));
        int top = CONTENT_TOP + (track - height) * scroll / maxScroll;
        int x = this.width / 2 + FULL_W / 2 + GAP;

        graphics.fill(x, CONTENT_TOP, x + SCROLLBAR_W, contentBottom, 0x80000000);
        graphics.fill(x, top, x + SCROLLBAR_W, top + height, 0xFFA0A0A0);
    }

    @Override
    public void onClose() {
        HerosGotMotion.textOpacity = Mth.clamp(HerosGotMotion.textOpacity, 26, 255);
        HerosGotMotionConfig.saveCurrent();
        this.minecraft.setScreen(parent);
    }

    private static Component enabledText() {
        return optionText("Mod Enabled", HerosGotMotion.enabled);
    }

    private static Component barText() {
        return optionText("Sprint Bar", HerosGotMotion.showBar);
    }

    private static Component smoothText() {
        return optionText("Smooth Bar", HerosGotMotion.smoothBar);
    }

    private static Component chartText() {
        return optionText("Velocity Chart", HerosGotMotion.showChart);
    }

    private static Component speedTextToggle() {
        return optionText("Velocity Text", HerosGotMotion.showTexts);
    }

    private static Component chartPositionText() {
        return Component.literal("Chart Position: " + HerosGotMotion.chartPosition.display());
    }

    private static Component speedScaleText() {
        return Component.literal("Speed Scale: "
                + (HerosGotMotion.relativeToSpeed ? "Relative" : "Absolute"));
    }

    private static Component velocityText() {
        return Component.literal("Velocity: "
                + (HerosGotMotion.totalVelocity ? "Total" : "Horizontal"));
    }

    private static Component velocityFrameText() {
        return Component.literal("Relative Velocity: "
                + (HerosGotMotion.objectiveVelocity ? "Off" : "On"));
    }

    static Component optionText(String label, boolean value) {
        return Component.literal(label + ": " + (value ? "ON" : "OFF"));
    }
}
