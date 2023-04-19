package io.boomerang.v4.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import io.boomerang.mongo.entity.ActivityEntity;
import io.boomerang.mongo.entity.RevisionEntity;
import io.boomerang.v4.data.model.TeamParameter;
import io.boomerang.v4.model.GlobalParam;
import io.boomerang.v4.model.enums.RelationshipRef;
import io.boomerang.v4.model.enums.RelationshipType;
import io.boomerang.v4.model.ref.ParamLayers;
import io.boomerang.v4.model.ref.ParamSpec;
import io.boomerang.v4.model.ref.Workflow;

/*
 * This is one half of the Param Layers. It collects the Global, Team, and Context Layers.
 * 
 * The Workflow and Task layers as well as Param resolution will be completed by the Engine
 */
@Service
public class ParameterManagerImpl implements ParameterManager {

  @Autowired
  private SettingsService settingsService;
  
  @Autowired
  private WorkflowService workflowService;

  @Autowired
  private TeamService teamService;

  @Autowired
  private GlobalParamService globalParamService;
  
  @Autowired
  private RelationshipService relationshipService;

  final String[] reserved = {"system", "workflow", "global", "team", "workflow"};
  
  @Override
  public List<String> buildParamKeys(String teamId, List<ParamSpec> workflowParamSpecs) {
    ParamLayers paramLayers = new ParamLayers();
    Map<String, Object> globalParams = paramLayers.getGlobalParams();
    Map<String, Object> teamParams = paramLayers.getTeamParams();
    Map<String, Object> workflowParams = paramLayers.getWorkflowParams();
    Map<String, Object> contextParams = paramLayers.getContextParams();
    //Set Global Params
    if (settingsService.getSetting("features", "globalParameters").getBooleanValue()) {
      buildGlobalParams(globalParams);
    }
    //Set Team Params
    if (settingsService.getSetting("features", "teamParameters").getBooleanValue()) {
      buildTeamParams(teamParams, teamId);
    }
    //Set the Keys from the Workflow - ignore values
    for (ParamSpec wfParam : workflowParamSpecs) {
      workflowParams.put("workflow.params." + wfParam.getName(), "");
    }
    //Set the available context Keys
    contextParams.put("workflowrun-trigger", "");
    contextParams.put("workflowrun-initiator", "");
    contextParams.put("workflow-name", "");
    contextParams.put("workflow-id", "");
    contextParams.put("workflow-version", "");
    contextParams.put("webhook-url", "");
    contextParams.put("wfe-url", "");
    contextParams.put("event-url", "");
    contextParams.put("task-name", "");
    contextParams.put("task-type", "");
    
    //TODO: add the available Workflow Tokens to the Context

    return paramLayers.getFlatKeys();
  }

  @Override
  public ParamLayers buildParamLayers(String wfRunId) {
    ParamLayers paramLayers = new ParamLayers();
    Map<String, Object> globalParams = paramLayers.getGlobalParams();
    Map<String, Object> teamParams = paramLayers.getTeamParams();
    Map<String, Object> contextParams = paramLayers.getContextParams();
    if (settingsService.getSetting("features", "globalParameters").getBooleanValue()) {
      buildGlobalParams(globalParams);
      
    }

    List<String> teamRefs = relationshipService.getFilteredRefs(Optional.of(RelationshipRef.WORKFLOWRUN),
        Optional.of(List.of(wfRunId)), Optional.of(RelationshipType.BELONGSTO), Optional.empty(), Optional.empty());
    if (!teamRefs.isEmpty()) {
      if (settingsService.getSetting("features", "teamParameters").getBooleanValue()) {
        buildTeamParams(teamParams, teamRefs.get(0));
      }
    }
    
    buildContextParams(contextParams, workflowRefs.get(0), wfRunId);

    return paramLayers;
  }

  /*
   * Build up global Params layer - defaultValue is not used with Global Params and can be ignored.
   */
  private void buildGlobalParams(Map<String, Object> globalParams) {
    List<GlobalParam> params = this.globalParamService.getAll();
    for (GlobalParam param : params) {
      if (param.getValue() != null) {
        globalParams.put(param.getKey(), param.getValue());
      }
    }
  }

  /*
   * Build up the Team Params - defaultValue is not used with Team Params and can be ignored.
   */
  private void buildTeamParams(Map<String, Object> teamParams, String teamId) {
    ResponseEntity<List<TeamParameter>> params = this.teamService.getParameters(teamId);
    if (params.getBody() != null) {
      for (TeamParameter param : params.getBody()) {
        teamParams.put(param.getKey(), param.getValue());
      }
    }
  }

  /*
   * Build up the reserved system Params
   * 
   * TODO: check this with the reserved Tekton ones
   */
  private void buildContextParams(Map<String, Object> contextParams, String workflowId, String wfRunId) {

    ResponseEntity<Workflow> workflowResponse = workflowService.get(workflowId, Optional.empty(), false);
    Workflow workflow = workflowResponse.getBody();
    if (activityId != null) {
      ActivityEntity activity = activityService.findWorkflowActivity(activityId);
      RevisionEntity revision =
          revisionService.getWorkflowlWithId(activity.getWorkflowRevisionid());

      if (revision != null) {
      }
      contextParams.put("workflowrun-trigger", activity.getTrigger());
      contextParams.put("workflowrun-initiator", "");
      if (activity.getInitiatedByUserId() != null) {
        contextParams.put("workflowrun-initiator", activity.getInitiatedByUserId());
      }
    }

    contextParams.put("workflow-name", workflow.getName());
    contextParams.put("workflow-id", workflow.getId());
    contextParams.put("workflow-version", workflow.getVersion());

    contextParams.put("webhook-url", this.settingsService.getWebhookURL());
    contextParams.put("wfe-url", this.settingsService.getWFEURL());
    contextParams.put("event-url", this.settingsService.getEventURL());


    if (task != null) {
      contextParams.put("task-name", task.getTaskName());
      contextParams.put("task-id", task.getTaskId());
      contextParams.put("task-type", task.getTaskType().toString());
    }
    
    // Add Tokens
    // TODO: use Relationship to get all the Tokens
//    WorkflowEntity workflow = workflowService.getWorkflow(workflowId);
//    if (workflow.getTokens() != null) {
//      for (WorkflowToken token : workflow.getTokens()) {
//        reservedProperties.put("context.tokens." + token.getLabel(), token.getToken());
//      }
//    }
  }
}
