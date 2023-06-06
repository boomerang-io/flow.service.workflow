# Flow Workflow Service


The Boomerang Flow Workflow service provides the CRUD backing the front-end web tier, as well as the v1 APIs for direct consumption, and the executor for the workflow tasks.

## v1 APIs
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

## Quartz

The Java Quartz library is used for running scheduled jobs via mongoDB and underpins the Schedule trigger.

The following links will help provide guidance in development
- http://www.quartz-scheduler.org/documentation/quartz-2.2.2/tutorials/tutorial-lesson-04.html
- http://www.quartz-scheduler.org/documentation/2.4.0-SNAPSHOT/cookbook/UpdateTrigger.html
- https://github.com/StackAbuse/spring-boot-quartz/blob/master/src/main/java/com/stackabuse/service/SchedulerJobService.java
- https://stackabuse.com/guide-to-quartz-with-spring-boot-job-scheduling-and-automation/ 
