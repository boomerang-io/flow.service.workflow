package io.boomerang.service.crud;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import io.boomerang.model.InsightsSummary;

public interface InsightsService {

  InsightsSummary getInsights(Optional<Date> from, Optional<Date> to, Pageable pageable,
      Optional<List<String>> workflowIds, Optional<List<String>> teamIds,
      Optional<List<String>> scopes);
}
