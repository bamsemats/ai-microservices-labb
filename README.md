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
- **Common Observability**: A dedicated module for centralized monitoring (Actuator), tracing (Micrometer), and production health indicators.
- **Common Test**: Singleton infrastructure for stable, noise-free integration tests across the monorepo.
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
- [x] **#45 Quality & Security Stabilization**: Exception log masking, atomic AI updates, and session-based WebSockets.
- [x] **#46 Infrastructure & Test Hardening**: Kubernetes securityContext, non-root execution, and Awaitility integration.
- [x] **#47 Persona Sync: AI-driven Profile Updates**: Automated bio updates from facts.
- [x] **#48 Infrastructure Noise Reduction**: Singleton containers and fast-fail test settings.
- [x] **#49 Production Hardening**: SCAN for Presence, Masking for Exceptions, and Atomic AI Updates.
- [x] **#50 Security & Resilience Sweep**: PII redaction, RabbitMQ DLQs, and reactive offloading.
- [x] **#51 System Stabilization & Hardening**: WebSocket backoff, auth timeouts, and shared AI queues.
- [x] **#52 Deepen Session Module**: Consolidate WebSocket state into `ChatSession` and `SessionRegistry`.
- [x] **#53 AI-Driven Design Tokens**: Deepen Analytical Seam by moving design logic to `ai-service`.
- [ ] **#54 Global Cross-Channel Search**: Searchable encrypted history (Phase 4).
- [ ] **#55 Direct File Transfers**: Media sharing service foundation.
- [ ] **#56 Voice/Video Integration**: WebRTC-based real-time media.
- [x] **#57 Observability Refactoring**: Consolidated monitoring, tracing (Jaeger/OTLP), and metrics (Prometheus) logic.
- [x] **#58 Full-Stack Security & Reliability Sweep**: Non-root Docker execution, mTLS hardening, and multi-token search.
- [x] **#59 Fix: Docker Build & Startup Stabilization**: Resolved dependency and permission issues.
- [x] **#60 UX: Theme Toggle & Profile Hub**: Added Dark/Light mode and dedicated profile editing with social links.
- [x] **#61 UX: Typing Indicators & Read Receipts**: Implemented real-time typing detection and persistent read status tracking.
- [x] **#62 UX: Accessibility Polish**: ARIA labels, roles, and keyboard navigation shortcuts.
- [x] **#63 UX: Mobile Optimization**: Responsive design, mobile sidebar, and adaptive grids.
- [x] **#64 Branding & Identity Update**: Refresh logotype and global design language.

---

## 🛠 Technology Stack

- **Backend**: Spring Boot 3.4.3, Spring WebFlux (Reactive), Kotlin
- **Frontend**: React 19, TypeScript, Vite, Zustand, Motion (v12), Vanilla CSS
- **Messaging**: RabbitMQ, gRPC, WebSockets
- **Persistence**: MongoDB, Redis
- **Orchestration**: Docker Compose, Kubernetes
- **Security**: Zero-Trust JWT, AES-256 PII Encryption, mTLS

---

## 🏗 Design Decisions & Trade-offs

### High-Availability WebSockets (Session-based)
The WebSocket architecture has been evolved from user-keyed sinks to **session-keyed sinks** managed by a dedicated `SessionRegistry`. 
- **Multi-Device Support**: A single user can maintain multiple active connections (tabs/devices) without message collisions.
- **Decoupled Delivery**: A new `MessageDeliveryService` abstracts the routing logic, allowing the `MessageWebSocketHandler` to focus solely on the reactive connection pipeline.
- **Resource Integrity**: `ChatSession` objects encapsulate all session-specific state (tokens, sinks, channels), ensuring robust cleanup and preventing memory leaks when connections drop.

### Full-Stack Security & Reliability Sweep
A comprehensive sweep was performed to harden the system's production readiness:

---

## ⚠️ Known Limitations
- **Binary Coupling**: Services depend on `common-security`, `common-observability`, and `proto`.
- **Coordinated Rollouts**: Shared library updates require full-stack redeployment.

---

## 🏁 How to Run

### Prerequisites
The system requires several environment variables for security and inter-service communication. For local development, create a `.env` file in the root directory:

```env
JWT_SECRET=your-256-bit-secret
ENCRYPTION_SECRET=your-32-char-encryption-key
OPENROUTER_API_KEY=your-api-key

# gRPC Keystore Passwords (local development only; override in non-local environments)
GRPC_SERVER_SECURITY_KEY_STORE_PASSWORD=password
GRPC_SERVER_SECURITY_KEY_PASSWORD=password
GRPC_SERVER_SECURITY_TRUST_STORE_PASSWORD=password
```

### Infrastructure & Frontend (Docker Compose) - **RECOMMENDED**
```bash
docker-compose up --build
```
- **Frontend UI**: [http://localhost:3000](http://localhost:3000)
- **API Gateway**: [http://localhost:8080](http://localhost:8080)

## Author
- [Bamsemats](https://github.com/bamsemats)
