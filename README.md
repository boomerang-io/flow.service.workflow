# Flow Workflow Service

The Boomerang Flow Workflow service provides the CRUD backing the front-end web tier, as well as the APIs for direct consumption, and the executor for the workflow tasks.

## v2 APIs

Flow service provides APIs for:

- User & Team Management
- Creating / Updating workflows
- Executing workflows as Directed acyclic graph (DAG)

## Prerequisites

1. Java 11
2. Spring Boot 2.3
3. Maven

## Testing Locally

## Dependencies

### CloudEvents

This service listen and consumes CloudEvents.

### Quartz

The Java Quartz library is used for running scheduled jobs via mongoDB and underpins the Schedule trigger.

The following links will help provide guidance in development

- http://www.quartz-scheduler.org/documentation/quartz-2.2.2/tutorials/tutorial-lesson-04.html
- http://www.quartz-scheduler.org/documentation/2.4.0-SNAPSHOT/cookbook/UpdateTrigger.html
- https://github.com/StackAbuse/spring-boot-quartz/blob/master/src/main/java/com/stackabuse/service/SchedulerJobService.java
- https://stackabuse.com/guide-to-quartz-with-spring-boot-job-scheduling-and-automation/

## Security

Security is enabled / disabled through the `flow.authorization.enabled` flag in the application.properties

The following classes are conditionally loaded based on this flag

| Class | Condition |
| AuthenticationFilter | true |
| InterceptorConfig (and by association SecurityInterceptor) | true |
| SecurityConfiguration | true |
| SecurityDisabledConfiguration | false |


