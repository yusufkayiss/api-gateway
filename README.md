# 🚀 API Gateway Service

[![Java CI with Maven](https://github.com/yusufkayiss/api-gateway/actions/workflows/maven.yml/badge.svg)](https://github.com/yusufkayiss/api-gateway/actions/workflows/maven.yml)

High-performance, reactive, and resilient API Gateway service for E-Commerce Microservices Architecture built with **Spring Cloud Gateway**, **Java 21**, and **Redis Rate Limiting**.

---

## 🌐 English

### 🚀 Overview
Central Entry Point and Reverse Proxy for the E-Commerce Microservices ecosystem. Designed to handle request routing, client identity resolution, dynamic rate limiting via Redis (Token Bucket Algorithm), and network resilience across distributed backend services.

### 🛠 Tech Stack
- **Language**: Java 21 (LTS)
- **Framework**: Spring Boot 3.x / Spring Cloud Gateway (Reactive WebFlux)
- **Rate Limiting**: Redis (`redis:7-alpine`)
- **Configuration**: Java-based Programmatic Routing (`RouteLocatorBuilder`)
- **Containerization**: Docker
- **Utilities**: Spring Data Reactive Redis

### 📐 Architecture & Request Flow

```mermaid
graph TD
    Client[Client / PowerShell / Postman] -->|HTTP Request| Gateway[Spring Cloud Gateway :8080]
    
    subgraph Gateway Engine
        Gateway -->|1. Resolve IP| KeyRes["KeyResolver (RateLimiterConfig)"]
        KeyRes -->|2. Check Tokens| RedisLimiter["RedisRateLimiter (10 rps / 20 burst)"]
        RedisLimiter <-->|Evaluate Limits| Redis[(Redis Docker Container :6379)]
    end
    
    RedisLimiter -->|Limit Exceeded 429| Client
    RedisLimiter -->|Allowed| Routing{"Route Matcher (GatewayConfig)"}
    
    Routing -->|/api/v1/products/**| CoreAPI["Core API Service :8081"]
    Routing -->|/api/v1/notifications/**| NotifAPI["Notification Service :8082"]
