package net.boomerangplatform.model;

import org.springframework.beans.BeanUtils;
import net.boomerangplatform.mongo.entity.FlowWorkflowEntity;

public class FlowWorkflow extends FlowWorkflowEntity {

  public FlowWorkflow() {

  }

  public FlowWorkflow(FlowWorkflowEntity entity) {
    BeanUtils.copyProperties(entity, this);
  }

}
