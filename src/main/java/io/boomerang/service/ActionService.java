package io.boomerang.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import io.boomerang.model.Action;
import io.boomerang.model.ActionRequest;
import io.boomerang.model.ActionSummary;
import io.boomerang.model.enums.ref.ActionStatus;
import io.boomerang.model.enums.ref.ActionType;

public interface ActionService {
  
  void action(List<ActionRequest> requests);

  Action get(String id);

  Action getByTaskRun(String id);

  ActionSummary summary(Optional<Date> fromDate, Optional<Date> toDate,
      Optional<List<String>> workflows, Optional<List<String>> scopes);

  Page<Action> query(Optional<Date> from, Optional<Date> to, Pageable pageable,
      Optional<List<ActionType>> types, Optional<List<ActionStatus>> status,
      Optional<List<String>> workflows, Optional<List<String>> teams);

}
