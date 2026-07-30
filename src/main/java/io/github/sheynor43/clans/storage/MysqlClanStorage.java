package io.github.sheynor43.clans.storage;

import com.zaxxer.hikari.HikariDataSource;
import io.github.sheynor43.clans.config.Settings;

import java.util.logging.Logger;

/** MySQL/MariaDB-backed storage using the MariaDB connector (works with both). */
public final class MysqlClanStorage extends SqlClanStorage {

    private final Settings settings;
    private final ClassLoader classLoader;

    public MysqlClanStorage(Settings settings, ClassLoader classLoader, Logger logger) {
        super(SqlDialect.MYSQL, logger);
        this.settings = settings;
        this.classLoader = classLoader;
    }

    @Override
    protected HikariDataSource createDataSource() {
        return HikariProvider.createMysql(
                settings.mysqlHost(),
                settings.mysqlPort(),
                settings.mysqlDatabase(),
                settings.mysqlUser(),
                settings.mysqlPassword(),
                settings.mysqlPoolSize(),
                settings.mysqlProperties(),
                classLoader);
    }
}
