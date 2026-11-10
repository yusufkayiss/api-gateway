Set-Content -Path README.md -Value @"
# 🚀 E-Commerce API Gateway Service

Central API Gateway implementation for Microservices Architecture built with **Java 21**, **Spring Boot 3**, **Spring Cloud Gateway**, and **Redis Rate Limiting**.

---

## 🛠 Tech Stack & Architecture

* **Java Version:** 21 (LTS)
* **Framework:** Spring Boot 3.x / Spring Cloud Gateway (Reactive WebFlux)
* **Rate Limiting:** Redis (Token Bucket Algorithm)
* **Containerization:** Docker (\`redis:7-alpine\`)
* **Configuration:** Programmatic Routing via \`RouteLocatorBuilder\` (Java Config)

---

## 📌 Architecture Highlights

* **Programmatic Routing:** Built-in routes configured explicitly via Java (\`GatewayConfig.java\`) for type safety and fast execution.
* **IP-Based Rate Limiting:** Implemented custom \`KeyResolver\` (\`RateLimiterConfig.java\`) to extract request source IP addresses safely without throwing NPEs.
* **Distributed Rate Limiter:** Integrated Spring Data Reactive Redis to handle rate limiting dynamically across distributed environments.

---

## ⚙️ How to Run

### 1. Prerequisites
Ensure Docker is running on your system.

### 2. Start Redis Container
\`\`\`bash
docker run -d --name ecommerce-redis -p 6379:6379 redis:7-alpine
\`\`\`

### 3. Run Gateway Application
\`\`\`bash
./mvnw spring-boot:run
\`\`\`

---

## 🧪 Testing Rate Limits

To test the Redis Rate Limiter (\`10 replenish rate / 20 burst capacity\`), execute the following command in PowerShell:

\`\`\`powershell
1..30 | ForEach-Object { 
    try { 
        (Invoke-WebRequest -Uri "http://localhost:8080/api/v1/products/1" -UseBasicParsing).StatusCode 
    } catch { 
        \$_.Exception.Response.StatusCode.Value__ 
    } 
}
\`\`\`

* **Expected Output:**
  * **First 20 requests:** \`500\` (Backend down / \`Connection Refused\`)
  * **Subsequent requests (21+):** \`429 Too Many Requests\` (Rate limit exceeded, blocked by Redis)
"@ ; git add README.md ; git commit -m "docs: add comprehensive README with architecture & rate limiter documentation" ; git push origin main
