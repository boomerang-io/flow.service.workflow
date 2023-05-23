package io.boomerang.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import io.boomerang.v4.model.ref.WorkflowRunInsight;

public interface InsightsService {

  WorkflowRunInsight getInsights(Optional<Date> from, Optional<Date> to, Pageable pageable,
      Optional<List<String>> workflowIds, Optional<List<String>> teamIds,
      Optional<List<String>> scopes,
      Optional<List<String>> statuses);
}
