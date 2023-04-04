package io.boomerang.v4.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import io.boomerang.model.ActionSummary;
import io.boomerang.model.ApprovalRequest;
import io.boomerang.model.ApprovalStatus;
import io.boomerang.model.ListActionResponse;
import io.boomerang.model.teams.Action;
import io.boomerang.mongo.model.ManualType;

public interface ActionServiceV4 {
  
  public void actionApproval(ApprovalRequest request);

  Action getApprovalById(String id);
  Action getApprovalByTaskActivityId(String id);

  public ListActionResponse getAllActions(Optional<Date> from, Optional<Date> to,
      Pageable pageable, Optional<List<String>> workflowIds, Optional<List<String>> teamIds,
      Optional<ManualType> type, Optional<List<String>> scopes, String string, Direction direction, Optional<ApprovalStatus> status);

  public ActionSummary getActionSummary( Optional<Date> fromDate,  Optional<Date> toDate, Optional<List<String>> workflowIds, Optional<List<String>> teamIds, Optional<ApprovalStatus> status, Optional<List<String>> scopes);

}
