package io.boomerang.mongo.repository;

import java.util.Date;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.model.ApprovalStatus;
import io.boomerang.mongo.entity.ApprovalEntity;
import io.boomerang.mongo.model.ManualType;


public interface FlowApprovalRepository extends MongoRepository<ApprovalEntity, String> {

  List<ApprovalEntity> findByTeamId(String teamId);

  ApprovalEntity findByTaskActivityId(String id);

  long countByActivityIdAndStatus(String activityId, ApprovalStatus status);
  
  long countByCreationDateBetween(Date from, Date to);
  long countByTypeAndCreationDateBetween(ManualType type, Date from, Date to);

  
}

