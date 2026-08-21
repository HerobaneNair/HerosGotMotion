package hero.bane.config;

import hero.bane.HerosGotMotion;
import hero.bane.command.HerosGotMotionCommand;
import hero.bane.command.HerosGotMotionCommand.Mode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

public class HerosGotMotionScreen extends Screen {
    private static final int COL_W = 150;
    private static final int GAP = 10;
    private static final int ROW_H = 20;

    private final Screen parent;

    private int leftX;
    private int rightX;
    private int speedLabelY;

    public HerosGotMotionScreen(Screen parent) {
        super(Component.translatable("herosgotmotion.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        leftX = this.width / 2 - COL_W - GAP / 2;
        rightX = this.width / 2 + GAP / 2;

        int y = 40;

        addRenderableWidget(Button.builder(enabledText(), b -> {
            HerosGotMotion.enabled = !HerosGotMotion.enabled;
            b.setMessage(enabledText());
        }).bounds(leftX, y, COL_W, ROW_H).build());

        addRenderableWidget(Button.builder(modeText(), b -> {
            HerosGotMotionCommand.setMode(nextMode(HerosGotMotionCommand.getMode()));
            b.setMessage(modeText());
        }).bounds(rightX, y, COL_W, ROW_H).build());

        y += ROW_H + 4;

        addRenderableWidget(Button.builder(smoothText(), b -> {
            HerosGotMotion.smoothBar = !HerosGotMotion.smoothBar;
            b.setMessage(smoothText());
        }).bounds(leftX, y, COL_W, ROW_H).build());

        addRenderableWidget(new ValueSlider(rightX, y, COL_W, ROW_H, 26, 255,
                () -> HerosGotMotion.textOpacity,
                v -> HerosGotMotion.textOpacity = (int) Math.round(v),
                "herosgotmotion.config.text_opacity"));

        y += ROW_H + 4;

        addRenderableWidget(new ValueSlider(leftX, y, COL_W * 2 + GAP, ROW_H, 0, 100,
                () -> HerosGotMotion.hideTicksAfterXp,
                v -> HerosGotMotion.hideTicksAfterXp = (int) Math.round(v),
                "herosgotmotion.config.xp_hide_ticks"));

        y += ROW_H + 16;

        speedLabelY = y;
        y += 11;
        addRenderableWidget(offsetBox(leftX, y, HerosGotMotion.speedOffsetX,
                v -> HerosGotMotion.speedOffsetX = v, "herosgotmotion.config.speed_offset_x"));
        addRenderableWidget(offsetBox(rightX, y, HerosGotMotion.speedOffsetY,
                v -> HerosGotMotion.speedOffsetY = v, "herosgotmotion.config.speed_offset_y"));

        y += ROW_H + 16;

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(this.width / 2 - 100, Math.min(y, this.height - 28), 200, ROW_H).build());
    }

    private EditBox offsetBox(int x, int y, double initial, DoubleConsumer setter, String labelKey) {
        EditBox box = new EditBox(this.font, x, y, COL_W, ROW_H, Component.translatable(labelKey));
        box.setValue(formatOffset(initial));
        box.setResponder(text -> {
            try {
                setter.accept(Double.parseDouble(text.trim()));
            } catch (NumberFormatException ignored) {
            }
        });
        return box;
    }

    private static String formatOffset(double value) {
        return value == Math.rint(value) ? Integer.toString((int) value) : Double.toString(value);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 18, 0xFFFFFFFF);
        drawFieldLabel(graphics, leftX, speedLabelY, "herosgotmotion.config.speed_offset_x");
        drawFieldLabel(graphics, rightX, speedLabelY, "herosgotmotion.config.speed_offset_y");
    }

    private void drawFieldLabel(GuiGraphics graphics, int x, int y, String key) {
        graphics.drawString(this.font, Component.translatable(key), x, y, 0xFFA0A0A0);
    }

    @Override
    public void onClose() {
        HerosGotMotion.textOpacity = Mth.clamp(HerosGotMotion.textOpacity, 26, 255);
        HerosGotMotionCommand.saveConfig();
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    private static Mode nextMode(Mode mode) {
        return switch (mode) {
            case BAR -> Mode.BOTH;
            case BOTH -> Mode.TEXT;
            case TEXT -> Mode.BAR;
        };
    }

    private static Component enabledText() {
        return optionText("herosgotmotion.config.enabled", HerosGotMotion.enabled);
    }

    private static Component smoothText() {
        return optionText("herosgotmotion.config.smooth_bar", HerosGotMotion.smoothBar);
    }

    private static Component modeText() {
        Mode mode = HerosGotMotionCommand.getMode();
        return Component.translatable("herosgotmotion.config.mode",
                Component.translatable("herosgotmotion.config.mode." + mode.name().toLowerCase()));
    }

    private static Component optionText(String key, boolean value) {
        return Component.translatable("options.generic_value",
                Component.translatable(key),
                value ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF);
    }

    private static final class ValueSlider extends AbstractSliderButton {
        private final double min;
        private final double max;
        private final DoubleConsumer setter;
        private final String labelKey;

        private ValueSlider(int x, int y, int width, int height, double min, double max,
                            DoubleSupplier getter, DoubleConsumer setter, String labelKey) {
            super(x, y, width, height, Component.empty(), (getter.getAsDouble() - min) / (max - min));
            this.min = min;
            this.max = max;
            this.setter = setter;
            this.labelKey = labelKey;
            updateMessage();
        }

        private int current() {
            return (int) Math.round(min + this.value * (max - min));
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("options.generic_value",
                    Component.translatable(labelKey), Component.literal(Integer.toString(current()))));
        }

        @Override
        protected void applyValue() {
            setter.accept(current());
        }
    }
}
