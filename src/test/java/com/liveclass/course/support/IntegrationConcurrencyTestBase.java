package com.liveclass.course.support;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=false",
        "spring.jpa.properties.hibernate.format_sql=false",
        "logging.level.org.hibernate.SQL=warn",
        "logging.level.org.hibernate.orm.jdbc.bind=warn",
        // 동시성 테스트가 10 thread 이상 사용 + main thread / @AfterEach truncate 도 connection 필요.
        // Hikari 기본 pool size 10이면 deadlock 발생 → 충분히 크게 설정.
        "spring.datasource.hikari.maximum-pool-size=30",
        "spring.datasource.hikari.connection-timeout=10000"
})
public abstract class IntegrationConcurrencyTestBase extends AbstractMySqlContainerTest {

    private static final List<String> TABLES = List.of(
            "payment", "enrollment", "waitlist_entry", "live_class", "users"
    );

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @AfterEach
    void truncateAll() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        for (String table : TABLES) {
            jdbcTemplate.execute("TRUNCATE TABLE " + table);
        }
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }
}
