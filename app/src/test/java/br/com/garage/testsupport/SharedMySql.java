package br.com.garage.testsupport;

import org.testcontainers.mysql.MySQLContainer;

public final class SharedMySql {

    private static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");

    static {
        MYSQL.start();
    }

    private SharedMySql() {}

    public static String jdbcUrl() {
        return MYSQL.getJdbcUrl();
    }

    public static String username() {
        return MYSQL.getUsername();
    }

    public static String password() {
        return MYSQL.getPassword();
    }
}
