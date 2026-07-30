package io.github.sheynor43.clans.storage;

/**
 * Encapsulates the few SQL differences between SQLite and MySQL/MariaDB so a
 * single schema definition works on both. Identifiers are backtick-quoted, which
 * both engines accept.
 */
public enum SqlDialect {

    SQLITE("", "INTEGER", "BIGINT", "DOUBLE"),
    MYSQL(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4", "INT", "BIGINT", "DOUBLE");

    private final String tableSuffix;
    private final String intType;
    private final String bigIntType;
    private final String doubleType;

    SqlDialect(String tableSuffix, String intType, String bigIntType, String doubleType) {
        this.tableSuffix = tableSuffix;
        this.intType = intType;
        this.bigIntType = bigIntType;
        this.doubleType = doubleType;
    }

    public String tableSuffix() {
        return tableSuffix;
    }

    public String intType() {
        return intType;
    }

    public String bigIntType() {
        return bigIntType;
    }

    public String doubleType() {
        return doubleType;
    }
}
