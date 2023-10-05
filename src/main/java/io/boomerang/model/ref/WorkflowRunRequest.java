package io.boomerang.model.ref;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class WorkflowRunRequest {

  private Map<String, String> labels = new HashMap<>();

  private Map<String, Object> annotations = new HashMap<>();
  
  private List<RunParam> params = new LinkedList<>();
  
  private List<WorkflowWorkspace> workspaces = new LinkedList<>();
  
  private Long timeout;
  
  private Long retries;
  
  private String trigger;
  
  private Boolean debug;

  @Override
  public String toString() {
    return "WorkflowRunRequest [labels=" + labels + ", annotations=" + annotations + ", params="
        + params + ", workspaces=" + workspaces + ", timeout=" + timeout + ", retries=" + retries
        + ", trigger=" + trigger + ", debug=" + debug + "]";
  }

  public Map<String, String> getLabels() {
    return labels;
  }

  public void setLabels(Map<String, String> labels) {
    this.labels = labels;
  }

  public Map<String, Object> getAnnotations() {
    return annotations;
  }

  public void setAnnotations(Map<String, Object> annotations) {
    this.annotations = annotations;
  }

  public List<RunParam> getParams() {
    return params;
  }

  public void setParams(List<RunParam> params) {
    this.params = params;
  }

  public List<WorkflowWorkspace> getWorkspaces() {
    return workspaces;
  }

  public void setWorkspaces(List<WorkflowWorkspace> workspaces) {
    this.workspaces = workspaces;
  }

  public Long getTimeout() {
    return timeout;
  }

  public void setTimeout(Long timeout) {
    this.timeout = timeout;
  }

  public Long getRetries() {
    return retries;
  }

  public void setRetries(Long retries) {
    this.retries = retries;
  }

  public String getTrigger() {
    return trigger;
  }

  public void setTrigger(String trigger) {
    this.trigger = trigger;
  }

  public Boolean getDebug() {
    return debug;
  }

  public void setDebug(Boolean debug) {
    this.debug = debug;
  }
}
