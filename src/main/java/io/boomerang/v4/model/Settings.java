package io.boomerang.v4.model;

import org.springframework.beans.BeanUtils;
import io.boomerang.v4.data.entity.SettingsEntity;


public class Settings extends SettingsEntity{

  public Settings() {
    super();
  }

  public Settings(SettingsEntity entity) {
    BeanUtils.copyProperties(entity, this); // NOSONAR
  }
}
