package net.boomerangplatform.model;

import org.springframework.beans.BeanUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import net.boomerangplatform.model.projectstormv5.WorkflowRevision;
import net.boomerangplatform.mongo.entity.FlowWorkflowRevisionEntity;
import net.boomerangplatform.util.ModelConverterV5;

public class FlowWorkflowRevision extends WorkflowRevision {

  public FlowWorkflowRevision() {

  }

  public FlowWorkflowRevision(FlowWorkflowRevisionEntity entity) {
    WorkflowRevision revision = ModelConverterV5.convertToRestModel(entity);
    BeanUtils.copyProperties(revision, this);
  }

  @JsonIgnore
  public FlowWorkflowRevisionEntity convertToEntity() {
    return ModelConverterV5.convertToEntityModel(this);
  }
}
