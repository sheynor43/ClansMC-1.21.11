package io.github.sheynor43.clans.logic;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Pure key-resolution logic for the localization layer: look up a flattened key
 * in the primary locale, fall back to the default locale, then give up. Values
 * may be a single string or a list of strings. Unit-tested.
 */
public final class LangResolver {

    private LangResolver() {
    }

    /**
     * @param primary  the selected locale's flattened key/value map
     * @param fallback the default locale's flattened key/value map (usually en)
     * @param key      the dotted key to resolve
     * @return the raw value (String or {@code List<String>}) if found in either map
     */
    public static Optional<Object> resolve(Map<String, Object> primary,
                                           Map<String, Object> fallback,
                                           String key) {
        Object value = primary.get(key);
        if (isPresent(value)) {
            return Optional.of(value);
        }
        Object fallbackValue = fallback.get(key);
        if (isPresent(fallbackValue)) {
            return Optional.of(fallbackValue);
        }
        return Optional.empty();
    }

    private static boolean isPresent(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof String s) {
            return !s.isEmpty();
        }
        if (value instanceof List<?> list) {
            return !list.isEmpty();
        }
        return true;
    }
}
