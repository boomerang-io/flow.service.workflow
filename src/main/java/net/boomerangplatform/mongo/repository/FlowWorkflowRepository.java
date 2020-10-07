package net.boomerangplatform.mongo.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import net.boomerangplatform.mongo.entity.WorkflowEntity;

public interface FlowWorkflowRepository extends MongoRepository<WorkflowEntity, String> {

  List<WorkflowEntity> findByFlowTeamId(String flowTeamId);

  List<WorkflowEntity> findByFlowTeamIdIn(List<String> flowTeamIds);

  @Query("{ 'tokens.token' : ?0 }")
  WorkflowEntity findByToken(String tokenString);

  @Query("{ 'triggers.scheduler.enable' : true }")
  List<WorkflowEntity> findAllScheduledWorkflows();

  @Query("{ 'triggers.event.enable' : true }")
  List<WorkflowEntity> findAllEventWorkflows();

  @Query("{ 'triggers.event.enable' : true, 'triggers.event.topic' : ?0 }")
  List<WorkflowEntity> findAllEventWorkflowsForTopic(String topic);
}
