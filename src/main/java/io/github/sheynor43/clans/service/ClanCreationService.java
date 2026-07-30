package io.github.sheynor43.clans.service;

import io.github.sheynor43.clans.config.Settings;
import io.github.sheynor43.clans.logic.NameValidator;
import io.github.sheynor43.clans.logic.TagValidator;
import io.github.sheynor43.clans.model.Clan;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Shared validation and creation used by both the interactive dialog and the
 * quick {@code /clan create <name> <tag>} form. Validators are rebuilt from the
 * live settings so a reload takes effect without a restart.
 */
public final class ClanCreationService {

    public enum NameCheck {
        OK, TOO_SHORT, TOO_LONG, BLACKLISTED, TAKEN
    }

    public enum TagCheck {
        OK, BAD_FORMAT, TOO_SHORT, TOO_LONG, TAKEN
    }

    private final Supplier<Settings> settings;
    private final ClanManager clans;

    public ClanCreationService(Supplier<Settings> settings, ClanManager clans) {
        this.settings = settings;
        this.clans = clans;
    }

    public NameCheck checkName(String name) {
        Settings s = settings.get();
        NameValidator validator = new NameValidator(s.nameMin(), s.nameMax(), s.nameBlacklist());
        NameValidator.Result result = validator.validate(name);
        return switch (result) {
            case TOO_SHORT -> NameCheck.TOO_SHORT;
            case TOO_LONG -> NameCheck.TOO_LONG;
            case BLACKLISTED -> NameCheck.BLACKLISTED;
            case OK -> clans.isNameTaken(name.strip()) ? NameCheck.TAKEN : NameCheck.OK;
        };
    }

    public TagCheck checkTag(String tag) {
        Settings s = settings.get();
        TagValidator validator = new TagValidator(s.tagMin(), s.tagMax());
        TagValidator.Result result = validator.validate(tag);
        return switch (result) {
            case BAD_FORMAT -> TagCheck.BAD_FORMAT;
            case TOO_SHORT -> TagCheck.TOO_SHORT;
            case TOO_LONG -> TagCheck.TOO_LONG;
            case OK -> clans.isTagTaken(tag) ? TagCheck.TAKEN : TagCheck.OK;
        };
    }

    /**
     * Creates the clan after a final uniqueness re-check.
     *
     * @return the new clan, or {@code null} if a name/tag collision was detected
     *         at the last moment (caller should report the appropriate error).
     */
    public Clan create(UUID leader, String leaderName, String name, String tag) {
        String cleanName = name.strip();
        if (clans.isNameTaken(cleanName) || clans.isTagTaken(tag)) {
            return null;
        }
        return clans.createClan(cleanName, tag, leader, leaderName);
    }
}
