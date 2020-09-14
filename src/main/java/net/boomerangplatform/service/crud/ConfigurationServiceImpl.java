package net.boomerangplatform.service.crud;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import net.boomerangplatform.model.FlowSettings;
import net.boomerangplatform.mongo.entity.FlowSettingsEntity;
import net.boomerangplatform.mongo.entity.FlowUserEntity;
import net.boomerangplatform.mongo.model.Config;
import net.boomerangplatform.mongo.model.ConfigurationType;
import net.boomerangplatform.mongo.model.UserType;
import net.boomerangplatform.mongo.service.FlowSettingsService;
import net.boomerangplatform.mongo.service.FlowUserService;
import net.boomerangplatform.security.service.UserDetailsService;
import net.boomerangplatform.service.UserIdentityService;
import net.boomerangplatform.util.DateUtil;

@Service
public class ConfigurationServiceImpl implements ConfigurationService {

  @Autowired
  private FlowSettingsService serviceSettings;

  @Autowired
  private UserDetailsService userDetailsService;

  @Autowired
  private FlowUserService userService;

  @Autowired
  UserIdentityService service;

  @Override
  public List<FlowSettings> getAllSettings() {
    validateUser();

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
    validateUser();
    for (final FlowSettings setting : settings) {
      final FlowSettingsEntity entity = serviceSettings.getConfigurationById(setting.getId());
      if (entity.getType() == ConfigurationType.ValuesList) {
        setConfigsValue(setting, entity);
      }
      entity.setLastModiifed(DateUtil.asDate(LocalDateTime.now()));

      serviceSettings.updateConfiguration(entity);
    }

    return this.getAllSettings();
  }

  protected void validateUser() {

    FlowUserEntity userEntity = service.getCurrentUser();
    if (userEntity == null || (!userEntity.getType().equals(UserType.admin)
        && !userEntity.getType().equals(UserType.operator))) {

      throw new HttpClientErrorException(HttpStatus.FORBIDDEN);
    }
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
