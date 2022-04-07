package io.boomerang.mongo.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.mongo.entity.FlowSettingsEntity;

public interface FlowSettingsRepository extends MongoRepository<FlowSettingsEntity, String> {

  FlowSettingsEntity findOneByKey(String key);

}
