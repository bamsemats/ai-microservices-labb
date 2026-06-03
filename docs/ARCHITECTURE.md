# Architecture Review & Design Decisions

This document outlines the key architectural decisions made during the development of the AdaptaChat system, their rationales, and the associated trade-offs.

## 1. Shared Security Library (`common-security`)

### Decision
The system utilizes a shared Maven module, `common-security`, to encapsulate security-related logic, including JWT signature verification, token validation, and global exception handling.

### Rationale
- **Simplicity & Speed**: Implementing security via a shared library is significantly faster than managing a Service Mesh for a project of this scope.
- **Consistency**: Centralizing the security logic ensures that every microservice applies the exact same validation rules, reducing the risk of "security drift."
- **Standardization**: All monorepo components follow a strict package naming convention (`com.example.labb_microservices.*`) facilitated by this module.

### Trade-offs: The "Distributed Monolith" Risk
By sharing binary logic across microservices, we introduce **binary coupling**.
- **Redeployment Coupling**: A breaking change in `common-security` requires all dependent services to be rebuilt and redeployed.
- **Language Lock-in**: All services must be compatible with the library's language (Kotlin/JVM).

## 2. Standardized Package Naming

### Decision
All services and common modules have been standardized to the root package `com.example.labb_microservices`.    

### Rationale
- **Architectural Alignment**: Provides a clean, professional structure that simplifies component scanning and IDE navigation.
- **Dependency Clarity**: Makes it immediately obvious which classes are internal to the service and which are imported from the monorepo's shared modules.

## 3. Prism Aura Design Architecture

### Decision
The system transitioned to a 3-layer Vanilla CSS architecture (`@layer tokens, base, components`) known as **Prism Aura**.

### Rationale
- **Performance**: Eliminates the overhead of utility-first frameworks (like Tailwind) while utilizing native browser features like `@layer` for specificity management.
- **Dynamic Adaptability**: Exposes semantic design tokens (`--sentiment-glow-intensity`, etc.) directly to the `:root` element, allowing for GPU-accelerated, AI-driven visual shifts without JS-heavy DOM manipulation.
- **Theming Scalability**: Provides a clear separation between design tokens (colors, spacing) and component-level styles, facilitating the implementation of multiple base aesthetics (Cyber, Nature, Minimal, Warm).

## 4. Asynchronous Semantic AI Pipeline

### Decision
The system transitioned from synchronous regex-based entity detection to an asynchronous, LLM-powered semantic extraction pipeline.

### Rationale
- **Intelligence**: Utilizing LLMs allows the system to identify entities (Streamers, Games, Topics) from natural language context rather than just raw URLs.
- **Resilience**: RabbitMQ decouples the heavy AI analysis from the critical message delivery path, ensuring the UI remains responsive even during high-latency AI processing.
- **Confidence Scoring**: High-confidence entities (threshold 0.6) ensure that third-party injections are relevant and non-intrusive.

## 5. Edge Security & Rate Limiting

### Decision
Implemented a dual-layer security posture combining Gateway-level hardening and internal Zero-Trust validation. 

### Rationale
- **Gateway Defense**: Implements `SecureHeaders` (HSTS, CSP) and a Redis-backed `RequestRateLimiter` to protect against brute-force attacks at the point of entry.
- **Internal Verification**: Every service re-verifies JWT signatures, ensuring that even internal network traffic is authenticated.
- **Input Validation**: Strict `jakarta.validation` constraints prevent malformed data from reaching the core business logic.

## 6. Standardized Error Handling & Global Exception Management

### Decision
The system implements a centralized `GlobalExceptionHandler` within the `common-security` module to provide a consistent JSON error structure across all microservices.

### Rationale
- **UX Consistency**: The frontend receives a predictable JSON payload (timestamp, status, error, message, path) regardless of which service fails.
- **Security Propagation**: Explicitly handles `AccessDeniedException` to ensure correct 401/403 status codes are returned, preventing internal details from leaking via generic errors.

## 7. Historical Sentiment Recovery (Advanced Search)

### Decision
The system persists AI-detected sentiment metadata (Theme/Intensity) directly into the message documents in MongoDB.

### Rationale
- **Emotional Retrieval**: Allows users to perform complex historical searches (e.g., "find all high-intensity vibrant messages") across all frequencies.
- **Data Locality**: Storing sentiment with the message ensures that decryption and emotional analysis are synchronized during historical recovery.

## 8. Robust DM Heuristics

### Decision
Implemented a sorted combined ID pattern (`minID-maxID`) for Direct Message channel identification.

### Rationale
- **Deterministic Synchronization**: Ensures both participants in a private conversation always resolve to the same logical frequency, regardless of who initiated the sync.
- **Scalability**: Allows the `message-service` to treat DMs as just another partitioned channel, simplifying the WebSocket routing logic.

## 9. Consolidated Configuration Strategy

### Decision
Redundant infrastructure and security settings are consolidated into a shared `observability-defaults.properties` file in `common-observability`.

### Rationale
- **Maintenance**: Reducing duplication makes the system less prone to configuration drift and easier to audit. 
- **Clarity**: Service-specific properties now contain only relevant overrides, improving readability.

## 10. Reactive Exception Handling

### Decision
The API Gateway handles errors using a native ErrorWebExceptionHandler rather than relying on @ControllerAdvice.

### Rationale
- **Reactive Native**: In Spring Cloud Gateway, failures often occur in the filter chain or during routing before reaching a traditional controller. A reactive web exception handler correctly intercepts these pipeline failures.
- **Consistent Payloads**: Ensures that route timeouts, 5xx downstream errors, and JWT validation failures all return the exact same JSON error structure as the rest of the services.

## 11. Enforced Chronological Data Retrieval

### Decision
All historical message queries in the MessageService explicitly enforce a database-level sort (	imestamp ASC), even when performing concurrent merge operations.

### Rationale
- **Predictable History**: Eliminates race conditions where concurrent .mergeWith() streams (like fetching Direct Messages from both perspectives) return interleaved or jumbled results.
- **Client Agnosticism**: Guarantees that the REST API contract is honored strictly by the backend, removing the burden of chronological sorting from the frontend or mobile clients.
