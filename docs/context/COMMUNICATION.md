# Service communication

## Current implementation

Only `user-service` exists. Its public interface is HTTP REST. No outbound inter-service calls, message broker, event schema, or cross-service contract is present in the repository. Endpoint inventory is in user-service [API](../../services/user-service/docs/context/API.md).

## Future synchronous communication (planned)

Use HTTP/REST when a caller needs an immediate response and a clear service contract. The product direction mentions REST/HTTP; no gateway or inter-service client is configured yet.

## Future asynchronous communication (planned)

A broker and domain/integration events may be introduced when a real asynchronous cross-service use case requires them. Conceptual events include `UserRegistered`, `UserInvitedToWorkspace`, `DocumentCreated`, and `DocumentDeleted`; none exist today. Define ownership, schema, delivery behavior, and failure handling before selecting a broker. Consider an outbox only if required by delivery guarantees.

## Realtime is a separate concern

Collaborative document updates have latency and ordering requirements that differ from business integration events. Planned direction is WebSocket + Yjs, with Redis if required for shared realtime state. Do not route every editor operation through Kafka/RabbitMQ simply because a broker may later exist. See [[ROADMAP]] and [[ARCHITECTURE]].
