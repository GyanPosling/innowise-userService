package com.innowise.userservice;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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
    protected static final String TS_HEADER = "X-TS";
    protected static final String SIGN_HEADER = "X-SIGN";
    protected static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";
    protected static final String INTERNAL_SECRET = "test-internal-secret";
    protected static final String SIGN_ALGORITHM = "HmacSHA256";

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
        registry.add("userservice.internal-endpoint-secret", () -> INTERNAL_SECRET);
    }

    protected HttpHeaders adminHeaders(String method, String path) {
        return buildHeaders(999L, "admin@example.com", "ADMIN", method, path);
    }

    protected HttpHeaders userHeaders(Long userId, String email, String method, String path) {
        return buildHeaders(userId, email, "USER", method, path);
    }

    protected HttpHeaders internalHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(INTERNAL_SECRET_HEADER, INTERNAL_SECRET);
        return headers;
    }

    private HttpHeaders buildHeaders(Long userId, String email, String role, String method, String path) {
        HttpHeaders headers = new HttpHeaders();
        String ts = String.valueOf(Instant.now().getEpochSecond());
        String signature = sign(buildPayload(method, path, String.valueOf(userId), role, email, email, ts));
        headers.add(USER_ID_HEADER, String.valueOf(userId));
        headers.add(USER_ROLE_HEADER, role);
        headers.add(USER_EMAIL_HEADER, email);
        headers.add(USERNAME_HEADER, email);
        headers.add(TS_HEADER, ts);
        headers.add(SIGN_HEADER, signature);
        return headers;
    }

    private String buildPayload(
            String method,
            String path,
            String userId,
            String role,
            String email,
            String username,
            String ts
    ) {
        return String.join(
                "|",
                emptyIfNull(method),
                emptyIfNull(path),
                emptyIfNull(userId),
                emptyIfNull(role),
                emptyIfNull(email),
                emptyIfNull(username),
                ts
        );
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(SIGN_ALGORITHM);
            mac.init(new SecretKeySpec(INTERNAL_SECRET.getBytes(StandardCharsets.UTF_8), SIGN_ALGORITHM));
            return Base64.getEncoder().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
