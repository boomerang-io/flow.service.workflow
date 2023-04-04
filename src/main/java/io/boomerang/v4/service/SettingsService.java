package io.boomerang.v4.service;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import io.boomerang.model.AESAlgorithm;
import io.boomerang.mongo.model.Config;
import io.boomerang.mongo.model.EncryptionConfig;
import io.boomerang.v4.data.entity.SettingsEntity;
import io.boomerang.v4.data.repository.SettingsRepository;


@Service
public class SettingsService {

  private static final String SECURED_TYPE = "secured";

  @Autowired
  private SettingsRepository settingsRepository;

  @Autowired
  private EncryptionConfig encryptConfig;

  public List<SettingsEntity> getAllConfigurations() {
    return settingsRepository.findAll();
  }

  public Config getConfiguration(String key, String name) {
    final SettingsEntity settings = this.settingsRepository.findOneByKey(key);
    final List<Config> configList = settings.getConfig();
    final Optional<Config> result =
        configList.stream().parallel().filter(x -> name.equals(x.getKey())).findFirst();

    if (result.isPresent() && SECURED_TYPE.equalsIgnoreCase(result.get().getType())) {
      result.get().setValue(decrypt(result.get().getValue()));
    }

    return result.orElseThrow(
        () -> new IllegalArgumentException("Unable to find configuration object: " + name));
  }

  public SettingsEntity getConfigurationById(String id) {
    Optional<SettingsEntity> entity = settingsRepository.findById(id);
    if (entity.isPresent()) {
      showDecryptedValues(entity.get());
    }

    return entity.orElseThrow(
        () -> new IllegalArgumentException("Unable to find configuration with ID: " + id));
  }

  public SettingsEntity getConfigurationByKey(String key) {
    final SettingsEntity settingsEntity = settingsRepository.findOneByKey(key);
    if (settingsEntity != null) {
      showDecryptedValues(settingsEntity);
      return settingsEntity;
    } else {
      throw new IllegalArgumentException("Unable to find configuration key: " + key);
    }
  }

  public void updateConfiguration(SettingsEntity configuration) {
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
