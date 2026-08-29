package hero.bane.config;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.DoubleConsumer;

class ValueSlider extends AbstractSliderButton {
    private final DoubleConsumer setter;
    private final String label;

    private double min;
    private double max;

    ValueSlider(int x, int y, int width, int height, double min, double max,
                double initial, DoubleConsumer setter, String label) {
        super(x, y, width, height, Component.empty(), 0.0);
        this.min = min;
        this.max = max;
        this.setter = setter;
        this.label = label;
        set(initial);
    }

    void set(double target) {
        this.value = max > min ? Mth.clamp((target - min) / (max - min), 0.0, 1.0) : 0.0;
        updateMessage();
        setter.accept(current());
    }

    void setRange(double min, double max) {
        double current = current();
        this.min = min;
        this.max = max;
        set(current);
    }

    private int current() {
        return (int) Math.round(min + this.value * (max - min));
    }

    @Override
    protected void updateMessage() {
        setMessage(Component.literal(label + ": " + current()));
    }

    @Override
    protected void applyValue() {
        setter.accept(current());
    }
}
