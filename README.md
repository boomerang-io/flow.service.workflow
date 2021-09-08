# Flow Workflow Service



Flow service provides APIs for:

* User & Team Management
* Creating / Updating workflows
* Executing workflows as Directed acyclic graph (DAG)

## Prerequisites**

1. Java 11
2. Spring Boot 2.3
3. Maven

## Testing Locally

### NATS Jetstream Integration

If eventing is enabled on this service it will attempt to connect to, and subscribe to events. If you want to test this locally run:

```bash
docker run --detach --network host -p 4222:4222 --name nats-jetstream nats -js
```

Visit <https://docs.nats.io/jetstream/getting_started/using_docker> for more information.
