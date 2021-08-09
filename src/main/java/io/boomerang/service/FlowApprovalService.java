package io.boomerang.service;

import java.util.List;
import io.boomerang.model.Approval;
import io.boomerang.model.ApprovalRequest;

public interface FlowApprovalService {
  
  public void actionApproval(ApprovalRequest request);

  public List<Approval> getApprovalsForUser();

  public List<Approval> getApprovalsForTeam(String teamId);
  
  Approval getApprovalById(String id);
  Approval getApprovalByTaskActivityId(String id);

}
