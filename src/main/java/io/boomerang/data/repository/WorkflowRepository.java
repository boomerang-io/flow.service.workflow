package io.boomerang.data.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.data.entity.TeamEntity;

public interface WorkflowRepository extends MongoRepository<WorkflowEntity, String> {
  
}
