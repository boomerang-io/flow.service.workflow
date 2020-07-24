package net.boomerangplatform.mongo.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import net.boomerangplatform.mongo.entity.FlowWorkflowEntity;

public interface FlowWorkflowRepository extends MongoRepository<FlowWorkflowEntity, String> {

  List<FlowWorkflowEntity> findByFlowTeamId(String flowTeamId);

  List<FlowWorkflowEntity> findByFlowTeamIdIn(List<String> flowTeamIds);

  @Query("{ 'triggers.webhook.token' : ?0 }")
  FlowWorkflowEntity findByToken(String tokenString);

  @Query("{ 'triggers.scheduler.enable' : true }")
  List<FlowWorkflowEntity> findAllScheduledWorkflows();

  @Query("{ 'triggers.event.enable' : true }")
  List<FlowWorkflowEntity> findAllEventWorkflows();

  @Query("{ 'triggers.event.enable' : true, 'triggers.event.topic' : ?0 }")
  List<FlowWorkflowEntity> findAllEventWorkflowsForTopic(String topic);
}
