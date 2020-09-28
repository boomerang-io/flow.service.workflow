package net.boomerangplatform.model;

public class WorkflowShortSummary {
  
  private String id;
  private String workflowName;
  private String token;
  private boolean webHookEnabled;
  
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }
  public String getWorkflowName() {
    return workflowName;
  }
  public void setWorkflowName(String workflowName) {
    this.workflowName = workflowName;
  }
  public String getToken() {
    return token;
  }
  public void setToken(String token) {
    this.token = token;
  }
  public boolean isWebHookEnabled() {
    return webHookEnabled;
  }
  public void setWebHookEnabled(boolean webHookEnabled) {
    this.webHookEnabled = webHookEnabled;
  }

}
