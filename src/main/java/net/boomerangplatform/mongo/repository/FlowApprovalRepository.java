package net.boomerangplatform.mongo.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import net.boomerangplatform.model.ApprovalStatus;
import net.boomerangplatform.mongo.entity.ApprovalEntity;


public interface FlowApprovalRepository extends MongoRepository<ApprovalEntity, String> {

  List<ApprovalEntity> findByTeamId(String teamId);

  ApprovalEntity findByTaskActivityId(String id);

  long countByActivityIdAndStatus(String activityId, ApprovalStatus status);
  
  
}

