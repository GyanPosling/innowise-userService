package com.innowise.userservice;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
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

    protected static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";
    protected static final String INTERNAL_SECRET = "test-internal-secret";
    private static final String GATEWAY_SIGNING_SECRET = "test-gateway-signing-secret";
    private static final String USER_ID_HEADER = "X-USER-ID";
    private static final String USER_ROLE_HEADER = "X-USER-ROLE";
    private static final String USER_EMAIL_HEADER = "X-USER-EMAIL";
    private static final String USERNAME_HEADER = "X-USER-NAME";
    private static final String TS_HEADER = "X-TS";
    private static final String SIGN_HEADER = "X-SIGN";
    private static final String SIGN_ALGORITHM = "HmacSHA256";

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
        registry.add("security.internal.secret", () -> INTERNAL_SECRET);
        registry.add("gateway.internal-signing-secret", () -> GATEWAY_SIGNING_SECRET);
    }

    protected HttpHeaders internalHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(INTERNAL_SECRET_HEADER, INTERNAL_SECRET);
        return headers;
    }

    protected HttpHeaders adminHeaders(String method, String path) {
        return gatewayHeaders(
                UUID.fromString("00000000-0000-0000-0000-000000000999"),
                "admin@example.com",
                "ADMIN",
                method,
                path
        );
    }

    protected HttpHeaders userHeaders(UUID userId, String email, String method, String path) {
        return gatewayHeaders(userId, email, "USER", method, path);
    }

    protected UUID testUserId(int suffix) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", suffix));
    }

    private HttpHeaders gatewayHeaders(
            UUID userId,
            String email,
            String role,
            String method,
            String path
    ) {
        String ts = String.valueOf(Instant.now().getEpochSecond());
        String payload = String.join(
                "|",
                method,
                path,
                userId.toString(),
                role,
                email,
                email,
                ts
        );

        HttpHeaders headers = new HttpHeaders();
        headers.add(USER_ID_HEADER, userId.toString());
        headers.add(USER_ROLE_HEADER, role);
        headers.add(USER_EMAIL_HEADER, email);
        headers.add(USERNAME_HEADER, email);
        headers.add(TS_HEADER, ts);
        headers.add(SIGN_HEADER, sign(payload));
        return headers;
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(SIGN_ALGORITHM);
            mac.init(new SecretKeySpec(GATEWAY_SIGNING_SECRET.getBytes(StandardCharsets.UTF_8), SIGN_ALGORITHM));
            return Base64.getEncoder().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("Failed to sign gateway headers", ex);
        }
    }
}
