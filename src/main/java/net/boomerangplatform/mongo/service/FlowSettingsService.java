package net.boomerangplatform.mongo.service;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import net.boomerangplatform.model.AESAlgorithm;
import net.boomerangplatform.mongo.entity.FlowSettingsEntity;
import net.boomerangplatform.mongo.model.Config;
import net.boomerangplatform.mongo.model.EncryptionConfig;
import net.boomerangplatform.mongo.repository.FlowSettingsRepository;


@Service
public class FlowSettingsService {

  private static final String SECURED_TYPE = "secured";

  @Autowired
  private FlowSettingsRepository configurationRepository;

  @Autowired
  private EncryptionConfig encryptConfig;

  public List<FlowSettingsEntity> getAllConfigurations() {
    return configurationRepository.findAll();
  }

  public Config getConfiguration(String key, String name) {
    final FlowSettingsEntity settings = this.configurationRepository.findOneByKey(key);
    final List<Config> configList = settings.getConfig();
    final Optional<Config> result =
        configList.stream().parallel().filter(x -> name.equals(x.getKey())).findFirst();

    if (result.isPresent() && SECURED_TYPE.equalsIgnoreCase(result.get().getType())) {
      result.get().setValue(decrypt(result.get().getValue()));
    }

    return result.orElseThrow(
        () -> new IllegalArgumentException("Unable to find configuration object: " + name));
  }

  public FlowSettingsEntity getConfigurationById(String id) {
    Optional<FlowSettingsEntity> entity = configurationRepository.findById(id);
    if (entity.isPresent()) {
      showDecryptedValues(entity.get());
    }

    return entity.orElseThrow(
        () -> new IllegalArgumentException("Unable to find configuration with ID: " + id));
  }

  public FlowSettingsEntity getConfigurationByKey(String key) {
    final FlowSettingsEntity settingsEntity = configurationRepository.findOneByKey(key);
    if (settingsEntity != null) {
      showDecryptedValues(settingsEntity);
      return settingsEntity;
    } else {
      throw new IllegalArgumentException("Unable to find configuration key: " + key);
    }
  }

  public void updateConfiguration(FlowSettingsEntity configuration) {
    setEncryptedValues(configuration);

    this.configurationRepository.save(configuration);
  }

  private void setEncryptedValues(FlowSettingsEntity configuration) {
    configuration.getConfig().stream()
        .filter(config -> SECURED_TYPE.equalsIgnoreCase(config.getType()))
        .forEach(c -> c.setValue(encrypt(c.getValue())));
  }

  private void showDecryptedValues(FlowSettingsEntity configuration) {
    configuration.getConfig().stream()
        .filter(config -> SECURED_TYPE.equalsIgnoreCase(config.getType()))
        .forEach(c -> c.setValue(decrypt(c.getValue())));
  }

  private String encrypt(String value) {

    if (StringUtils.isEmpty(value) || value.startsWith("crypt_v1")) {
      return value;
    }

    return StringUtils.isEmpty(value) ? value
        : ("crypt_v1{AES|"
            + AESAlgorithm.encrypt(value, encryptConfig.getSecretKey(), encryptConfig.getSalt())
            + "}");
  }

  private String decrypt(String value) {

    if (StringUtils.isEmpty(value) || !value.startsWith("crypt_v1")) {
      return value;
    }

    String replacedValue = value.replace("crypt_v1{AES|", "").replace("}", "");
    return AESAlgorithm.decrypt(replacedValue, encryptConfig.getSecretKey(),
        encryptConfig.getSalt());

  }
}
