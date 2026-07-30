package io.github.sheynor43.clans;

import io.github.sheynor43.clans.logic.LangResolver;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LangResolverTest {

    @Test
    void primaryWins() {
        Map<String, Object> primary = Map.of("greeting", "Привет");
        Map<String, Object> fallback = Map.of("greeting", "Hello");
        assertEquals(Optional.of("Привет"), LangResolver.resolve(primary, fallback, "greeting"));
    }

    @Test
    void fallsBackWhenMissingInPrimary() {
        Map<String, Object> primary = Map.of();
        Map<String, Object> fallback = Map.of("greeting", "Hello");
        assertEquals(Optional.of("Hello"), LangResolver.resolve(primary, fallback, "greeting"));
    }

    @Test
    void emptyStringIsTreatedAsMissing() {
        Map<String, Object> primary = Map.of("greeting", "");
        Map<String, Object> fallback = Map.of("greeting", "Hello");
        assertEquals(Optional.of("Hello"), LangResolver.resolve(primary, fallback, "greeting"));
    }

    @Test
    void listValuesAreSupported() {
        Map<String, Object> primary = Map.of("lines", List.of("a", "b"));
        Optional<Object> result = LangResolver.resolve(primary, Map.of(), "lines");
        assertTrue(result.isPresent());
        assertEquals(List.of("a", "b"), result.get());
    }

    @Test
    void missingInBothIsEmpty() {
        assertTrue(LangResolver.resolve(Map.of(), Map.of(), "nope").isEmpty());
    }
}
