package io.boomerang.mongo.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.model.ApprovalStatus;
import io.boomerang.mongo.entity.ApprovalEntity;


public interface FlowApprovalRepository extends MongoRepository<ApprovalEntity, String> {

  List<ApprovalEntity> findByTeamId(String teamId);

  ApprovalEntity findByTaskActivityId(String id);

  long countByActivityIdAndStatus(String activityId, ApprovalStatus status);
  
  
}

