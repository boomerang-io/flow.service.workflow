package io.boomerang.model;

import org.springframework.beans.BeanUtils;
import io.boomerang.mongo.entity.WorkflowEntity;

public class WorkflowSummary extends WorkflowEntity {

  private long revisionCount;
  private boolean templateUpgradesAvailable;


  public WorkflowSummary() {

  }

  public WorkflowSummary(WorkflowEntity entity) {
    BeanUtils.copyProperties(entity, this);
  }

  public long getRevisionCount() {
    return revisionCount;
  }

  public void setRevisionCount(long revisionCount) {
    this.revisionCount = revisionCount;
  }

  public boolean isTemplateUpgradesAvailable() {
    return templateUpgradesAvailable;
  }

  public void setTemplateUpgradesAvailable(boolean templateUpgradesAvailable) {
    this.templateUpgradesAvailable = templateUpgradesAvailable;
  }



}
