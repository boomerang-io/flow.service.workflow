package net.boomerangplatform.service;

import java.util.List;
import net.boomerangplatform.model.Approval;
import net.boomerangplatform.model.ApprovalRequest;

public interface FlowApprovalService {
  
  public void actionApproval(ApprovalRequest request);

  public List<Approval> getApprovalsForUser();

  public List<Approval> getApprovalsForTeam(String teamId);
  
  Approval getApprovalById(String id);
  Approval getApprovalByTaskActivityId(String id);

}
