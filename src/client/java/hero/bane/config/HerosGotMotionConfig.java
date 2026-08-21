package hero.bane.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import hero.bane.HerosGotMotion;
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
        public String mode = "BAR";
        public double speedOffsetX = HerosGotMotion.DEFAULT_SPEED_OFFSET_X;
        public double speedOffsetY = HerosGotMotion.DEFAULT_SPEED_OFFSET_Y;
        public boolean smoothBar = true;
        public int textOpacity = 255;
        public int hideTicksAfterXp = 25;
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
