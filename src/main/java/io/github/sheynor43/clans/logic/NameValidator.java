package io.github.sheynor43.clans.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Validates clan names against length bounds and a case-insensitive blacklist.
 * Uniqueness is checked separately by the clan service. Pure logic, unit-tested.
 */
public final class NameValidator {

    public enum Result {
        OK,
        TOO_SHORT,
        TOO_LONG,
        BLACKLISTED
    }

    private final int minLength;
    private final int maxLength;
    private final List<String> blacklistLower;

    public NameValidator(int minLength, int maxLength, List<String> blacklist) {
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.blacklistLower = new ArrayList<>();
        for (String word : blacklist) {
            if (word != null && !word.isBlank()) {
                this.blacklistLower.add(word.toLowerCase(Locale.ROOT));
            }
        }
    }

    public Result validate(String rawName) {
        if (rawName == null) {
            return Result.TOO_SHORT;
        }
        String name = rawName.strip();
        int length = name.length();
        if (length < minLength) {
            return Result.TOO_SHORT;
        }
        if (length > maxLength) {
            return Result.TOO_LONG;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        for (String bad : blacklistLower) {
            if (lower.contains(bad)) {
                return Result.BLACKLISTED;
            }
        }
        return Result.OK;
    }
}
