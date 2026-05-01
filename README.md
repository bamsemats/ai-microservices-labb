# Distributed Microservices Chat System (Labb 2)

A high-performance, resilient, and secure distributed chat system built with Spring Boot, Kotlin, and Kubernetes. This project demonstrates advanced microservices patterns, including hybrid synchronous/asynchronous communication, zero-trust security, and real-time data streaming.

## 🏗 Architecture Overview

The system follows a **Database-per-Service** pattern and utilizes a **Monorepo** structure for streamlined development and orchestration.

- **API Gateway**: Entry point for all clients. Handles routing and coarse-grained JWT validation.
- **Auth Service**: Manages user sessions and issues JWTs upon successful login.
- **User Service**: Handles user registration and profile management. Provides gRPC endpoints for metadata lookups.
- **Message Service**: Manages chat messages, real-time WebSocket connections, and asynchronous persistence via RabbitMQ.
- **Common Security**: A shared module providing reusable zero-trust JWT signature verification across all services.
- **Proto**: Shared Protobuf definitions for type-safe gRPC communication.

### 🛡 Security: Zero-Trust Model
Even though the Gateway validates incoming tokens, every microservice in the cluster re-verifies the cryptographic signature of the JWT using the `common-security` filter. This ensures that even if the gateway is bypassed or an internal service is compromised, the system remains secure.

### 📡 Communication Patterns
- **Synchronous (gRPC)**: Used for critical, low-latency lookups (e.g., Auth Service checking credentials in User Service).
- **Asynchronous (RabbitMQ)**: Used for message delivery and persistence. When a message is sent, it's published to an exchange, allowing the Message Service to return a quick "Accepted" response while consumers handle storage and delivery in the background.
- **Real-time (WebSockets)**: Built with Spring WebFlux Sinks to stream messages to active clients with minimal overhead.

---

## 🚀 Progress & Roadmap

- [x] **#01 Scaffold Monorepo**: Base structure, Maven multi-module setup, and Dockerfiles.
- [x] **#02 User Service Registration**: Implemented registration flow with Reactive MongoDB.
- [x] **#03 Auth Service & JWT Issuance**: Implemented login and JWT signing.
- [x] **#04 API Gateway & JWT Routing**: Configured routing rules and initial security checks.
- [x] **#05 Zero-Trust Signature Verification**: Extracted security logic to a common module for multi-layer validation.
- [x] **#06 Sync User Data Retrieval (gRPC)**: Implemented full gRPC contracts for inter-service metadata lookups.
- [x] **#07 Async Message Pipeline (RabbitMQ)**: Integrated RabbitMQ for reliable, non-blocking message processing.
- [x] **#08 Real-time Delivery (WebSockets)**: Enabled live message streaming to authenticated clients.
- [x] **#09 Kubernetes Deployment**: Created comprehensive K8s manifests for the entire stack.
- [x] **#10 Resilience Review**: Verified architecture against scalability and isolation requirements.
- [x] **#11 Infrastructure & CI**: Established Docker Compose orchestration and a GitHub Actions CI pipeline.
- [x] **#12 Frontend: Auth Foundation**: Scaffolded React SPA with Zustand state management and Vanilla CSS authentication UI.
- [ ] **#13 Frontend: Real-time Engine**: WebSocket integration and message event handling.

---

## 🛠 Technology Stack

- **Backend**: Spring Boot 3.4.1, Spring WebFlux (Reactive)
- **Frontend**: React 18, TypeScript, Vite, Zustand, Vanilla CSS
- **CI/CD**: GitHub Actions
- **Orchestration**: Docker Compose, Kubernetes (Minikube)
- **Messaging**: RabbitMQ
- **RPC**: gRPC / Protobuf
- **Database**: MongoDB (Reactive)

---

## 🏃 How to Run

### Prerequisites
- Java 21+
- Docker & Docker Compose (or Minikube for K8s)
- Maven

### Local Development (Maven)
1. **Install shared modules**:
   ```bash
   mvn install -pl proto,common-security -DskipTests
   ```
2. **Run individual services**:
   You can start the services using your IDE or Maven. Ensure MongoDB and RabbitMQ are running locally.
   ```bash
   mvn spring-boot:run -pl user-service
   ```

### Infrastructure (Docker Compose)
A `docker-compose.yaml` (not yet explicitly created but implied by individual Dockerfiles) would typically spin up:
- MongoDB (Port 27017)
- RabbitMQ (Port 5672, Management: 15672)

### Kubernetes Deployment
All manifests are located in the `k8s/` directory.
1. **Apply Infrastructure**:
   ```bash
   kubectl apply -f k8s/infrastructure/
   ```
2. **Apply Services**:
   ```bash
   kubectl apply -f k8s/services/
   ```

---

## 📈 Scalability Note
The current architecture supports horizontal scaling for all services. For the **Message Service**, scaling WebSockets across multiple replicas requires a RabbitMQ fanout strategy (as documented in `docs/RESILIENCE.md`) to ensure messages reach the correct node holding the recipient's active connection.
