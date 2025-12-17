# Instructions for candidates

This is the Java version of the Payment Gateway challenge. If you haven't already read this [README.md](https://github.com/cko-recruitment/) on the details of this exercise, please do so now.

## Requirements
- JDK 17
- Docker

## Template structure

src/ - A skeleton SpringBoot Application

test/ - Some simple JUnit tests

imposters/ - contains the bank simulator configuration. Don't change this

.editorconfig - don't change this. It ensures a consistent set of rules for submissions when reformatting code

docker-compose.yml - configures the bank simulator


## API Documentation
For documentation openAPI is included, and it can be found under the following url: **http://localhost:8090/swagger-ui/index.html**

## Design & Implementation Notes

This document outlines the architectural decisions, system behaviors, and trade-offs made during the implementation of the Payment Gateway.

### Architecture: Layered & Domain-Centric
The system follows a clean, four-layer architecture designed to separate concerns and isolate business logic.

* **1. Interface Layer (Web API)**:
  * **Location**: `com.checkout.payment.gateway.interfaces`
  * **Responsibility**: Handles HTTP entry points (`PaymentGatewayController`), DTO validation, and error mapping.
  * **Role**: It acts as the "Entry Adapter", converting external HTTP requests into internal commands.

* **2. Application Layer (Orchestration)**:
  * **Location**: `com.checkout.payment.gateway.application`
  * **Responsibility**: Contains `PaymentGatewayService`.
  * **Key Decision**: This layer orchestrates the flow. It does not contain business rules (which belong to Domain) nor HTTP details (Interface). It simply coordinates the transaction: "Validate -> Persist(Pending) -> Call Bank -> Persist(Result)".

* **3. Domain Layer (The Core)**:
  * **Location**: `com.checkout.payment.gateway.domain`
  * **Responsibility**: Encapsulates the core business state (`Payment`, `PaymentStatus`) and defines contracts/ports (`PaymentsRepository`, `AcquiringBank`).
  * **Key Decision**: This layer is pure Java and framework-agnostic. It defines what needs to be done, without knowing how.

* **4. Infrastructure Layer**:
  * **Location**: `com.checkout.payment.gateway.infrastructure`
  * **Responsibility**: Implements the domain interfaces.
  * **Role**: Provides the technical capabilities (e.g., `AcquiringBankImpl` via RestTemplate, `InMemoryPaymentsRepository` via ConcurrentHashMap) to support the domain.
  
### Payment Lifecycle & State Management
Managing the "indeterminate state" of distributed transactions is the core challenge of a payment gateway. The system implements a **"Pending-by-Default"** consistency model.

1. **Validation**: Requests are validated at the entry point. Invalid requests are rejected immediately (HTTP 400) and never enter the domain or storage.
2. **Pending**: Valid requests are strictly persisted as `PENDING` before any external bank call is made. This ensures an audit trail exists for reconciliation.
3. **Processing**: The system attempts to communicate with the Acquiring Bank.
4. **Finalization**:
  * If the bank returns a definitive success or decline, the state is updated to `AUTHORIZED` or `DECLINED`.
  * If the bank returns a malformed response, the state is updated to `REJECTED`.
  * If the bank times out, returns a 5xx error, the state is updated to `UNKNOWN`. 

*(Please refer to **Appendix A: System Behavior Matrix** at the end of this document for the detailed mapping of scenarios to Database States and HTTP Responses.)*

### Idempotent requests
The API supports idempotency for safely retrying requests without accidentally performing the same operation twice. 

* **Mechanism**: Clients optionally provide `Idempotency-Key` header.
* **Behavior**:
  1. If a request arrives with a key that exists in the system.
  2. The gateway returns the **cached response** (including the original status) without re-initiating a bank call.

### Security & Compliance (PCI-DSS)
* **No CVV Storage**: The CVV is passed transiently to the Acquiring Bank and immediately discarded from memory. It is never persisted.
* **Data Masking**: Primary Account Numbers (PAN) are masked upon entry into the domain model. All internal logs and API read operations expose only the last 4 digits.

### Testing & Execution
#### Prerequisites
* **Docker & Compose**: Required to orchestrate the environment.
* **Service Orchestration**: The entire stack, including the **Gateway** and the **Bank Simulator**, is now fully containerized. You can launch both services with a single command:
    ```bash
    docker-compose up --build
    ```
  *Note: The Gateway will be accessible at `http://localhost:8091`, while the Simulator remains at `http://localhost:8080`.* 

#### Testing Strategy
* **Controller Tests**: Use `@MockBean` to strictly verify the HTTP contract, JSON serialization, and Exception-to-Status mapping without relying on the repository.
* **Domain Tests**: Verify business rules and validation logic in isolation.
* **Infrastructure Tests**: Validate the integration with the external Simulator, ensuring that 4xx/5xx/Timeout responses from the bank are correctly mapped to domain exceptions.

### Future Improvements & Technical Debt

#### Test Infrastructure (Testcontainers)
* **Current State**: The test suite currently depends on a pre-running `docker-compose` environment for the Bank Simulator.
* **Attempted Solution**: `Testcontainers` was initially explored to manage the simulator lifecycle programmatically for integration tests.
* **Roadblock**: Encountered environment-specific networking issues between the test container and the host environment that could not be resolved within the time constraints.
* **Future Plan**: Migrate to `Testcontainers` to ensure tests are self-contained and reproducible in any CI/CD environment without external setup.

#### Production Readiness
* **Persistence**: Replace in-memory storage with PostgreSQL (relational data) and Redis (idempotency/locking).
* **Circuit Breaking**: Implement Resilience4j to handle "Service Unavailable" scenarios (e.g., cards ending in 0) to prevent resource exhaustion.
* **Observability**: Integrate Micrometer and OpenTelemetry for metrics and distributed tracing.
* **Data Lifecycle**: Implement **TTL (Time-To-Live)** policies for Idempotency Keys (e.g., 24-48 hours). Currently, the in-memory map grows indefinitely; in production, keys should be automatically evicted via Redis expiration to prevent storage bloat.
* **Authentication**: Use **OAuth2 with Client Credentials grant type** (standard for Server-to-Server communication). Merchants would be required to provide a valid JWT token in the `Authorization` header.
* **Authorization**: Implement **Role-Based Access Control (RBAC)**. For example, a merchant should only be able to retrieve (`GET`) payments that belong to their own `merchant_id`.
 
---

### Appendix A: System Behavior Matrix

The following table maps real-world scenarios to System/Database states and HTTP responses.

| Scenario | HTTP Response | Database Status         | System Behavior & Rationale                                                                                                                                                                                                                                                 |
| :--- | :--- |:------------------------|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Request Validation Failure**<br>(e.g., Expiry in past, Invalid Currency) | `400 Bad Request` | **N/A (Not Persisted)** | **Fail Fast**: Invalid requests are rejected at the API entry point before reaching the domain. This prevents data pollution and protects storage resources.                                                                                                                |
| **Processing Started** | N/A (Internal) | `PENDING`               | **Audit Trail**: Before calling the bank, the request is persisted as `PENDING`. This ensures a record exists for reconciliation even if the application crashes immediately after.                                                                                         |
| **Bank Approved** | `201 Created` | `AUTHORIZED`            | **Final State**: The bank confirmed the transaction. Masked card details and Authorization Code are returned.                                                                                                                                                               |
| **Bank Declined**<br>(e.g., Insufficient Funds) | `201 Created` | `DECLINED`              | **Final State**: A valid business outcome. The transaction was processed successfully but rejected by the issuer.                                                                                                                                                           |
| **Upstream Malformed Response**<br>(Bank returns 201 but invalid/empty body) | `201 Created` | `REJECTED`              | **Fail Safe**: Although the bank returned a success code, the response body was unreadable or missing critical fields. To mitigate risk, the system conservatively treats this protocol violation as a rejection (Status: `REJECTED`) rather than leaving it indeterminate. |
| **Upstream Timeout**<br>(Network Partition/Bank Slow) | `504 Gateway Timeout` | `UNKNOWN`               | **Safety Lock**: The system did not receive a definitive answer. The state updated to `UNKNOWN` to prevent double-charging. The client is instructed to retry safely using the Idempotency Key.                                                                             |
| **Upstream Unavailable**<br>(Bank returns 5xx/429) | `502 Bad Gateway` | `UNKNOWN`               | **Transient Failure**: Similar to timeout, the final status is unknown. The state updated to `UNKNOWN` for future reconciliation or retry.                                                                                                                                  |

