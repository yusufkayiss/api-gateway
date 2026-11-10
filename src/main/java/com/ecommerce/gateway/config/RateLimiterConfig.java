package com.ecommerce.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Optional;

@Configuration
public class RateLimiterConfig {

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        // replenishRate: 10, burstCapacity: 20, requestedTokens: 1
        return new RedisRateLimiter(10, 20, 1);
    }

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
                Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                        .map(InetSocketAddress::getAddress)
                        .map(inetAddress -> inetAddress.getHostAddress())
                        .orElse("anonymous")
        );
    }
}