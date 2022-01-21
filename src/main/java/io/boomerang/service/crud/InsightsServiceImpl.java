package io.boomerang.service.crud;

import static java.util.stream.Collectors.groupingBy;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.boomerang.model.Execution;
import io.boomerang.model.FlowActivity;
import io.boomerang.model.FlowExecutionRequest;
import io.boomerang.model.InsightsSummary;
import io.boomerang.model.ListActivityResponse;
import io.boomerang.model.Sort;
import io.boomerang.model.Task;
import io.boomerang.model.TaskExecutionResponse;
import io.boomerang.model.TaskOutputResult;
import io.boomerang.model.controller.TaskResult;
import io.boomerang.model.controller.TaskWorkspace;
import io.boomerang.model.teams.Action;
import io.boomerang.mongo.entity.ActivityEntity;
import io.boomerang.mongo.entity.FlowTaskTemplateEntity;
import io.boomerang.mongo.entity.FlowUserEntity;
import io.boomerang.mongo.entity.RevisionEntity;
import io.boomerang.mongo.entity.TaskExecutionEntity;
import io.boomerang.mongo.entity.TeamEntity;
import io.boomerang.mongo.entity.WorkflowEntity;
import io.boomerang.mongo.model.Dag;
import io.boomerang.mongo.model.ErrorResponse;
import io.boomerang.mongo.model.FlowTriggerEnum;
import io.boomerang.mongo.model.KeyValuePair;
import io.boomerang.mongo.model.Revision;
import io.boomerang.mongo.model.TaskStatus;
import io.boomerang.mongo.model.TaskTemplateConfig;
import io.boomerang.mongo.model.TaskType;
import io.boomerang.mongo.model.UserType;
import io.boomerang.mongo.model.WorkflowScope;
import io.boomerang.mongo.model.next.DAGTask;
import io.boomerang.mongo.service.ActivityTaskService;
import io.boomerang.mongo.service.FlowSettingsService;
import io.boomerang.mongo.service.FlowTaskTemplateService;
import io.boomerang.mongo.service.FlowTeamService;
import io.boomerang.mongo.service.FlowWorkflowActivityService;
import io.boomerang.mongo.service.FlowWorkflowService;
import io.boomerang.mongo.service.RevisionService;
import io.boomerang.service.ActionService;
import io.boomerang.service.PropertyManager;
import io.boomerang.service.UserIdentityService;
import io.boomerang.service.refactor.ControllerRequestProperties;
import io.boomerang.service.runner.misc.ControllerClient;
import io.boomerang.util.DateUtil;
import io.boomerang.util.ParameterMapper;

@Service
public class InsightsServiceImpl implements InsightsService {
  
  @Autowired
  private FlowWorkflowActivityService activitiesService;

  @Override
  public InsightsSummary getInsights(Optional<Date> from, Optional<Date> to,
      Pageable pageable, Optional<List<String>> workflowIds, Optional<List<String>> teamIds, Optional<List<String>> scopes) {

    final Page<ActivityEntity> records = activitiesService.getAllActivities(from, to, pageable,
        Optional.of(workflowIdsList), statuses, triggers);
    final InsightsSummary response = new InsightsSummary();
    final List<FlowActivity> activities = convert(records.getContent());
    List<Execution> executions = new ArrayList<>();
    Long totalExecutionTime = 0L;
    Long executionTime;

    for (FlowActivity activity : activities) {

      executionTime = activity.getDuration();

      if (executionTime != null) {
        totalExecutionTime = totalExecutionTime + executionTime;
      }

      addActivityDetail(teamId, executions, activity);
    }
    response.setTotalActivitiesExecuted(executions.size());
    response.setExecutions(executions);

    if (response.getTotalActivitiesExecuted() != 0) {
      response.setMedianExecutionTime(totalExecutionTime / executions.size());

    } else {
      response.setMedianExecutionTime(0L);
    }
    return response;
  }
}
