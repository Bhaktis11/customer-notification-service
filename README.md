# Customer Notification Service

Spring Boot service that accepts notification requests quickly and delivers them asynchronously, with idempotency and retries.

## Tech stack
Java 17, Spring Boot 4.1.2, Spring Data JPA, H2 (in-memory), JUnit 5, Mockito

## How to run
1. `mvnw.cmd spring-boot:run` (Windows) or `./mvnw spring-boot:run`
2. The app starts on http://localhost:8080
3. H2 console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:notificationdb`, user `sa`, empty password)
4. Run tests: `mvnw.cmd test`

## API
**Create notification**
`POST /notifications`
```json
{
  "customerId": "CUST101",
  "channel": "SMS",
  "message": "Payment Successful",
  "idempotencyKey": "PAY12345"
}
```
Response `202 Accepted`: `{"notificationId": "N...", "status": "QUEUED"}`

**Check status**
`GET /notifications/{id}` returns the current status and retry count.

| Case | Result |
|---|---|
| Invalid input (for example unknown channel) | 400 |
| Unknown notification id | 404 |
| Same idempotency key, different payload | 409 |
| Same idempotency key, same payload | 202 with the original notificationId |

## Design
- **Fast API:** the request path validates, saves one QUEUED row and returns 202. Delivery happens in the background.
- **Worker:** a scheduled job picks up QUEUED and due RETRYING notifications and sends them through a mock provider (30% random failure).
- **Statuses:** QUEUED, PROCESSING, DELIVERED, RETRYING, FAILED
- **Retries:** exponential backoff (2s, 4s, 8s) with jitter, maximum 3 retries, then FAILED. Retry state (`retry_count`, `next_retry_at`) is stored in the DB, so it survives restarts.
- **Idempotency:** a UNIQUE constraint on (customer_id, idempotency_key) guarantees no duplicate rows, even for simultaneous requests.
- **Errors:** a global `@RestControllerAdvice` returns clean JSON errors.

## Limitations and production improvements
- Replace the polling worker with a Kafka producer and consumers, with retry topics and a dead-letter queue.
- Cache status lookups and idempotency checks in Redis.
- Use PostgreSQL with Flyway migrations instead of H2 and `ddl-auto`.
- For multiple instances, use consumer groups or `SELECT ... FOR UPDATE SKIP LOCKED`, and add recovery for rows stuck in PROCESSING.
- Add metrics, structured logging, authentication and rate limiting.