package io.boomerang.v4.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import io.boomerang.v4.model.ref.WorkflowRunInsight;

@Service
public class InsightsServiceImpl implements InsightsService {

  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  private WorkflowRunService workflowRunService;

  @Override
  public WorkflowRunInsight getInsights(Optional<Date> from, Optional<Date> to,
      Pageable pageable, Optional<List<String>> workflowIds, Optional<List<String>> teamIds,
      Optional<List<String>> statuses, Optional<List<String>> triggers) {

    return workflowRunService.insight(Optional.of(from.get().getTime()),
            Optional.of(to.get().getTime()), Optional.empty(), workflowIds,
            teamIds);
  }
}
