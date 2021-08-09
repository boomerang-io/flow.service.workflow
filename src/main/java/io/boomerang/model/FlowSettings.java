package io.boomerang.model;

import org.springframework.beans.BeanUtils;
import io.boomerang.mongo.entity.FlowSettingsEntity;


public class FlowSettings extends FlowSettingsEntity{

  public FlowSettings() {
    super();
  }

  public FlowSettings(FlowSettingsEntity entity) {
    BeanUtils.copyProperties(entity, this); // NOSONAR
  }
}
