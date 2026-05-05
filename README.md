# AdaptaChat: Distributed Microservices Chat System (Labb 2)

A high-performance, resilient, and secure distributed chat system built with Spring Boot, Kotlin, and Kubernetes. This project demonstrates advanced microservices patterns, including hybrid synchronous/asynchronous communication, zero-trust security, and real-time data streaming.

## 🏗 Architecture Overview

The system follows a **Database-per-Service** pattern and utilizes a **Monorepo** structure for streamlined development and orchestration.

- **API Gateway**: Entry point for all clients. Handles routing and coarse-grained JWT validation.
- **Auth Service**: Manages user sessions and issues JWTs upon successful login.
- **User Service**: Handles user registration and profile management. Provides gRPC endpoints for metadata lookups.
- **Message Service**: Manages chat messages, real-time WebSocket connections, and asynchronous persistence via RabbitMQ.
- **AI Service**: Performs real-time sentiment analysis and provides intelligent chat interactions.
- **Content Aggregator**: Extracts entities from conversations and injects rich media widgets (e.g., Twitch).
- **Common Security**: A shared module providing reusable zero-trust JWT signature verification across all services.
- **Proto**: Shared Protobuf definitions for type-safe gRPC communication.

---

## 🚀 Progress & Roadmap

- [x] **#01 Scaffold Monorepo**: Base structure, Maven multi-module setup.
- [x] **#02 User Service Registration**: Implemented registration flow with Reactive MongoDB.
- [x] **#03 Auth Service & JWT Issuance**: Implemented login and JWT signing.
- [x] **#04 API Gateway & JWT Routing**: Configured routing rules and initial security checks.
- [x] **#05 Zero-Trust Signature Verification**: Extracted security logic to common module.
- [x] **#06 Sync User Data Retrieval (gRPC)**: Implemented gRPC for inter-service metadata lookups.
- [x] **#07 Async Message Pipeline (RabbitMQ)**: Integrated RabbitMQ for reliable processing.
- [x] **#08 Real-time Delivery (WebSockets)**: Enabled live message streaming.
- [x] **#09 Kubernetes Deployment**: Created comprehensive K8s manifests.
- [x] **#10 Resilience Review**: Verified architecture scalability.
- [x] **#11 Infrastructure & CI**: Established Docker Compose and GitHub Actions.
- [x] **#12 Frontend: Auth Foundation**: Scaffolded React SPA with Zustand.
- [x] **#13 Frontend: Real-time Engine**: WebSocket integration and event handling.
- [x] **#16 WebSocket Scalability**: Fixed multi-replica delivery bug.
- [x] **#17 Zero-Trust WebSocket Security**: Periodic JWT and account status (lockout) re-validation.
- [x] **#18 Reactive gRPC Resilience**: Migrated to non-blocking stubs.
- [x] **#19 Architecture Documentation**: Finalized design trade-off docs.
- [x] **#20 Redis Refresh Tokens**: Secure token rotation and persistence.
- [x] **#21 PII Encryption**: Transparent field-level encryption.
- [x] **#22 Infrastructure: Redis**: Deployment for session/token management.
- [x] **#23 Refresh Token Lifecycle**: Implemented rotate/revoke flows.
- [x] **#24 PII Blind Indexing**: Searchable encrypted data implementation.
- [x] **#25 Async AI Loop**: Message processing pipeline for AI features.
- [x] **#26 mTLS Enforcement**: inter-service identity with certificates.
- [x] **#27 UI Adaptation**: Real-time aesthetic shifts based on AI sentiment.
- [x] **#28 Content Aggregator**: Contextual widget injection (Twitch, etc.).
- [x] **#29 UI Foundation**: "Lumina Fluid" style system with glassmorphism.
- [x] **#30 Chat Hub**: Sidebar and channel navigation architecture.
- [x] **#31 Messaging Interface**: Physics-based animations and AI prompt assists.
- [x] **#32 Discovery Hub**: Exploratory interface for creators and topics.
- [x] **#33 AI Insights Dashboard**: Performance visualization and profile customization.
- [x] **#37 Persistent Channel Partitioning**: Strict channel-based message isolation.
- [x] **#38 Real-time Presence Backend**: Redis-backed status tracking for all users.
- [x] **#39 Presence UI Integration**: Live status indicators in the global sidebar.
- [x] **#40 Real-time Trending Topics**: Activity tracking for discoverable channels.
- [x] **#41 Personal Interaction Stats**: User analytics dashboard implementation.
- [x] **#42 Real AI Loop Closure**: Intelligent response generation with simulated LLM latency.
- [x] **#43 Adapta-Memory: Fact Extraction**: Persistent preference storage in MongoDB.
- [x] **#44 Personalization Engine**: Hybrid context (Wiki + Session) responses.
- [x] **#45 Quality & Security Stabilization**: Masking, DTOs, Idempotency, and Accessibility.
- [x] **#46 Infrastructure & Test Hardening**: Bounded timeouts, BOM-less certs, and container static discovery.
- [x] **#47 Persona Sync: AI-driven Profile Updates**: Automated bio updates from facts.

---

## 🛠 Technology Stack

- **Backend**: Spring Boot 3.4.1, Spring WebFlux (Reactive), Kotlin
- **Frontend**: React 19, TypeScript, Vite, Zustand, Motion (v12), Vanilla CSS
- **Messaging**: RabbitMQ, gRPC, WebSockets
- **Persistence**: MongoDB, Redis
- **Orchestration**: Docker Compose, Kubernetes
- **Security**: Zero-Trust JWT, AES-256 PII Encryption, mTLS

---

## 🏛 Design Decisions & Trade-offs

### Adapta-Memory & Personalization
The system now includes an "Adapta-Memory" worker that asynchronously extracts user interests and preferences from chat streams. This uses a deduplicated MongoDB store to build a long-term "Knowledge Base" for each user.

Key enhancements in Phase 3:
- **Hybrid Context Injection**: The AI response generator retrieves these facts and injects them into the prompt, enabling hyper-personalized responses that reference past user facts.
- **Persona Sync**: High-confidence facts (confidence > 0.9) trigger a `PersonaUpdateEvent` via RabbitMQ, which the `user-service` consumes to automatically update the user's public bio.

### Shared Security Library vs. Sidecar
We use a shared module (`common-security`) for zero-trust validation. While this introduces a "distributed monolith" risk, it provides extreme simplicity and performance compared to a Service Mesh.

### Synchronous vs. Asynchronous Communication
The system uses gRPC for critical, low-latency lookups and RabbitMQ for everything else, ensuring a fast user experience while maintaining data integrity.

---

## ⚠️ Known Limitations
- **Binary Coupling**: Services depend on `common-security` and `proto`.
- **Coordinated Rollouts**: Shared library updates require full-stack redeployment.

---

## 🏃 How to Run

### Infrastructure & Frontend (Docker Compose) - **RECOMMENDED**
```bash
docker-compose up --build
```
- **Frontend UI**: [http://localhost:3000](http://localhost:3000)
- **API Gateway**: [http://localhost:8080](http://localhost:8080)

## Author
- [Bamsemats](https://github.com/bamsemats)
