package net.boomerangplatform.mongo.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import net.boomerangplatform.mongo.entity.FlowWorkflowActivityEntity;
import net.boomerangplatform.mongo.model.FlowTaskStatus;

public interface FlowWorkflowActivityService {

  Page<FlowWorkflowActivityEntity> findAllActivities(Optional<Date> from, Optional<Date> to,
      Pageable page, Optional<String> query);

  Page<FlowWorkflowActivityEntity> findAllActivities(Optional<Date> from, Optional<Date> to,
      Pageable page);

  Page<FlowWorkflowActivityEntity> findAllActivitiesForWorkflows(Optional<Date> from,
      Optional<Date> to, List<String> workflows, Pageable page);

  FlowWorkflowActivityEntity findWorkflowActiivtyById(String id);


  FlowWorkflowActivityEntity findByWorkflowAndProperty(String workflowId, String key, String value);

  FlowWorkflowActivityEntity saveWorkflowActivity(FlowWorkflowActivityEntity entity);

  Page<FlowWorkflowActivityEntity> getAllActivites(Optional<Date> from, Optional<Date> to,
      Pageable page, Optional<List<String>> workflowIds, Optional<List<String>> statuses,
      Optional<List<String>> triggers);
  
  Page<FlowWorkflowActivityEntity> findbyWorkflowIdsAndStatus(List<String> workflowIds, FlowTaskStatus status);

}
