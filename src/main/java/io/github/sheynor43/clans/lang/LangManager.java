package io.github.sheynor43.clans.lang;

import io.github.sheynor43.clans.config.Settings;
import io.github.sheynor43.clans.logic.LangResolver;
import io.github.sheynor43.clans.util.Text;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads every {@code lang/<code>.yml}, exposes MiniMessage-rendered messages and
 * implements the fallback chain: selected locale → default (en) → the key itself
 * (logged once). Bundled languages are extracted on first run and never
 * overwritten; any user-added {@code <code>.yml} is discovered automatically.
 */
public final class LangManager {

    private static final String DEFAULT_LOCALE = "en";
    private static final String[] BUNDLED = { "en", "ru" };

    private final JavaPlugin plugin;

    /** locale code -> flattened key/value map (values are String or List&lt;String&gt;). */
    private final Map<String, Map<String, Object>> locales = new ConcurrentHashMap<>();
    private final Set<String> warnedKeys = ConcurrentHashMap.newKeySet();

    private volatile String selectedLocale = DEFAULT_LOCALE;
    private volatile boolean perPlayerLocale = false;

    public LangManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load(Settings settings) {
        this.selectedLocale = settings.language().toLowerCase(Locale.ROOT);
        this.perPlayerLocale = settings.perPlayerLocale();
        this.warnedKeys.clear();
        this.locales.clear();

        extractBundled();
        discoverFiles();
        checkVersions();

        if (!locales.containsKey(DEFAULT_LOCALE)) {
            plugin.getLogger().severe("Default language file en.yml is missing; messages will show raw keys.");
            locales.put(DEFAULT_LOCALE, new HashMap<>());
        }
        if (!locales.containsKey(selectedLocale)) {
            plugin.getLogger().warning("Language '" + selectedLocale + "' not found; falling back to '"
                    + DEFAULT_LOCALE + "'.");
            selectedLocale = DEFAULT_LOCALE;
        }
    }

    private void extractBundled() {
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists() && !langDir.mkdirs()) {
            plugin.getLogger().warning("Could not create lang directory.");
        }
        for (String code : BUNDLED) {
            File file = new File(langDir, code + ".yml");
            if (!file.exists()) {
                plugin.saveResource("lang/" + code + ".yml", false);
            }
        }
    }

    private void discoverFiles() {
        File langDir = new File(plugin.getDataFolder(), "lang");
        File[] files = langDir.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            String code = file.getName().substring(0, file.getName().length() - 4).toLowerCase(Locale.ROOT);
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            locales.put(code, flatten(yaml));
        }
    }

    private void checkVersions() {
        for (String code : BUNDLED) {
            Map<String, Object> bundled = loadBundled(code);
            Map<String, Object> onDisk = locales.get(code);
            if (bundled == null || onDisk == null) {
                continue;
            }
            Object bundledVer = bundled.get("lang-version");
            Object diskVer = onDisk.get("lang-version");
            if (bundledVer != null && !bundledVer.equals(diskVer)) {
                Set<String> newKeys = new TreeSet<>(bundled.keySet());
                newKeys.removeAll(onDisk.keySet());
                Set<String> removedKeys = new TreeSet<>(onDisk.keySet());
                removedKeys.removeAll(bundled.keySet());
                plugin.getLogger().warning("lang/" + code + ".yml is version " + diskVer
                        + " but the plugin ships version " + bundledVer + ".");
                if (!newKeys.isEmpty()) {
                    plugin.getLogger().warning("  New keys (using en fallback until added): " + newKeys);
                }
                if (!removedKeys.isEmpty()) {
                    plugin.getLogger().warning("  Keys no longer used: " + removedKeys);
                }
            }
        }
    }

    private Map<String, Object> loadBundled(String code) {
        try (InputStream in = plugin.getResource("lang/" + code + ".yml")) {
            if (in == null) {
                return null;
            }
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(in, StandardCharsets.UTF_8));
            return flatten(yaml);
        } catch (Exception ex) {
            return null;
        }
    }

    private Map<String, Object> flatten(YamlConfiguration yaml) {
        Map<String, Object> map = new HashMap<>();
        for (String key : yaml.getKeys(true)) {
            if (yaml.isConfigurationSection(key)) {
                continue;
            }
            if (yaml.isList(key)) {
                map.put(key, yaml.getStringList(key));
            } else {
                map.put(key, yaml.get(key));
            }
        }
        return map;
    }

    // ---- locale selection ---------------------------------------------------

    public String localeFor(Player player) {
        if (perPlayerLocale && player != null) {
            String tag = player.locale().getLanguage().toLowerCase(Locale.ROOT);
            if (locales.containsKey(tag)) {
                return tag;
            }
        }
        return selectedLocale;
    }

    public String defaultLocale() {
        return selectedLocale;
    }

    // ---- rendering ----------------------------------------------------------

    public String cancelWord(String locale) {
        Object value = rawValue(locale, "cancel-word");
        return value instanceof String s ? s : "cancel";
    }

    /** Renders a message key as a single component (list values are joined by newlines). */
    public Component render(String locale, String key, TagResolver... resolvers) {
        Object value = rawValue(locale, key);
        if (value == null) {
            return Component.text(key);
        }
        TagResolver[] all = withPrefix(locale, resolvers);
        if (value instanceof List<?> list) {
            Component result = Component.empty();
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    result = result.append(Component.newline());
                }
                result = result.append(Text.parse(String.valueOf(list.get(i)), all));
            }
            return result;
        }
        return Text.parse(String.valueOf(value), all);
    }

    /** Renders a message key as a list of components (one per line). */
    public List<Component> renderList(String locale, String key, TagResolver... resolvers) {
        Object value = rawValue(locale, key);
        if (value == null) {
            return List.of(Component.text(key));
        }
        TagResolver[] all = withPrefix(locale, resolvers);
        List<Component> out = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object line : list) {
                out.add(Text.parse(String.valueOf(line), all));
            }
        } else {
            out.add(Text.parse(String.valueOf(value), all));
        }
        return out;
    }

    /** Renders a message key to a plain string (for PlaceholderAPI and logs). */
    public String renderPlain(String locale, String key, TagResolver... resolvers) {
        return Text.plain(render(locale, key, resolvers));
    }

    public void send(Audience audience, String locale, String key, TagResolver... resolvers) {
        Component component = render(locale, key, resolvers);
        audience.sendMessage(component);
    }

    public void sendList(Audience audience, String locale, String key, TagResolver... resolvers) {
        for (Component line : renderList(locale, key, resolvers)) {
            audience.sendMessage(line);
        }
    }

    private TagResolver[] withPrefix(String locale, TagResolver[] resolvers) {
        Object prefix = rawValue(locale, "prefix");
        Component prefixComponent = prefix instanceof String s ? Text.parse(s) : Component.empty();
        TagResolver[] all = new TagResolver[resolvers.length + 1];
        all[0] = Placeholder.component("prefix", prefixComponent);
        System.arraycopy(resolvers, 0, all, 1, resolvers.length);
        return all;
    }

    private Object rawValue(String locale, String key) {
        Map<String, Object> primary = locales.getOrDefault(locale, Collections.emptyMap());
        Map<String, Object> fallback = locales.getOrDefault(DEFAULT_LOCALE, Collections.emptyMap());
        Optional<Object> resolved = LangResolver.resolve(primary, fallback, key);
        if (resolved.isEmpty()) {
            if (warnedKeys.add(key)) {
                plugin.getLogger().warning("Missing language key '" + key + "' in both '" + locale
                        + "' and '" + DEFAULT_LOCALE + "'. Showing the key itself.");
            }
            return null;
        }
        return resolved.get();
    }
}
