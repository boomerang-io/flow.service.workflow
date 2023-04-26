package io.boomerang.service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import io.boomerang.model.FlowActivity;
import io.boomerang.model.FlowExecutionRequest;
import io.boomerang.model.ListActivityResponse;
import io.boomerang.model.TaskExecutionResponse;
import io.boomerang.model.controller.TaskWorkspace;
import io.boomerang.mongo.entity.ActivityEntity;
import io.boomerang.mongo.entity.TaskExecutionEntity;
import io.boomerang.mongo.model.ErrorResponse;
import io.boomerang.mongo.model.KeyValuePair;
import io.boomerang.v4.data.entity.UserEntity;

public interface FlowActivityService {

    ActivityEntity createFlowActivity(String workflowVersionId, Optional<String> trigger,
            FlowExecutionRequest request, Optional<List<TaskWorkspace>> taskWorkspaces,
            List<KeyValuePair> list);

    ActivityEntity findWorkflowActivity(String id);

    List<FlowActivity> findActivity(Pageable pageable, Optional<String> labels);

    ListActivityResponse getAllActivities(Optional<Date> from, Optional<Date> to, Pageable page,
            Optional<List<String>> workflowIds, Optional<List<String>> teamIds,
            Optional<List<String>> statuses, Optional<List<String>> triggers,
            Optional<List<String>> scopes, String property, Direction direction);

    ListActivityResponse getAllActivitiesForUser(UserEntity user, Optional<Date> from,
            Optional<Date> to, Pageable page, String property, Direction direction);

    public List<TaskExecutionResponse> getTaskExecutions(String activityId);

    TaskExecutionEntity saveTaskExecution(TaskExecutionEntity task);

    StreamingResponseBody getTaskLog(String activityId, String taskId);

    Map<String, Long> getActivitySummary(Pageable pageable, Optional<List<String>> teamIds,
            List<String> triggers, Optional<List<String>> workflowIds,
            Optional<List<String>> scopes, Long fromDate, Long toDate);

    void cancelWorkflowActivity(String activityId, ErrorResponse error);

    public boolean hasExceededExecutionQuotas(String activityId);
}
