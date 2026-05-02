# Architecture Review & Design Decisions

This document outlines the key architectural decisions made during the development of the Distributed Microservices Chat System, their rationales, and the associated trade-offs.

## 1. Shared Security Library (`common-security`)

### Decision
The system utilizes a shared Maven module, `common-security`, to encapsulate security-related logic, including JWT signature verification, token validation, and encryption utilities.

### Rationale
- **Simplicity & Speed**: Implementing security via a shared library is significantly faster and less complex than setting up and managing a Service Mesh (e.g., Istio) or a Sidecar pattern (e.g., Envoy) for a project of this scope.
- **Consistency**: Centralizing the security logic ensures that every microservice applies the exact same validation rules and cryptographic checks, reducing the risk of "security drift."
- **Low Overhead**: Avoids the network latency and resource consumption associated with proxying every request through a sidecar.

### Trade-offs: The "Distributed Monolith" Risk
By sharing binary logic across microservices, we introduce **binary coupling**. This pattern is often referred to as a "distributed monolith" because:
- **Redeployment Coupling**: A breaking change or a critical bug fix in `common-security` requires all dependent services (`auth-service`, `user-service`, `message-service`) to be rebuilt and redeployed.
- **Language Lock-in**: All services must be compatible with the library's language (Kotlin/JVM) and its dependencies.

## 2. Shared Library Update Protocol

To manage the coupling introduced by `common-security` and the `proto` module, the following update protocol is defined:

1.  **Local Development**:
    - Modify the shared library.
    - Run `mvn install -pl common-security,proto -DskipTests` to update the local Maven repository.
    - Restart dependent services to pick up changes.
2.  **CI/CD & Deployment**:
    - The CI pipeline (`.github/workflows/ci.yml`) is configured to build and install shared modules before building the microservices.
    - **Breaking Changes**: If a change in the shared library breaks downstream services, the PR must include the necessary fixes in all affected modules simultaneously.
    - **Rolling Updates**: In a production environment, versioned releases of the library should be used (e.g., `1.2.0`) instead of `SNAPSHOT` to allow for phased rollouts, though `SNAPSHOT` is currently used for simplicity in this lab.

## 3. Known Limitations

- **Binary Coupling**: As noted above, the shared library pattern limits the independence of service deployments.
- **Scaling WebSockets**: Scaling the `message-service` requires external synchronization (implemented via RabbitMQ Fanout) to ensure messages are delivered to the correct instance holding the client's WebSocket session.
- **Database-per-Service Complexity**: While providing isolation, this pattern requires gRPC or eventual consistency for cross-service data needs, increasing the complexity of "join" operations.

## 4. Alternative Considered: Sidecar Pattern
We considered using a sidecar proxy (like Envoy) to handle JWT validation. While this would have decoupled the security logic from the application code and allowed for polyglot services, it was deemed an unnecessary complexity for this project's current requirements and timeline. The zero-trust requirement is satisfied by the internal verification within the JVM process.
