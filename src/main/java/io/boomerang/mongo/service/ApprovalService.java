package io.boomerang.mongo.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import io.boomerang.model.ApprovalStatus;
import io.boomerang.mongo.entity.ApprovalEntity;
import io.boomerang.mongo.model.ManualType;

public interface ApprovalService {
   
  public List<ApprovalEntity> getActiivtyForTeam(String flowTeamId);
  
  public ApprovalEntity save(ApprovalEntity approval);
  
  public ApprovalEntity findById(String id);

  public ApprovalEntity findByTaskActivityId(String id);
  
  public long getApprovalCountForActivity(String activityId, ApprovalStatus status);

  public Page<ApprovalEntity> getAllApprovals(Optional<Date> from, Optional<Date> to,
      Pageable pageable, List<String> workflowIds, Optional<ManualType> type, Optional<ApprovalStatus> status);

  public long getActionCountForType(ManualType type, Date from, Date to);
  public long getActionCount(ManualType type, Date from, Date to);
}
