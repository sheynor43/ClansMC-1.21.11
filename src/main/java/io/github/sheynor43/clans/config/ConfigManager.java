package io.github.sheynor43.clans.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Loads {@code config.yml}, merges newly added keys from the bundled default on
 * version bumps (without touching user values or comments) and exposes a typed
 * {@link Settings} snapshot.
 */
public final class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private Settings settings;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "config.yml");
        if (!file.exists()) {
            plugin.saveResource("config.yml", false);
        }

        FileConfiguration user = YamlConfiguration.loadConfiguration(file);
        YamlConfiguration defaults = loadDefaults();
        if (defaults != null) {
            mergeMissingKeys(user, defaults, file);
        }

        this.config = user;
        this.settings = new Settings(user);
    }

    private YamlConfiguration loadDefaults() {
        try (InputStream in = plugin.getResource("config.yml")) {
            if (in == null) {
                return null;
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not read bundled config.yml: " + ex.getMessage());
            return null;
        }
    }

    private void mergeMissingKeys(FileConfiguration user, YamlConfiguration defaults, File file) {
        int userVersion = user.getInt("config-version", 0);
        int jarVersion = defaults.getInt("config-version", 0);
        if (userVersion >= jarVersion) {
            return;
        }

        int added = 0;
        for (String key : defaults.getKeys(true)) {
            if (defaults.isConfigurationSection(key)) {
                continue;
            }
            if (!user.contains(key, true)) {
                user.set(key, defaults.get(key));
                user.setComments(key, defaults.getComments(key));
                user.setInlineComments(key, defaults.getInlineComments(key));
                added++;
            }
        }
        user.set("config-version", jarVersion);

        try {
            user.save(file);
            plugin.getLogger().info("config.yml updated to version " + jarVersion
                    + " (" + added + " new key(s) merged, your values were kept).");
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to save merged config.yml: " + ex.getMessage());
        }
    }

    public FileConfiguration raw() {
        return config;
    }

    public Settings settings() {
        return settings;
    }
}
