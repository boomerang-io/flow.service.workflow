package io.boomerang.service.crud;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.boomerang.model.FlowSettings;
import io.boomerang.mongo.model.Config;
import io.boomerang.mongo.model.ConfigurationType;
import io.boomerang.util.DateUtil;
import io.boomerang.v4.data.entity.SettingsEntity;
import io.boomerang.v4.service.SettingsService;

@Service
public class ConfigurationServiceImpl implements ConfigurationService {

  @Autowired
  private SettingsService serviceSettings;

  @Override
  public List<FlowSettings> getAllSettings() {
  

    final List<FlowSettings> settingList = new LinkedList<>();
    final List<SettingsEntity> entityList = serviceSettings.getAllConfigurations();
    for (final SettingsEntity entity : entityList) {
      final FlowSettings newSetting = new FlowSettings(entity);
      settingList.add(newSetting);
    }

    return settingList;
  }

  @Override
  public List<FlowSettings> updateSettings(List<FlowSettings> settings) {
    for (final FlowSettings setting : settings) {
      final SettingsEntity entity = serviceSettings.getConfigurationById(setting.getId());
      if (entity.getType() == ConfigurationType.ValuesList) {
        setConfigsValue(setting, entity);
      }
      entity.setLastModiifed(DateUtil.asDate(LocalDateTime.now()));

      serviceSettings.updateConfiguration(entity);
    }

    return this.getAllSettings();
  }


  private void setConfigsValue(final FlowSettings setting, final SettingsEntity entity) {
    for (final Config config : setting.getConfig()) {
      final String newValue = config.getValue();
      final Optional<Config> result = entity.getConfig().stream().parallel()
          .filter(x -> config.getKey().equals(x.getKey())).findFirst();
      if (result.isPresent()) {
        final Config originalConfig = result.get();
        originalConfig.setValue(newValue);
      }
    }
  }
}
