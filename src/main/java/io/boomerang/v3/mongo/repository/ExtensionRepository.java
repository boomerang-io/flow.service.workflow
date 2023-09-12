package io.boomerang.v3.mongo.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.v3.mongo.entity.ExtensionEntity;

public interface ExtensionRepository extends MongoRepository<ExtensionEntity, String> {

  List<ExtensionEntity> findByType(String type);  
}

