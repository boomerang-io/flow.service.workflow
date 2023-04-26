package io.boomerang.v4.model;

import org.springframework.beans.BeanUtils;
import io.boomerang.v4.data.entity.SettingEntity;


public class Setting extends SettingEntity{

  public Setting() {
    super();
  }

  public Setting(SettingEntity entity) {
    BeanUtils.copyProperties(entity, this); // NOSONAR
  }
}
