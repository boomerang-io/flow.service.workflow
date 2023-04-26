package io.boomerang.v4.data.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.v4.data.entity.SettingEntity;

public interface SettingsRepository extends MongoRepository<SettingEntity, String> {

  SettingEntity findOneByKey(String key);

}
