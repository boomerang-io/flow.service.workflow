package net.boomerangplatform.model;

import net.boomerangplatform.mongo.entity.RevisionEntity;
import net.boomerangplatform.mongo.entity.WorkflowEntity;

public class WorkflowExport extends WorkflowSummary {

  public WorkflowExport() {
    super();
  }

  public WorkflowExport(WorkflowEntity entity) {
    super(entity);
  }

  private RevisionEntity latestRevision;

  public RevisionEntity getLatestRevision() {
    return latestRevision;
  }

  public void setLatestRevision(RevisionEntity latestRevision) {
    this.latestRevision = latestRevision;
  }

}
