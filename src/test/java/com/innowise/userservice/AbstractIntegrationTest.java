package com.innowise.userservice;

import org.springframework.http.HttpHeaders;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractIntegrationTest {

    protected static final String USER_ID_HEADER = "X-USER-ID";
    protected static final String USER_ROLE_HEADER = "X-USER-ROLE";
    protected static final String USER_EMAIL_HEADER = "X-USER-EMAIL";
    protected static final String USERNAME_HEADER = "X-USER-NAME";
    protected static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";
    protected static final String INTERNAL_SECRET = "test-internal-secret";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));

        registry.add("app.cache.enabled", () -> "false");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("internal.secret", () -> INTERNAL_SECRET);
    }

    protected HttpHeaders adminHeaders() {
        return buildHeaders(999L, "admin@example.com", "ADMIN");
    }

    protected HttpHeaders userHeaders(Long userId, String email) {
        return buildHeaders(userId, email, "USER");
    }

    protected HttpHeaders internalHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(INTERNAL_SECRET_HEADER, INTERNAL_SECRET);
        return headers;
    }

    private HttpHeaders buildHeaders(Long userId, String email, String role) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(USER_ID_HEADER, String.valueOf(userId));
        headers.add(USER_ROLE_HEADER, role);
        headers.add(USER_EMAIL_HEADER, email);
        headers.add(USERNAME_HEADER, email);
        return headers;
    }
}
