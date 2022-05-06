package io.boomerang.model.eventing;

import io.boomerang.mongo.model.TaskStatus;

public class EventWFE {

  private String workflowId;

  private String workflowActivityId;

  private String topic;

  private TaskStatus status;
}
