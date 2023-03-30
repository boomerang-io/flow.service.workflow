package io.boomerang.v4.model.ref;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class WorkflowRunRequest {

  private Map<String, String> labels = new HashMap<>();

  private Map<String, Object> annotations = new HashMap<>();
  
  private List<RunParam> params = new LinkedList<>();
  
  private List<WorkflowWorkspace> workspaces = new LinkedList<>();
  
  private long timeout;
  
  private long retries;

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

  public long getTimeout() {
    return timeout;
  }

  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }

  public long getRetries() {
    return retries;
  }

  public void setRetries(long retries) {
    this.retries = retries;
  }
}
