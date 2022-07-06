package io.boomerang.mongo.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.mongo.entity.ExtensionEntity;

public interface ExtensionRepository extends MongoRepository<ExtensionEntity, String> {

  List<ExtensionEntity> findByType(String type);  
}

