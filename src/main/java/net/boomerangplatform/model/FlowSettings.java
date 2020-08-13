package net.boomerangplatform.model;

import org.springframework.beans.BeanUtils;
import net.boomerangplatform.mongo.entity.FlowSettingsEntity;


public class FlowSettings extends FlowSettingsEntity{

  public FlowSettings() {
    super();
  }

  public FlowSettings(FlowSettingsEntity entity) {
    BeanUtils.copyProperties(entity, this); // NOSONAR
  }
}
