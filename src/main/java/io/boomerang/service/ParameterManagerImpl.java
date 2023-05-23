package io.boomerang.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import io.boomerang.v4.data.entity.RelationshipEntity;
import io.boomerang.v4.model.AbstractParam;
import io.boomerang.v4.model.GlobalParam;
import io.boomerang.v4.model.enums.RelationshipRef;
import io.boomerang.v4.model.enums.RelationshipType;
import io.boomerang.v4.model.ref.ParamLayers;
import io.boomerang.v4.model.ref.ParamSpec;

/*
 * This is one half of the Param Layers. It collects the Global, Team, and Context Layers.
 * 
 * The Workflow and Task layers as well as Param resolution will be completed by the Engine
 * 
 * CAUTION: this is tightly coupled with Engine 
 */
@Service
public class ParameterManagerImpl implements ParameterManager {

  @Autowired
  private SettingsService settingsService;

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
    if (settingsService.getSettingConfig("features", "globalParameters").getBooleanValue()) {
      buildGlobalParams(globalParams);
    }
    //Set Team Params
    if (settingsService.getSettingConfig("features", "teamParameters").getBooleanValue()) {
      buildTeamParams(teamParams, teamId);
    }
    //Set the Keys from the Workflow - ignore values
    for (ParamSpec wfParam : workflowParamSpecs) {
      workflowParams.put("workflow.params." + wfParam.getName(), "");
    }
    buildContextParams(contextParams);

    return paramLayers.getFlatKeys();
  }
  
  /*
   * Only needs to set the Global, Team, and partial Context Params. Engine will add and resolve.
   */
  @Override
  public ParamLayers buildParamLayers(String workflowId) {
    ParamLayers paramLayers = new ParamLayers();
    Map<String, Object> globalParams = paramLayers.getGlobalParams();
    Map<String, Object> teamParams = paramLayers.getTeamParams();
    Map<String, Object> contextParams = paramLayers.getContextParams();
    //Set Global Params
    if (settingsService.getSettingConfig("features", "globalParameters").getBooleanValue()) {
      buildGlobalParams(globalParams);
    }
    //Set Team Params
    if (settingsService.getSettingConfig("features", "teamParameters").getBooleanValue()) {
      Optional<RelationshipEntity> rel = relationshipService.getRelationship(RelationshipRef.WORKFLOW, workflowId, RelationshipType.BELONGSTO);
      if (rel.isEmpty()) {
        buildTeamParams(teamParams, rel.get().getToRef());
      }
    }
    buildContextParams(contextParams);

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
    ResponseEntity<List<AbstractParam>> params = this.teamService.getParameters(teamId);
    if (params.getBody() != null) {
      for (AbstractParam param : params.getBody()) {
        teamParams.put(param.getKey(), param.getValue());
      }
    }
  }

  /*
   * Build up the reserved system Params
   * 
   * TODO: check this with the reserved Tekton ones
   */
  private void buildContextParams(Map<String, Object> contextParams) {
    contextParams.put("workflowrun-trigger", "");
    contextParams.put("workflowrun-initiator", "");
    contextParams.put("workflowrun-id", "");
    contextParams.put("workflow-name", "");
    contextParams.put("workflow-id", "");
    contextParams.put("workflow-version", "");
    contextParams.put("taskrun-id", "");
    contextParams.put("taskrun-name", "");
    contextParams.put("taskrun-type", "");
    contextParams.put("webhook-url", this.settingsService.getWebhookURL());
    contextParams.put("wfe-url", this.settingsService.getWFEURL());
    contextParams.put("event-url", this.settingsService.getEventURL());
    
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
