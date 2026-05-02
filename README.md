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
- [x] **#13 Frontend: Real-time Engine**: WebSocket integration and message event handling.
- [x] **#14 Frontend: Chat Dashboard**: Sidebar, message history, and reactive UI components.
- [x] **#15 UI/UX Refinement**: Vanilla CSS styling, glassmorphism, and responsive design.

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

### Infrastructure & Frontend (Docker Compose) - **RECOMMENDED**
This is the fastest way to test the entire system. From the project root, run:
```bash
docker-compose up --build
```
Once started:
- **Frontend UI**: [http://localhost:3000](http://localhost:3000)
- **API Gateway**: [http://localhost:8080](http://localhost:8080)
- **RabbitMQ Dashboard**: [http://localhost:15672](http://localhost:15672) (guest/guest)
- **MongoDB**: [localhost:27018](localhost:27018) (Internal: 27017)

### Local Development (Manual)
1. **Prerequisites**: Start MongoDB and RabbitMQ:
   ```bash
   docker-compose up mongodb rabbitmq
   ```
2. **Install shared modules**:
   ```bash
   mvn install -pl proto,common-security -DskipTests
   ```
3. **Run Services**: Start services via IDE or Maven (e.g., `mvn spring-boot:run -pl user-service`).
4. **Frontend Dev Server**:
   ```bash
   cd frontend && npm install && npm run dev
   ```
   The UI will be at [http://localhost:5173](http://localhost:5173) (proxies to gateway on `8080`).

### Kubernetes Deployment (Production-like)
All manifests are located in the `k8s/` directory.
1. **Apply Infrastructure**: `kubectl apply -f k8s/infrastructure/`
2. **Apply Services**: `kubectl apply -f k8s/services/`

---

## 🧪 Testing

You can run the full test suite across all microservices using the Maven Wrapper.

### Running all tests
```bash
./mvnw test -DskipTests=false
```

### Running specific modules
To save time during development, you can run tests for a specific service:
```bash
./mvnw test -pl auth-service -DskipTests=false
```

### Requirements
- **Docker Desktop**: The `user-service` uses Testcontainers and requires a running Docker engine for its integration tests.
- **Java 26 Compatibility**: The project is pre-configured to handle Java 26 (Byte Buddy experimental flags are included in the parent POM).

---

## 📈 Scalability Note
The current architecture supports horizontal scaling for all services. For the **Message Service**, scaling WebSockets across multiple replicas requires a RabbitMQ fanout strategy (as documented in `docs/RESILIENCE.md`) to ensure messages reach the correct node holding the recipient's active connection.

## Author
- [Bamsemats](https://github.com/bamsemats)