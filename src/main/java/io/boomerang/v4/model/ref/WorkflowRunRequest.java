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
}
