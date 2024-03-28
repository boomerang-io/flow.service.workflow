package io.boomerang.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import io.boomerang.model.ref.WorkflowRunInsight;

public interface InsightsService {

  WorkflowRunInsight get(String team, Date from, Date to, Optional<List<String>> workflowRefs,
      Optional<List<String>> statuses);
}
