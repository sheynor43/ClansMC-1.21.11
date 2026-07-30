package io.github.sheynor43.clans.storage;

import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.util.logging.Logger;

/** SQLite-backed storage: a single local database file, zero external setup. */
public final class SqliteClanStorage extends SqlClanStorage {

    private final File databaseFile;
    private final ClassLoader classLoader;

    public SqliteClanStorage(File databaseFile, ClassLoader classLoader, Logger logger) {
        super(SqlDialect.SQLITE, logger);
        this.databaseFile = databaseFile;
        this.classLoader = classLoader;
    }

    @Override
    protected HikariDataSource createDataSource() {
        File parent = databaseFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Could not create storage directory: " + parent);
        }
        return HikariProvider.createSqlite(databaseFile, classLoader);
    }
}
