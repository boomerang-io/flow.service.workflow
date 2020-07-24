package net.boomerangplatform.mongo.repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import net.boomerangplatform.mongo.entity.FlowWorkflowRevisionEntity;

public interface FlowWorkflowVersionRepository
    extends MongoRepository<FlowWorkflowRevisionEntity, String> {

  long countByworkFlowId(String workFlowId);

  @Override
  Optional<FlowWorkflowRevisionEntity> findById(String id);

  FlowWorkflowRevisionEntity findByworkFlowIdAndVersion(String workFlowId, long version);

  Page<FlowWorkflowRevisionEntity> findByworkFlowId(String string, Pageable pageable);

}
