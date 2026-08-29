package hero.bane.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import hero.bane.HerosGotMotion;
import hero.bane.config.SpeedAnchors.Anchor;
import hero.bane.config.SpeedAnchors.Direction;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class HerosGotMotionConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("herosgotmotion.json");

    public static final class Data {
        public boolean showBar = true;
        public boolean showTexts = false;
        public double speedOffsetX = HerosGotMotion.DEFAULT_SPEED_OFFSET_X;
        public double speedOffsetY = HerosGotMotion.DEFAULT_SPEED_OFFSET_Y;
        public String chartPosition = ChartPosition.DEFAULT.name();
        public double chartPaddingX = 0;
        public double chartPaddingY = 0;
        public boolean smoothBar = true;
        public int textOpacity = 255;
        public boolean totalVelocity = false;
        public boolean objectiveVelocity = false;
        public boolean relativeToSpeed = true;
        public AnchorSet forwardAnchors = new AnchorSet();
        public AnchorSet backwardAnchors = new AnchorSet();
        public int hideTicksAfterXp = 25;

        public void readAnchorsInto(SpeedAnchors target) {
            set(target, Direction.FORWARD, forwardAnchors);
            set(target, Direction.BACKWARD, backwardAnchors);
        }

        public void writeAnchorsFrom(SpeedAnchors source) {
            forwardAnchors = get(source, Direction.FORWARD);
            backwardAnchors = get(source, Direction.BACKWARD);
        }

        private static void set(SpeedAnchors target, Direction direction, AnchorSet values) {
            AnchorSet safe = values == null ? new AnchorSet() : values;
            target.set(direction, Anchor.CROUCH, safe.crouch);
            target.set(direction, Anchor.WALK, safe.walk);
            target.set(direction, Anchor.SPRINT, safe.sprint);
            target.set(direction, Anchor.MAX, safe.max);
        }

        private static AnchorSet get(SpeedAnchors source, Direction direction) {
            AnchorSet values = new AnchorSet();
            values.crouch = source.get(direction, Anchor.CROUCH);
            values.walk = source.get(direction, Anchor.WALK);
            values.sprint = source.get(direction, Anchor.SPRINT);
            values.max = source.get(direction, Anchor.MAX);
            return values;
        }
    }

    public static final class AnchorSet {
        public double crouch = Anchor.CROUCH.defaultValue;
        public double walk = Anchor.WALK.defaultValue;
        public double sprint = Anchor.SPRINT.defaultValue;
        public double max = Anchor.MAX.defaultValue;
    }

    public static Data load() {
        try {
            if (Files.exists(PATH)) {
                try (Reader r = Files.newBufferedReader(PATH)) {
                    Data d = GSON.fromJson(r, Data.class);
                    return d == null ? new Data() : d;
                }
            }
        } catch (IOException e) {
            HerosGotMotion.LOG.warn("Failed to load config: {}", e.toString());
        }
        return new Data();
    }

    public static Data snapshot() {
        Data d = new Data();
        d.showBar = HerosGotMotion.showBar;
        d.showTexts = HerosGotMotion.showTexts;
        d.speedOffsetX = HerosGotMotion.speedOffsetX;
        d.speedOffsetY = HerosGotMotion.speedOffsetY;
        d.chartPosition = HerosGotMotion.chartPosition.name();
        d.chartPaddingX = HerosGotMotion.chartPaddingX;
        d.chartPaddingY = HerosGotMotion.chartPaddingY;
        d.smoothBar = HerosGotMotion.smoothBar;
        d.textOpacity = HerosGotMotion.textOpacity;
        d.totalVelocity = HerosGotMotion.totalVelocity;
        d.objectiveVelocity = HerosGotMotion.objectiveVelocity;
        d.relativeToSpeed = HerosGotMotion.relativeToSpeed;
        d.writeAnchorsFrom(HerosGotMotion.anchors);
        d.hideTicksAfterXp = HerosGotMotion.hideTicksAfterXp;
        return d;
    }

    public static void saveCurrent() {
        save(snapshot());
    }

    public static void save(Data d) {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer w = Files.newBufferedWriter(PATH)) {
                GSON.toJson(d, w);
            }
        } catch (IOException e) {
            HerosGotMotion.LOG.warn("Failed to save config: {}", e.toString());
        }
    }

    private HerosGotMotionConfig() {}
}
