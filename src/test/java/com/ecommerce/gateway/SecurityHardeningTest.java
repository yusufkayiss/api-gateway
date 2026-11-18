package com.ecommerce.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "services.core-api.uri=http://localhost:${wiremock.server.port}",
                "services.notification.uri=http://localhost:${wiremock.server.port}"
        }
)
@AutoConfigureWireMock(port = 0)
@Testcontainers
class SecurityHardeningTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldAttachEnterpriseSecurityHeadersToResponses() {
        stubFor(get(urlEqualTo("/api/v1/products"))
                .willReturn(aResponse().withStatus(200).withBody("[]")));

        webTestClient.get()
                .uri("/api/v1/products")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Content-Type-Options", "nosniff")
                .expectHeader().valueEquals("X-Frame-Options", "DENY")
                .expectHeader().valueEquals("Referrer-Policy", "strict-origin-when-cross-origin")
                .expectHeader().exists("Content-Security-Policy")
                .expectHeader().exists("Strict-Transport-Security");
    }

    @Test
    void shouldApplyStrictRateLimitOnAuthEndpoints() {
        stubFor(post(urlEqualTo("/api/v1/auth/login"))
                .willReturn(aResponse().withStatus(401).withBody("{\"error\":\"Bad credentials\"}")));

        List<HttpStatusCode> statuses = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            webTestClient.post()
                    .uri("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"email\":\"attacker@test.com\",\"password\":\"123\"}")
                    .exchange()
                    .expectBody()
                    .consumeWith(res -> statuses.add(res.getStatus()));
        }

        assertThat(statuses).contains(HttpStatus.TOO_MANY_REQUESTS);
    }
}