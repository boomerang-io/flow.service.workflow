package io.boomerang.integrations.data.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.integrations.data.entity.IntegrationsEntity;

public interface IntegrationsRepository extends MongoRepository<IntegrationsEntity, String> {

  List<IntegrationsEntity> findByType(String type); 
  
  Optional<IntegrationsEntity> findByRef(String ref);
  
  Optional<IntegrationsEntity> findByIdAndType(String id, String type);
}

