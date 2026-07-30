package io.github.sheynor43.clans.storage.migration;

import io.github.sheynor43.clans.storage.SqlDialect;

import java.sql.Connection;
import java.sql.SQLException;

/** A single, ordered, idempotent schema change. */
public interface Migration {

    /** Monotonically increasing version. Applied only when greater than the stored version. */
    int version();

    /** Human-readable description for logging. */
    String description();

    /** Applies the change. Should tolerate re-runs where practical. */
    void apply(Connection connection, SqlDialect dialect) throws SQLException;
}
