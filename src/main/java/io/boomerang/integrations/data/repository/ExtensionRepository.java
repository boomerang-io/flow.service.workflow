package io.boomerang.integrations.data.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.integrations.data.entity.IntegrationEntity;

public interface ExtensionRepository extends MongoRepository<IntegrationEntity, String> {

  List<IntegrationEntity> findByType(String type);  
}

