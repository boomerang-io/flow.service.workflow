package io.boomerang.mongo.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import io.boomerang.mongo.entity.ActivityEntity;
import io.boomerang.mongo.model.TaskStatus;

public interface FlowWorkflowActivityService {

  Page<ActivityEntity> findAllActivities(Optional<Date> from, Optional<Date> to,
      Pageable page, Optional<String> query);

  Page<ActivityEntity> findAllActivities(Optional<Date> from, Optional<Date> to,
      Pageable page);

  Page<ActivityEntity> findAllActivitiesForWorkflows(Optional<Date> from,
      Optional<Date> to, List<String> workflows, Pageable page);

  ActivityEntity findWorkflowActivtyById(String id);


  ActivityEntity findByWorkflowAndProperty(String workflowId, String key, String value);

  ActivityEntity saveWorkflowActivity(ActivityEntity entity);

  Page<ActivityEntity> getAllActivities(Optional<Date> from, Optional<Date> to,
      Pageable page, Optional<List<String>> workflowIds, Optional<List<String>> statuses,
      Optional<List<String>> triggers);
  
  List<ActivityEntity> findbyWorkflowIdsAndStatus(List<String> workflowIds, TaskStatus status);

  Page<ActivityEntity> findAllActivitiesForTeam(Optional<Date> fromDate, Optional<Date> toDate,
      String teamId, Pageable page);
  
  Page<ActivityEntity> findAllActivitiesForUser(Optional<Date> fromDate, Optional<Date> toDate,
      String userId, Pageable page);

}
