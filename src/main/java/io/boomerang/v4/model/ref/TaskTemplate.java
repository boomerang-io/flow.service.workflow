package io.boomerang.v4.model.ref;

import org.springframework.beans.BeanUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.boomerang.data.entity.TaskTemplateEntity;

@JsonInclude(Include.NON_NULL)
public class TaskTemplate extends TaskTemplateEntity {

  public TaskTemplate() {

  }

  public TaskTemplate(TaskTemplateEntity entity) {
    BeanUtils.copyProperties(entity, this);
  }
}
