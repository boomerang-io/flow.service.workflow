package io.boomerang.model;

import org.springframework.beans.BeanUtils;
import io.boomerang.mongo.entity.WorkflowEntity;

public class FlowWorkflow extends WorkflowEntity {

  public FlowWorkflow() {

  }

  public FlowWorkflow(WorkflowEntity entity) {
    BeanUtils.copyProperties(entity, this);
  }

}
