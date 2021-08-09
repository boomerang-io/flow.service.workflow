package io.boomerang.mongo.service;

import java.util.List;
import io.boomerang.model.ApprovalStatus;
import io.boomerang.mongo.entity.ApprovalEntity;

public interface ApprovalService {
   
  public List<ApprovalEntity> getActiivtyForTeam(String flowTeamId);
  
  public ApprovalEntity save(ApprovalEntity approval);
  
  public ApprovalEntity findById(String id);

  public ApprovalEntity findByTaskActivityId(String id);
  
  public long getApprovalCountForActivity(String activityId, ApprovalStatus status);

}
