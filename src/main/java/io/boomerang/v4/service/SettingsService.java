package io.boomerang.v4.service;

import java.util.List;
import io.boomerang.mongo.model.Config;
import io.boomerang.v4.data.entity.SettingsEntity;
import io.boomerang.v4.model.AbstractParam;

public interface SettingsService {

  String getWebhookURL();

  String getEventURL();

  String getWFEURL();

  List<SettingsEntity> getAllSettings();

  AbstractParam getSetting(String key, String name);

  SettingsEntity getSettingById(String id);

  SettingsEntity getSettingByKey(String key);

  void updateSetting(SettingsEntity configuration);
  
}
