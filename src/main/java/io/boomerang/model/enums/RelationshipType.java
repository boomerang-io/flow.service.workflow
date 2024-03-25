package io.boomerang.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum RelationshipType {

  WORKFLOW("Workflow"), WORKFLOWRUN("WorkflowRun"), TASKTEMPLATE("TaskTemplate"), USER("User"), TEAM("Team"),
  GLOBAL("Global"), SYSTEM("System"), APPROVERGROUP("ApproverGroup"), TEMPLATE("Template"), TOKEN("Token"), INTEGRATION("Integration"), SCHEDULE("Schedule"), TASK("Task"), GLOBALTASK("GlobalTask");

  private String ref;

  RelationshipType(String ref) {
    this.ref = ref;
  }

  @JsonValue
  public String getRef() {
    return ref;
  }
}