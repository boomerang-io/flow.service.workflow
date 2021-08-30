package io.boomerang.model;

import java.util.List;
import io.boomerang.model.projectstormv5.WorkflowRevision;
import io.boomerang.mongo.model.WorkflowProperty;

public class TemplateWorkflowSummary {
  private String id;
  private String icon;
  private String description;
  private List<WorkflowProperty> parameters;
  private WorkflowRevision revision;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<WorkflowProperty> getParameters() {
    return parameters;
  }

  public void setParameters(List<WorkflowProperty> parameters) {
    this.parameters = parameters;
  }

  public WorkflowRevision getRevision() {
    return revision;
  }

  public void setRevision(WorkflowRevision revision) {
    this.revision = revision;
  }
}