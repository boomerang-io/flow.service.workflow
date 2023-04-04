package io.boomerang.v4.data.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.v4.data.entity.SettingsEntity;

public interface SettingsRepository extends MongoRepository<SettingsEntity, String> {

  SettingsEntity findOneByKey(String key);

}
