package io.boomerang.service;

import java.util.List;
import io.boomerang.data.entity.SettingEntity;
import io.boomerang.model.AbstractParam;
import io.boomerang.model.Setting;

public interface SettingsService {

  String getWebhookURL();

  String getEventURL();

  String getWFEURL();

  List<Setting> getAllSettings();

  AbstractParam getSettingConfig(String key, String name);

  SettingEntity getSettingById(String id);

  SettingEntity getSettingByKey(String key);

  void updateSetting(SettingEntity configuration);

  List<Setting> updateSettings(List<Setting> settings);
  
}
