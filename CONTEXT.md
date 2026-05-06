# Domain Context: AdaptaChat

## Core Entities

### User
The primary identity in the system. Managed by `user-service`. Has a profile, credentials, and account status.

### Message
A unit of communication between users or a user and a bot. Managed by `message-service`. Delivered via WebSockets.

### Channel (or Room)
A logical partition for messages. Ensures isolation between different chat contexts.

### Sentiment
An analytical property of a message (e.g., zen, emergency). Extracted by `ai-service`.

### Memory Fragment
A piece of long-term user preference or fact extracted from conversations. Managed by `ai-service`.

### Content Widget
External rich media (e.g., Twitch stream) injected into the chat based on entities detected in messages. Managed by `content-aggregator-service`.

## System Seams

### Identity Seam (gRPC)
The interface between `auth-service` and `user-service` for validating credentials and retrieving user metadata.

### Real-time Seam (WebSocket)
The streaming interface between clients and the `message-service`.

### Analytical Seam (RabbitMQ)
The asynchronous pipeline where messages are sent for AI processing and content aggregation.
