package br.com.garage.outbound.persistence;

import br.com.garage.fixture.TestGarage;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.mysql.MySQLContainer;

final class MySqlSupport {

    private static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");
    private static final DataSource DATA_SOURCE;

    static {
        MYSQL.start();
        DATA_SOURCE = new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        Flyway.configure().dataSource(DATA_SOURCE).load().migrate();
    }

    private MySqlSupport() {}

    static DataSource dataSource() {
        return DATA_SOURCE;
    }

    static JdbcClient jdbc() {
        return JdbcClient.create(DATA_SOURCE);
    }

    static void resetWithTestGarage() {
        JdbcClient jdbc = jdbc();
        jdbc.sql("DELETE FROM parking_session").update();
        jdbc.sql("DELETE FROM capacity_counter").update();
        jdbc.sql("DELETE FROM spot").update();
        jdbc.sql("DELETE FROM sector").update();
        new JdbcSectorRepository(jdbc).saveAll(TestGarage.CONFIG.sectors());
        new JdbcSpotRepository(jdbc).saveAll(TestGarage.CONFIG.spots());
    }
}
