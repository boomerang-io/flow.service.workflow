package io.boomerang.model;

import org.springframework.beans.BeanUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.boomerang.model.projectstormv5.WorkflowRevision;
import io.boomerang.mongo.entity.RevisionEntity;
import io.boomerang.util.ModelConverterV5;

public class FlowWorkflowRevision extends WorkflowRevision {

  public FlowWorkflowRevision() {

  }

  public FlowWorkflowRevision(RevisionEntity entity) {
    WorkflowRevision revision = ModelConverterV5.convertToRestModel(entity);
    BeanUtils.copyProperties(revision, this);
  }

  @JsonIgnore
  public RevisionEntity convertToEntity() {
    return ModelConverterV5.convertToEntityModel(this);
  }
}
