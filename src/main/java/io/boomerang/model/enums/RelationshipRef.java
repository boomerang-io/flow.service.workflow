package io.boomerang.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum RelationshipRef {

  WORKFLOW("Workflow"), WORKFLOWRUN("WorkflowRun"), TASKTEMPLATE("TaskTemplate"), USER("User"), TEAM("Team"),
  GLOBAL("Global"), SYSTEM("System"), APPROVERGROUP("ApproverGroup"), TEMPLATE("Template"), TOKEN("Token"), INTEGRATION("Integration");

  private String ref;

  RelationshipRef(String ref) {
    this.ref = ref;
  }

  @JsonValue
  public String getRef() {
    return ref;
  }
}