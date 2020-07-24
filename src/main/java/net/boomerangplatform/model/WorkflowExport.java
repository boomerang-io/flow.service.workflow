package net.boomerangplatform.model;

import net.boomerangplatform.mongo.entity.FlowWorkflowEntity;
import net.boomerangplatform.mongo.entity.FlowWorkflowRevisionEntity;

public class WorkflowExport extends WorkflowSummary {

  public WorkflowExport() {
    super();
  }

  public WorkflowExport(FlowWorkflowEntity entity) {
    super(entity);
  }

  private FlowWorkflowRevisionEntity latestRevision;

  public FlowWorkflowRevisionEntity getLatestRevision() {
    return latestRevision;
  }

  public void setLatestRevision(FlowWorkflowRevisionEntity latestRevision) {
    this.latestRevision = latestRevision;
  }

}
