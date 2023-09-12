package io.boomerang.data.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.data.entity.SettingEntity;

public interface SettingsRepository extends MongoRepository<SettingEntity, String> {

  SettingEntity findOneByKey(String key);

}
