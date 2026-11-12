package com.ecommerce.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Value("${services.core-api.uri}")
    private String coreApiUri;

    @Value("${services.notification.uri}")
    private String notificationUri;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder,
                                           RedisRateLimiter redisRateLimiter,
                                           KeyResolver userKeyResolver) {
        return builder.routes()
                .route("ecommerce-core-api", r -> r
                        .path("/api/v1/products/**", "/api/v1/orders/**", "/api/v1/auth/**")
                        .filters(f -> f
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(redisRateLimiter)
                                        .setKeyResolver(userKeyResolver))
                                .circuitBreaker(c -> c
                                        .setName("coreApiCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/core-api"))
                                .addRequestHeader("X-Gateway-Source", "api-gateway"))
                        .uri(coreApiUri))
                .route("notification-service", r -> r
                        .path("/api/v1/notifications/**")
                        .filters(f -> f
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(redisRateLimiter)
                                        .setKeyResolver(userKeyResolver))
                                .circuitBreaker(c -> c
                                        .setName("notificationCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/notifications"))
                                .addRequestHeader("X-Gateway-Source", "api-gateway"))
                        .uri(notificationUri))
                .build();
    }
}