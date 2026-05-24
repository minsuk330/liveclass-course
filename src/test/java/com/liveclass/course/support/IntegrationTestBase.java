package com.liveclass.course.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=false",
        "spring.jpa.properties.hibernate.format_sql=false",
        "logging.level.org.hibernate.SQL=warn",
        "logging.level.org.hibernate.orm.jdbc.bind=warn",
        // 동시성 테스트와 properties 통일 → 같은 Spring context 캐싱 가능
        "spring.datasource.hikari.maximum-pool-size=30",
        "spring.datasource.hikari.connection-timeout=10000"
})
public abstract class IntegrationTestBase extends AbstractMySqlContainerTest {
}
