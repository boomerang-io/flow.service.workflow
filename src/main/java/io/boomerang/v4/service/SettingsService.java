package io.boomerang.v4.service;

import java.util.List;
import io.boomerang.v4.data.entity.SettingEntity;
import io.boomerang.v4.model.AbstractParam;
import io.boomerang.v4.model.Setting;

public interface SettingsService {

  String getWebhookURL();

  String getEventURL();

  String getWFEURL();

  List<Setting> getAllSettings();

  AbstractParam getSetting(String key, String name);

  SettingEntity getSettingById(String id);

  SettingEntity getSettingByKey(String key);

  void updateSetting(SettingEntity configuration);

  List<Setting> updateSettings(List<Setting> settings);
  
}
