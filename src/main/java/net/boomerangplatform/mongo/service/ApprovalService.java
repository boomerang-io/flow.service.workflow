package net.boomerangplatform.mongo.service;

import java.util.List;
import net.boomerangplatform.mongo.entity.ApprovalEntity;

public interface ApprovalService {
   
  public List<ApprovalEntity> getActiivtyForTeam(String flowTeamId);
  
  public ApprovalEntity createApproval(ApprovalEntity approval);
  
  public ApprovalEntity findById(String id);
  

}
