package io.github.ximu.autofish.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("autofish/config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("autofish.json");

    private AutoFishConfig config = new AutoFishConfig();

    public AutoFishConfig get() {
        return config;
    }

    public void replace(AutoFishConfig newConfig) {
        newConfig.normalize();
        config = newConfig;
        save();
    }

    public void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }

        try {
            AutoFishConfig loaded = GSON.fromJson(Files.readString(CONFIG_PATH, StandardCharsets.UTF_8), AutoFishConfig.class);
            if (loaded == null) {
                throw new JsonParseException("Configuration is empty");
            }
            int loadedVersion = loaded.version;
            loaded.normalize();
            config = loaded;
            if (loadedVersion != AutoFishConfig.CURRENT_VERSION) {
                save();
            }
        } catch (IOException | JsonParseException exception) {
            LOGGER.error("Could not read {}; using defaults", CONFIG_PATH, exception);
            backupInvalidConfig();
            config = new AutoFishConfig();
            save();
        }
    }

    public void save() {
        config.normalize();
        Path temporary = CONFIG_PATH.resolveSibling(CONFIG_PATH.getFileName() + ".tmp");
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(temporary, GSON.toJson(config), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            LOGGER.error("Could not save {}", CONFIG_PATH, exception);
        }
    }

    private void backupInvalidConfig() {
        try {
            Files.move(CONFIG_PATH, CONFIG_PATH.resolveSibling("autofish.invalid.json"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            LOGGER.warn("Could not back up invalid configuration", exception);
        }
    }
}
