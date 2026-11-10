package com.ecommerce.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder,
                                           RedisRateLimiter redisRateLimiter,
                                           KeyResolver ipKeyResolver) {
        return builder.routes()
                .route("ecommerce-core-api", r -> r
                        .path("/api/v1/products/**", "/api/v1/orders/**", "/api/v1/auth/**")
                        .filters(f -> f.requestRateLimiter(c -> c
                                .setRateLimiter(redisRateLimiter)
                                .setKeyResolver(ipKeyResolver)))
                        .uri("http://localhost:8081"))
                .route("notification-service", r -> r
                        .path("/api/v1/notifications/**")
                        .uri("http://localhost:8082"))
                .build();
    }
}