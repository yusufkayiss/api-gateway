package com.ecommerce.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
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
class RateLimiterIntegrationTest {

    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void setRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
    }

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("Redis Testcontainer üzerinden Actuator Health kontrolü yapılmalı")
    void shouldVerifyRedisConnectionViaHealthEndpoint() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    @DisplayName("Burst kapasitesi (20) aşıldığında 429 Too Many Requests dönmeli")
    void shouldReturn429WhenRateLimitExceeded() {
        stubFor(get(urlEqualTo("/api/v1/products"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"products\":[]}")));

        List<HttpStatusCode> statuses = new ArrayList<>();

        for (int i = 0; i < 45; i++) {
            webTestClient.get()
                    .uri("/api/v1/products")
                    .exchange()
                    .expectBody()
                    .consumeWith(result -> statuses.add(result.getStatus()));
        }

        assertThat(statuses).contains(HttpStatus.OK);
        assertThat(statuses).contains(HttpStatus.TOO_MANY_REQUESTS);
    }
}