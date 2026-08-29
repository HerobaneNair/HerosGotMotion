package hero.bane.config;

import hero.bane.HerosGotMotion;
import hero.bane.config.SpeedAnchors.Anchor;
import hero.bane.config.SpeedAnchors.Direction;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public class SpeedAnchorsScreen extends Screen {
    private static final int BOX_W = 100;
    private static final int LABEL_W = 76;
    private static final int GAP = 10;
    private static final int ROW_H = 20;
    private static final int HEADER_Y = 40;
    private static final int FIRST_ROW_Y = 52;

    private final Screen parent;
    private final Map<Direction, Map<Anchor, EditBox>> boxes = new EnumMap<>(Direction.class);

    private boolean updating;

    private int labelX;
    private int forwardX;
    private int backwardX;

    public SpeedAnchorsScreen(Screen parent) {
        super(Component.literal("Speed Anchors (bl/s)"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int blockWidth = LABEL_W + GAP + BOX_W + GAP + BOX_W;
        labelX = this.width / 2 - blockWidth / 2;
        forwardX = labelX + LABEL_W + GAP;
        backwardX = forwardX + BOX_W + GAP;

        boxes.clear();
        int y = FIRST_ROW_Y;
        for (Anchor anchor : SpeedAnchors.ANCHORS) {
            addBox(Direction.FORWARD, anchor, forwardX, y);
            addBox(Direction.BACKWARD, anchor, backwardX, y);
            y += ROW_H + 4;
        }

        y += 8;
        addRenderableWidget(Button.builder(Component.literal("Reset to Defaults"), b -> {
            HerosGotMotion.anchors.reset();
            rebuildBoxValues();
        }).bounds(labelX, y, blockWidth / 2 - GAP / 2, ROW_H).build());

        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(labelX + blockWidth / 2 + GAP / 2, y, blockWidth / 2 - GAP / 2, ROW_H).build());
    }

    private void addBox(Direction direction, Anchor anchor, int x, int y) {
        Component label = Component.literal(direction.display() + " " + anchor.display());

        EditBox box = new EditBox(this.font, x, y, BOX_W, ROW_H, label);
        box.setValue(format(HerosGotMotion.anchors.get(direction, anchor)));
        box.setFilter(text -> typeable(direction, text));
        box.setResponder(text -> {
            if (updating) return;
            try {
                double value = Double.parseDouble(text.trim());
                if (!SpeedAnchors.accepts(direction, value)) return;
                HerosGotMotion.anchors.set(direction, anchor, value);
                rebuildBoxValues(box);
            } catch (NumberFormatException ignored) {
            }
        });
        addRenderableWidget(box);
        boxes.computeIfAbsent(direction, d -> new EnumMap<>(Anchor.class)).put(anchor, box);
    }

    private static boolean typeable(Direction direction, String text) {
        if (text.isEmpty()) return true;
        if (direction == Direction.BACKWARD) {
            if (!text.startsWith("-")) return false;
            return text.length() == 1 || isDecimal(text.substring(1));
        }
        return isDecimal(text);
    }

    private static boolean isDecimal(String text) {
        boolean dot = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '.') {
                if (dot) return false;
                dot = true;
            } else if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    private void rebuildBoxValues() {
        rebuildBoxValues(null);
    }

    private void rebuildBoxValues(EditBox skip) {
        updating = true;
        try {
            boxes.forEach((direction, row) -> row.forEach((anchor, box) -> {
                if (box == skip) return;
                String current = format(HerosGotMotion.anchors.get(direction, anchor));
                if (!current.equals(box.getValue())) box.setValue(current);
            }));
        } finally {
            updating = false;
        }
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
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
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 18, 0xFFFFFFFF);

        graphics.drawCenteredString(this.font, Component.literal("Forward"),
                forwardX + BOX_W / 2, HEADER_Y, 0xFFA0A0A0);
        graphics.drawCenteredString(this.font, Component.literal("Backward"),
                backwardX + BOX_W / 2, HEADER_Y, 0xFFA0A0A0);

        int y = FIRST_ROW_Y + (ROW_H - 8) / 2;
        for (Anchor anchor : SpeedAnchors.ANCHORS) {
            graphics.drawString(this.font, Component.literal(anchor.display()),
                    labelX, y, 0xFFFFFFFF);
            y += ROW_H + 4;
        }
    }

    @Override
    public void onClose() {
        HerosGotMotionConfig.saveCurrent();
        this.minecraft.setScreen(parent);
    }
}
