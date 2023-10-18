package io.boomerang.integrations.data.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.integrations.data.entity.IntegrationsEntity;

public interface IntegrationsRepository extends MongoRepository<IntegrationsEntity, String> {

  List<IntegrationsEntity> findByType(String type);  
}

