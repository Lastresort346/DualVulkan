package net.beryl.option;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collections;

public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().excludeFieldsWithModifiers(java.lang.reflect.Modifier.TRANSIENT).create();
    private static Path CONFIG_PATH;

    public boolean shadersOn = true;
    public int shadowRenderDistance = 12;
    public int shadowResolution = 2048;
    public float atmFogIntensity = 1.0f;
    public float bloomIntensity = 1.0f;

    public void write() {
        if (!Files.exists(CONFIG_PATH.getParent(), new LinkOption[0])) {
            try { Files.createDirectories(CONFIG_PATH.getParent()); } catch (IOException e) { e.printStackTrace(); }
        }
        try { Files.write(CONFIG_PATH, Collections.singleton(GSON.toJson(this))); } catch (IOException e) { e.printStackTrace(); }
    }

    public static Config load(Path path) {
        CONFIG_PATH = path;
        if (Files.exists(path, new LinkOption[0])) {
            try (FileReader r = new FileReader(path.toFile())) {
                return GSON.fromJson(r, Config.class);
            } catch (IOException e) { throw new RuntimeException(e); }
        }
        return null;
    }
}
