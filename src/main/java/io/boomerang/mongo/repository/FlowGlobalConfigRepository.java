package io.boomerang.mongo.repository;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.mongo.entity.FlowGlobalConfigEntity;

public interface FlowGlobalConfigRepository
    extends MongoRepository<FlowGlobalConfigEntity, String> {

  @Override
  Optional<FlowGlobalConfigEntity> findById(String id);

}
