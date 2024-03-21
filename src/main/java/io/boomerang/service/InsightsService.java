package io.boomerang.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import io.boomerang.model.ref.WorkflowRunInsight;

public interface InsightsService {

  WorkflowRunInsight getInsights(String team, Optional<Date> from, Optional<Date> to,
      Pageable pageable, Optional<List<String>> workflowIds, Optional<List<String>> statuses,
      Optional<List<String>> triggers);
}
