package io.github.sheynor43.clans;

import io.github.sheynor43.clans.logic.NameValidator;
import io.github.sheynor43.clans.logic.TagValidator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValidatorTest {

    private final NameValidator names = new NameValidator(3, 16, List.of("admin", "staff"));
    private final TagValidator tags = new TagValidator(2, 5);

    @Test
    void nameLengthBounds() {
        assertEquals(NameValidator.Result.TOO_SHORT, names.validate("ab"));
        assertEquals(NameValidator.Result.TOO_LONG, names.validate("thisnameiswaytoolong"));
        assertEquals(NameValidator.Result.OK, names.validate("Knights"));
    }

    @Test
    void nameAllowsSpaces() {
        assertEquals(NameValidator.Result.OK, names.validate("Red Wolves"));
    }

    @Test
    void nameBlacklistIsCaseInsensitive() {
        assertEquals(NameValidator.Result.BLACKLISTED, names.validate("ADMIN Team"));
        assertEquals(NameValidator.Result.BLACKLISTED, names.validate("theStaffClub"));
    }

    @Test
    void tagFormatRejectsSymbolsAndSpaces() {
        assertEquals(TagValidator.Result.BAD_FORMAT, tags.validate("a b"));
        assertEquals(TagValidator.Result.BAD_FORMAT, tags.validate("a-b"));
        assertEquals(TagValidator.Result.BAD_FORMAT, tags.validate("cl@n"));
    }

    @Test
    void tagLengthBounds() {
        assertEquals(TagValidator.Result.TOO_SHORT, tags.validate("A"));
        assertEquals(TagValidator.Result.TOO_LONG, tags.validate("ABCDEF"));
        assertEquals(TagValidator.Result.OK, tags.validate("AB12"));
    }
}
