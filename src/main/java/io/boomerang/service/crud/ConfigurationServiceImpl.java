package io.boomerang.service.crud;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.boomerang.model.FlowSettings;
import io.boomerang.mongo.entity.FlowSettingsEntity;
import io.boomerang.mongo.model.Config;
import io.boomerang.mongo.model.ConfigurationType;
import io.boomerang.mongo.service.FlowSettingsService;
import io.boomerang.util.DateUtil;

@Service
public class ConfigurationServiceImpl implements ConfigurationService {

  @Autowired
  private FlowSettingsService serviceSettings;

  @Override
  public List<FlowSettings> getAllSettings() {
  

    final List<FlowSettings> settingList = new LinkedList<>();
    final List<FlowSettingsEntity> entityList = serviceSettings.getAllConfigurations();
    for (final FlowSettingsEntity entity : entityList) {
      final FlowSettings newSetting = new FlowSettings(entity);
      settingList.add(newSetting);
    }

    return settingList;
  }

  @Override
  public List<FlowSettings> updateSettings(List<FlowSettings> settings) {
    for (final FlowSettings setting : settings) {
      final FlowSettingsEntity entity = serviceSettings.getConfigurationById(setting.getId());
      if (entity.getType() == ConfigurationType.ValuesList) {
        setConfigsValue(setting, entity);
      }
      entity.setLastModiifed(DateUtil.asDate(LocalDateTime.now()));
      if (entity.getKey().equals("eventing")) {
        boolean eventingDisabled = !entity.getConfig().stream()
            .filter(c -> c.getKey().equals("enable.eventing")).findFirst().get().getBooleanValue();
        if (eventingDisabled) {
          for (Config config : entity.getConfig()) {
            config.setValue("false");
          }
        }
      }
      serviceSettings.updateConfiguration(entity);
    }
    return this.getAllSettings();
  }


  private void setConfigsValue(final FlowSettings setting, final FlowSettingsEntity entity) {
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
