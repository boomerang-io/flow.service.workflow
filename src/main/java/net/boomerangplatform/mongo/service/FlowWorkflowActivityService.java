package net.boomerangplatform.mongo.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import net.boomerangplatform.mongo.entity.ActivityEntity;
import net.boomerangplatform.mongo.model.TaskStatus;

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

  Page<ActivityEntity> getAllActivites(Optional<Date> from, Optional<Date> to,
      Pageable page, Optional<List<String>> workflowIds, Optional<List<String>> statuses,
      Optional<List<String>> triggers);
  
  List<ActivityEntity> findbyWorkflowIdsAndStatus(List<String> workflowIds, TaskStatus status);

}
