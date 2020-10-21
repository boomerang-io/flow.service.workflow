package net.boomerangplatform.service.crud;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import net.boomerangplatform.model.FlowExecutionRequest;
import net.boomerangplatform.model.InsightsSummary;
import net.boomerangplatform.model.ListActivityResponse;
import net.boomerangplatform.mongo.entity.ActivityEntity;
import net.boomerangplatform.mongo.entity.FlowUserEntity;
import net.boomerangplatform.mongo.entity.TaskExecutionEntity;
import net.boomerangplatform.mongo.model.FlowTriggerEnum;

public interface FlowActivityService {

  ActivityEntity createFlowActivity(String workflowVersionId,
      Optional<String> trigger, FlowExecutionRequest request);

  ActivityEntity findWorkflowActivity(String id);

  ListActivityResponse getAllActivites(Optional<Date> from, Optional<Date> to, Pageable page,
      Optional<List<String>> workflowIds, Optional<List<String>> teamIds,
      Optional<List<String>> statuses, Optional<List<String>> triggers, String property, Direction direction);

  ListActivityResponse getAllActivitesForUser(FlowUserEntity user, Optional<Date> from,
      Optional<Date> to, Pageable page, String property, Direction direction);
  
  List<TaskExecutionEntity> getTaskExecutions(String activityId);

  TaskExecutionEntity saveTaskExecution(TaskExecutionEntity task);

  InsightsSummary getInsightsSummary(Optional<Date> from, Optional<Date> to, Pageable pageable,
      Optional<String> teamId);

  StreamingResponseBody getTaskLog(String activityId, String taskId);

  Map<String, Long> getActivitySummary(Pageable pageable, List<String> teamIds,
      List<String> triggers, Long fromDate, Long toDate);


}
