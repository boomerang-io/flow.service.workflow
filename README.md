# Flow Workflow Service

Flow service provides APIs for:

* User & Team Management
* Creating / Updating workflows
* Executing workflows as Directed acyclic graph (DAG)

## Prerequisites**

1. Java 11
2. Springboot 2.3 
3. Maven

## NATS Integration

If eventing is enabled on this service it will attempt to connect to, and subscribe to events. If you want to test this locally run

```
docker run --entrypoint /nats-streaming-server -p 4222:4222 -p 8222:8222 nats-streaming
```

See https://hub.docker.com/_/nats-streaming for more information
