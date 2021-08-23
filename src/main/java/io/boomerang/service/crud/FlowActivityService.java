package io.boomerang.service.crud;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import io.boomerang.model.FlowActivity;
import io.boomerang.model.FlowExecutionRequest;
import io.boomerang.model.InsightsSummary;
import io.boomerang.model.ListActivityResponse;
import io.boomerang.model.TaskExecutionResponse;
import io.boomerang.model.controller.TaskWorkspace;
import io.boomerang.mongo.entity.ActivityEntity;
import io.boomerang.mongo.entity.FlowUserEntity;
import io.boomerang.mongo.entity.TaskExecutionEntity;
import io.boomerang.mongo.model.KeyValuePair;

public interface FlowActivityService {

  ActivityEntity createFlowActivity(String workflowVersionId,
      Optional<String> trigger, FlowExecutionRequest request, Optional<List<TaskWorkspace>> taskWorkspaces, List<KeyValuePair> list);

  ActivityEntity findWorkflowActivity(String id);

  List<FlowActivity> findActivty(Pageable pageable,Optional<String> labels);
  
  ListActivityResponse getAllActivites(Optional<Date> from, Optional<Date> to, Pageable page,
      Optional<List<String>> workflowIds, Optional<List<String>> teamIds,
      Optional<List<String>> statuses, Optional<List<String>> triggers, Optional<List<String>> scopes, String property, Direction direction);

  ListActivityResponse getAllActivitesForUser(FlowUserEntity user, Optional<Date> from,
      Optional<Date> to, Pageable page, String property, Direction direction);
  
  public List<TaskExecutionResponse> getTaskExecutions(String activityId);
  
  TaskExecutionEntity saveTaskExecution(TaskExecutionEntity task);

  InsightsSummary getInsightsSummary(Optional<Date> from, Optional<Date> to, Pageable pageable,
      Optional<String> teamId);

  StreamingResponseBody getTaskLog(String activityId, String taskId);

  Map<String, Long> getActivitySummary(Pageable pageable, Optional<List<String>> teamIds,
      List<String> triggers, Optional<List<String>> workflowIds,Optional<List<String>> scopes, Long fromDate, Long toDate);

  void cancelWorkflowActivity(String activityId);


  public boolean hasExceededExecutionQuotas(String activityId);
}
