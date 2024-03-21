package io.boomerang.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import io.boomerang.model.ref.WorkflowRunInsight;

@Service
public class InsightsServiceImpl implements InsightsService {

  @Autowired
  private WorkflowRunService workflowRunService;

  /*
   * Wraps insight call on WorkflowRuns
   */
  @Override
  public WorkflowRunInsight getInsights(String team, Optional<Date> from, Optional<Date> to,
      Pageable pageable, Optional<List<String>> workflowIds,
      Optional<List<String>> statuses, Optional<List<String>> triggers) {

    Optional<Long> fromLong = Optional.empty();
    if (from.isPresent()) {
      fromLong = Optional.of(from.get().getTime());
    }
    
    Optional<Long> toLong = Optional.empty();
    if (to.isPresent()) {
      toLong = Optional.of(to.get().getTime());
    }
    
    return workflowRunService.insight(team, fromLong,
            toLong, Optional.empty(), workflowIds);
  }
}
