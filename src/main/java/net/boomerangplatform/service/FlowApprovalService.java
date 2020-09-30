package net.boomerangplatform.service;

import java.util.List;
import net.boomerangplatform.model.Approval;
import net.boomerangplatform.mongo.entity.ApprovalEntity;

public interface FlowApprovalService {
  
  public void actionApproval(String id, Boolean approved);

  public List<Approval> getApprovalsForUser();

  public List<Approval> getApprovalsForTeam(String teamId);
  
  Approval getApprovalById(String id);
  ApprovalEntity getApprovalByTaskActivityId(String id);

}
