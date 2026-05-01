package net.vulkanmod.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.SharedConstants;
import net.vulkanmod.Initializer;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

public abstract class UpdateChecker {
    private static boolean updateAvailable = false;

    public static void checkForUpdates() {
        CompletableFuture.supplyAsync(() -> {
            try {
                String req = "https://api.modrinth.com/v2/project/vulkanmod/version?include_changelog=false";
                String mcVersion = SharedConstants.getCurrentVersion().getName();
                req += "&game_versions=%s".formatted(mcVersion);

                URL url = new URL(req);
                HttpURLConnection http = (HttpURLConnection)url.openConnection();
                var inputStream = http.getInputStream();

                JsonObject data = JsonParser.parseString("{ versions: " + new String(inputStream.readAllBytes()) + "}").getAsJsonObject();
                JsonArray versions = data.getAsJsonArray("versions");
                http.disconnect();

                String version = String.valueOf(versions.get(0).getAsJsonObject().get("version_number")).replace("\"", "");

                var currentVersion = parseSemVer(Initializer.getVersion());
                if (Initializer.getVersion().contains("-")) {
                    Initializer.LOGGER.info("Pre-release version, skipping update check.");

                    return null;
                }

                updateAvailable = compareSemVer(currentVersion, parseSemVer(version)) < 0;

                if (updateAvailable) {
                    Initializer.LOGGER.info("Update available!");
                }
            }
            catch (IOException e) {
                Initializer.LOGGER.info("Error occurred, skipping update check.");
            }
            return null;
        });
    }

    private static int[] parseSemVer(String version) {
        String normalized = version.split("-", 2)[0];
        String[] split = normalized.split("\\.");
        int[] parts = new int[]{0, 0, 0};
        for (int i = 0; i < Math.min(split.length, 3); i++) {
            try {
                parts[i] = Integer.parseInt(split[i].replaceAll("[^0-9]", ""));
            } catch (NumberFormatException ignored) {
                parts[i] = 0;
            }
        }
        return parts;
    }

    private static int compareSemVer(int[] current, int[] remote) {
        for (int i = 0; i < 3; i++) {
            if (current[i] != remote[i]) {
                return Integer.compare(current[i], remote[i]);
            }
        }
        return 0;
    }

    public static boolean isUpdateAvailable() {
        return updateAvailable;
    }
}
