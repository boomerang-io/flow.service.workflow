package io.boomerang.v4.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum RelationshipRefType {

  WORKFLOW("Workflow"), WORKFLOWRUN("WorkflowRun"), TASKTEMPLATE("TaskTemplate"), USER("User"), TEAM("Team"),
  GLOBAL("Global"), SYSTEM("System");

  private String ref;

  RelationshipRefType(String ref) {
    this.ref = ref;
  }

  @JsonValue
  public String getRef() {
    return ref;
  }
}