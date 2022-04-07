package io.boomerang.model;

import io.boomerang.mongo.entity.RevisionEntity;
import io.boomerang.mongo.entity.WorkflowEntity;

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
