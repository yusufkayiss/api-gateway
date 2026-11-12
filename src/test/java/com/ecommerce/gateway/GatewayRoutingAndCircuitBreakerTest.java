package com.ecommerce.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "services.core-api.uri=http://localhost:${wiremock.server.port}",
                "services.notification.uri=http://localhost:${wiremock.server.port}"
        }
)
@AutoConfigureWireMock(port = 0)
@Testcontainers
class GatewayRoutingAndCircuitBreakerTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldRouteToCoreApiAndInjectGatewayHeader() {
        stubFor(get(urlEqualTo("/api/v1/products"))
                .withHeader("X-Gateway-Source", equalTo("api-gateway"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"products\":[\"Laptop\",\"Phone\"]}")));

        webTestClient.get()
                .uri("/api/v1/products")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.products[0]").isEqualTo("Laptop");
    }

    @Test
    void shouldTriggerCircuitBreakerFallbackWhenServiceTimesOut() {
        stubFor(get(urlEqualTo("/api/v1/orders"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(4000)));

        webTestClient.get()
                .uri("/api/v1/orders")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody()
                .jsonPath("$.status").isEqualTo(503)
                .jsonPath("$.error").isEqualTo("Service Unavailable")
                .jsonPath("$.message").isEqualTo("Core E-Commerce Service is currently unavailable. Please try again later.");
    }
}