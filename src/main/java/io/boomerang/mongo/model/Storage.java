package io.boomerang.mongo.model;

public class Storage {

  private WorkflowStorage workflow;
  private WorkspaceStorage workspace;

  public WorkflowStorage getWorkflow() {
    return workflow;
  }

  public void setWorkflow(WorkflowStorage workflow) {
    this.workflow = workflow;
  }

  public WorkspaceStorage getWorkspace() {
    return workspace;
  }

  public void setWorkspace(WorkspaceStorage workspace) {
    this.workspace = workspace;
  }



}
