package io.github.sheynor43.clans.logic;

import java.util.regex.Pattern;

/**
 * Validates clan tags: a single {@code [a-zA-Z0-9]} word within the configured
 * length bounds. Case is preserved; uniqueness is checked separately. Pure logic.
 */
public final class TagValidator {

    public enum Result {
        OK,
        BAD_FORMAT,
        TOO_SHORT,
        TOO_LONG
    }

    private static final Pattern WORD = Pattern.compile("[a-zA-Z0-9]+");

    private final int minLength;
    private final int maxLength;

    public TagValidator(int minLength, int maxLength) {
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    public Result validate(String rawTag) {
        if (rawTag == null || !WORD.matcher(rawTag).matches()) {
            return Result.BAD_FORMAT;
        }
        int length = rawTag.length();
        if (length < minLength) {
            return Result.TOO_SHORT;
        }
        if (length > maxLength) {
            return Result.TOO_LONG;
        }
        return Result.OK;
    }
}
