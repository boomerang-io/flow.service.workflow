package io.boomerang.v4.service;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import io.boomerang.model.AESAlgorithm;
import io.boomerang.mongo.model.EncryptionConfig;
import io.boomerang.v4.data.entity.SettingsEntity;
import io.boomerang.v4.data.repository.SettingsRepository;
import io.boomerang.v4.model.AbstractParam;


@Service
public class SettingsServiceImpl implements SettingsService {

  private static final String SECURED_TYPE = "secured";
  
  @Value("${flow.workflow.webhook.url}")
  private String webhookUrl;
  
  
  @Value("${flow.workflow.wfe.url}")
  private String waitForEventUrl;
  
  
  @Value("${flow.workflow.event.url}")
  private String eventUrl;

  @Autowired
  private SettingsRepository settingsRepository;

  @Autowired
  private EncryptionConfig encryptConfig;
  
//  @Override
//  public List<Settings> getAllSettings() {
//    final List<Settings> settingList = new LinkedList<>();
//    final List<SettingsEntity> entityList = serviceSettings.getAllSettings();
//    for (final SettingsEntity entity : entityList) {
//      final Settings newSetting = new Settings(entity);
//      settingList.add(newSetting);
//    }
//
//    return settingList;
//  }
//
//  @Override
//  public List<Settings> updateSettings(List<Settings> settings) {
//    for (final Settings setting : settings) {
//      final SettingsEntity entity = serviceSettings.getSettingById(setting.getId());
//      if (entity.getType() == ConfigurationType.ValuesList) {
//        setConfigsValue(setting, entity);
//      }
//      entity.setLastModiifed(DateUtil.asDate(LocalDateTime.now()));
//
//      serviceSettings.updateSetting(entity);
//    }
//
//    return this.getAllSettings();
//  }
//
//
//  private void setConfigsValue(final Settings setting, final SettingsEntity entity) {
//    for (final Config config : setting.getConfig()) {
//      final String newValue = config.getValue();
//      final Optional<Config> result = entity.getConfig().stream().parallel()
//          .filter(x -> config.getKey().equals(x.getKey())).findFirst();
//      if (result.isPresent()) {
//        final Config originalConfig = result.get();
//        originalConfig.setValue(newValue);
//      }
//    }
//  }

  @Override
  public String getWebhookURL() {
    return webhookUrl;
  }

  @Override
  public String getEventURL() {
    return eventUrl;
  }

  @Override
  public String getWFEURL() {
    return waitForEventUrl;
  }

  @Override
  public List<SettingsEntity> getAllSettings() {
    return settingsRepository.findAll();
  }

  @Override
  public AbstractParam getSetting(String key, String name) {
    final SettingsEntity settings = this.settingsRepository.findOneByKey(key);
    final List<AbstractParam> configList = settings.getConfig();
    final Optional<AbstractParam> result =
        configList.stream().parallel().filter(x -> name.equals(x.getKey())).findFirst();

    if (result.isPresent() && SECURED_TYPE.equalsIgnoreCase(result.get().getType())) {
      result.get().setValue(decrypt(result.get().getValue()));
    }

    return result.orElseThrow(
        () -> new IllegalArgumentException("Unable to find configuration object: " + name));
  }

  @Override
  public SettingsEntity getSettingById(String id) {
    Optional<SettingsEntity> entity = settingsRepository.findById(id);
    if (entity.isPresent()) {
      showDecryptedValues(entity.get());
    }

    return entity.orElseThrow(
        () -> new IllegalArgumentException("Unable to find configuration with ID: " + id));
  }

  @Override
  public SettingsEntity getSettingByKey(String key) {
    final SettingsEntity settingsEntity = settingsRepository.findOneByKey(key);
    if (settingsEntity != null) {
      showDecryptedValues(settingsEntity);
      return settingsEntity;
    } else {
      throw new IllegalArgumentException("Unable to find configuration key: " + key);
    }
  }

  @Override
  public void updateSetting(SettingsEntity configuration) {
    setEncryptedValues(configuration);

    this.settingsRepository.save(configuration);
  }

  private void setEncryptedValues(SettingsEntity configuration) {
    configuration.getConfig().stream()
        .filter(config -> SECURED_TYPE.equalsIgnoreCase(config.getType()))
        .forEach(c -> c.setValue(encrypt(c.getValue())));
  }

  private void showDecryptedValues(SettingsEntity configuration) {
    configuration.getConfig().stream()
        .filter(config -> SECURED_TYPE.equalsIgnoreCase(config.getType()))
        .forEach(c -> c.setValue(decrypt(c.getValue())));
  }

  private String encrypt(String value) {

    if (StringUtils.hasText(value) || value.startsWith("crypt_v1")) {
      return value;
    }

    return StringUtils.hasText(value) ? value
        : ("crypt_v1{AES|"
            + AESAlgorithm.encrypt(value, encryptConfig.getSecretKey(), encryptConfig.getSalt())
            + "}");
  }

  private String decrypt(String value) {

    if (StringUtils.hasText(value) || !value.startsWith("crypt_v1")) {
      return value;
    }

    String replacedValue = value.replace("crypt_v1{AES|", "").replace("}", "");
    return AESAlgorithm.decrypt(replacedValue, encryptConfig.getSecretKey(),
        encryptConfig.getSalt());

  }
}
