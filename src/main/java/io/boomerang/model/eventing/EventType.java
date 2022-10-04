package io.boomerang.model.eventing;

public enum EventType {

  // @formatter:off
  TRIGGER("io.boomerang.event.workflow.trigger"),
  WFE("io.boomerang.event.workflow.wfe"),
  CANCEL("io.boomerang.event.workflow.cancel"),
  WORKFLOW_STATUS_UPDATE("io.boomerang.event.workflow.status"),
  TASK_STATUS_UPDATE("io.boomerang.event.task.status");
  // @formatter:off

  private final String cloudEventType;

  private EventType(String cloudEventType) {
    this.cloudEventType = cloudEventType;
  }

  public String getCloudEventType() {
    return cloudEventType;
  }

  public static EventType valueOfCloudEventType(String extendedType) {
    for (EventType eventType : values()) {
      if (extendedType.startsWith(eventType.cloudEventType)) {
        return eventType;
      }
    }
    return null;
}
}
