package io.boomerang.model;

import org.springframework.beans.BeanUtils;
import io.boomerang.v4.data.entity.SettingsEntity;


public class FlowSettings extends SettingsEntity{

  public FlowSettings() {
    super();
  }

  public FlowSettings(SettingsEntity entity) {
    BeanUtils.copyProperties(entity, this); // NOSONAR
  }
}
