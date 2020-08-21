package net.boomerangplatform.mongo.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import net.boomerangplatform.mongo.entity.FlowSettingsEntity;

public interface FlowSettingsRepository extends MongoRepository<FlowSettingsEntity, String> {

  FlowSettingsEntity findOneByKey(String key);

}
