package net.boomerangplatform.model;

import org.springframework.beans.BeanUtils;
import net.boomerangplatform.mongo.entity.WorkflowEntity;

public class FlowWorkflow extends WorkflowEntity {

  public FlowWorkflow() {

  }

  public FlowWorkflow(WorkflowEntity entity) {
    BeanUtils.copyProperties(entity, this);
  }

}
