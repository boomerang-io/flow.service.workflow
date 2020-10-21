package net.boomerangplatform.mongo.repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import net.boomerangplatform.mongo.entity.RevisionEntity;

public interface FlowWorkflowVersionRepository
    extends MongoRepository<RevisionEntity, String> {

  long countByworkFlowId(String workFlowId);

  @Override
  Optional<RevisionEntity> findById(String id);

  RevisionEntity findByworkFlowIdAndVersion(String workFlowId, long version);

  Page<RevisionEntity> findByworkFlowId(String string, Pageable pageable);

}
