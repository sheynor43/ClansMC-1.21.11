package io.github.sheynor43.clans.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Driver;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.StringJoiner;

/**
 * Builds a HikariCP {@link HikariDataSource} for either back-end. The bundled
 * JDBC drivers are force-registered through the plugin class loader so
 * {@code DriverManager} (used by Hikari's {@code jdbcUrl}) can find them despite
 * Shadow relocation and the isolated plugin class loader.
 */
public final class HikariProvider {

    private HikariProvider() {
    }

    /** Ensures the relocated JDBC drivers on the plugin class path are registered. */
    public static void registerDrivers(ClassLoader classLoader) {
        // Instantiating each provider triggers the driver's static registration block.
        for (Driver driver : ServiceLoader.load(Driver.class, classLoader)) {
            driver.getMajorVersion();
        }
    }

    public static HikariDataSource createSqlite(File databaseFile, ClassLoader classLoader) {
        registerDrivers(classLoader);
        HikariConfig config = new HikariConfig();
        config.setPoolName("ClansMC-SQLite");
        // sqlite-jdbc is bundled unrelocated (its JNI native library requires the
        // original org.sqlite package), so this name is used as-is at runtime.
        config.setDriverClassName("org.sqlite.JDBC");
        config.setJdbcUrl("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        // SQLite has a single writer; serialise access to avoid "database is locked".
        config.setMaximumPoolSize(1);
        config.setConnectionInitSql("PRAGMA busy_timeout = 5000");
        return new HikariDataSource(config);
    }

    public static HikariDataSource createMysql(String host, int port, String database,
                                               String user, String password, int poolSize,
                                               Map<String, String> properties, ClassLoader classLoader) {
        registerDrivers(classLoader);
        StringJoiner query = new StringJoiner("&");
        properties.forEach((k, v) -> query.add(k + "=" + v));
        String url = "jdbc:mariadb://" + host + ":" + port + "/" + database
                + (query.length() > 0 ? "?" + query : "");

        HikariConfig config = new HikariConfig();
        config.setPoolName("ClansMC-MySQL");
        // Relocated by Shadow at build time (see the SQLite note above).
        config.setDriverClassName("org.mariadb.jdbc.Driver");
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(Math.max(1, poolSize));
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        return new HikariDataSource(config);
    }
}
