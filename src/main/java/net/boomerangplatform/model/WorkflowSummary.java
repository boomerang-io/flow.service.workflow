package net.boomerangplatform.model;

import org.springframework.beans.BeanUtils;
import net.boomerangplatform.mongo.entity.FlowWorkflowEntity;

public class WorkflowSummary extends FlowWorkflowEntity {

  private long revisionCount;


  private boolean templateUpgradesAvailable;


  public WorkflowSummary(FlowWorkflowEntity entity) {
    BeanUtils.copyProperties(entity, this);
  }

  public WorkflowSummary() {

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
