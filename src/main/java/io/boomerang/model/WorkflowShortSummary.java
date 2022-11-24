package io.boomerang.model;

import io.boomerang.mongo.model.WorkflowScope;

public class WorkflowShortSummary {

  private String workflowId;

  private String workflowName;

  private String teamId;

  private String teamName;

  private boolean webhookEnabled;

  private WorkflowScope scope;

  private boolean customEventEnabled;

  private String customEvent;

  public String getWorkflowId() {
    return this.workflowId;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  public String getWorkflowName() {
    return this.workflowName;
  }

  public void setWorkflowName(String workflowName) {
    this.workflowName = workflowName;
  }

  public String getTeamId() {
    return this.teamId;
  }

  public void setTeamId(String teamId) {
    this.teamId = teamId;
  }

  public String getTeamName() {
    return this.teamName;
  }

  public void setTeamName(String teamName) {
    this.teamName = teamName;
  }

  public boolean isWebhookEnabled() {
    return this.webhookEnabled;
  }

  public boolean getWebhookEnabled() {
    return this.webhookEnabled;
  }

  public void setWebhookEnabled(boolean webhookEnabled) {
    this.webhookEnabled = webhookEnabled;
  }

  public WorkflowScope getScope() {
    return this.scope;
  }

  public void setScope(WorkflowScope scope) {
    this.scope = scope;
  }

  public boolean isCustomEventEnabled() {
    return this.customEventEnabled;
  }

  public boolean getCustomEventEnabled() {
    return this.customEventEnabled;
  }

  public void setCustomEventEnabled(boolean customEventEnabled) {
    this.customEventEnabled = customEventEnabled;
  }

  public String getCustomEvent() {
    return this.customEvent;
  }

  public void setCustomEvent(String customEvent) {
    this.customEvent = customEvent;
  }
}
