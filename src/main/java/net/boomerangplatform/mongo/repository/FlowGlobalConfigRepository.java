package net.boomerangplatform.mongo.repository;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import net.boomerangplatform.mongo.entity.FlowGlobalConfigEntity;

public interface FlowGlobalConfigRepository
    extends MongoRepository<FlowGlobalConfigEntity, String> {

  @Override
  Optional<FlowGlobalConfigEntity> findById(String id);

}
