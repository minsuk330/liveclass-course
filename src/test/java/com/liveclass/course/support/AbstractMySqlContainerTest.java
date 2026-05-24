package com.liveclass.course.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

/**
 * 모든 통합 테스트가 공유하는 단일 MySQL Testcontainer.
 *
 * <p>JUnit의 {@code @Testcontainers} / {@code @Container} 기반 라이프사이클은 상속/클래스 캐시
 * 조합에서 컨테이너가 중간에 죽거나 새로 뜨는 문제를 일으켰다.
 * static initializer로 직접 start하면 JVM 1회만 부팅되고 JVM 종료 시까지 살아있다.
 *
 * <p>{@code @DynamicPropertySource}로 URL/계정을 Spring 환경에 주입한다.
 * {@code @ServiceConnection}의 자동 매직보다 명시적이며 cache key 영향이 적다.
 */
public abstract class AbstractMySqlContainerTest {

    @SuppressWarnings("resource")
    protected static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("course")
            .withUsername("course")
            .withPassword("course")
            .withCommand(
                    "--character-set-server=utf8mb4",
                    "--collation-server=utf8mb4_unicode_ci"
            );

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void registerDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }
}
