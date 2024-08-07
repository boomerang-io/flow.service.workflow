package io.boomerang.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.boomerang.data.entity.TeamEntity;
import io.boomerang.data.repository.TeamRepository;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.model.AbstractParam;
import io.boomerang.model.ref.ParamLayers;
import io.boomerang.model.ref.ParamSpec;
import io.boomerang.model.ref.Workflow;
import io.boomerang.security.entity.TokenEntity;
import io.boomerang.security.model.AuthType;
import io.boomerang.security.repository.TokenRepository;

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
  private TeamRepository teamRepository;

  @Autowired
  private GlobalParamServiceImpl globalParamService;

  @Autowired
  private TokenRepository tokenRepository;

  final String[] reserved = {"system", "workflow", "global", "team", "workflow"};
  
  /*
   * Used by the /available-params endpoint to retrieve all param keys workflow and above in stack
   */
  @Override
  public List<String> buildParamKeys(String teamId, Workflow workflow) {
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
    for (ParamSpec wfParam : workflow.getParams()) {
      workflowParams.put(wfParam.getName(), "");
    }
    buildContextParams(contextParams, workflow);

    return paramLayers.getFlatKeys();
  }
  
  /*
   * Only needs to set the Global, Team, and partial Context Params. Engine will add and resolve.
   */
  @Override
  public ParamLayers buildParamLayers(String teamId, Workflow workflow) {
    ParamLayers paramLayers = new ParamLayers();
    Map<String, Object> teamParams = paramLayers.getTeamParams();
    Map<String, Object> globalParams = paramLayers.getGlobalParams();
    Map<String, Object> contextParams = paramLayers.getContextParams();
    //Set Team Params
    if (settingsService.getSettingConfig("features", "teamParameters").getBooleanValue()) {
        buildTeamParams(teamParams, teamId);
    }
    //Set Global Params
    if (settingsService.getSettingConfig("features", "globalParameters").getBooleanValue()) {
      buildGlobalParams(globalParams);
    }
    buildContextParams(contextParams, workflow);
    
    return paramLayers;
  }

  /*
   * Build up global Params layer - defaultValue is not used with Global Params and can be ignored.
   */
  private void buildGlobalParams(Map<String, Object> globalParams) {
    List<AbstractParam> params = this.globalParamService.getAllUnfiltered();
    for (AbstractParam param : params) {
      if (param.getValue() != null) {
        globalParams.put(param.getKey(), param.getValue());
      }
    }
  }

  /*
   * Build up the Team Params - defaultValue is not used with Team Params and can be ignored.
   */
  private void buildTeamParams(Map<String, Object> teamParams, String team) {
      Optional<TeamEntity> optTeamEntity = teamRepository.findByNameIgnoreCase(team);
      if (!optTeamEntity.isPresent()) {
        throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
      }
      TeamEntity teamEntity = optTeamEntity.get();
    if (teamEntity.getParameters() != null && !teamEntity.getParameters().isEmpty()) {
      for (AbstractParam param : teamEntity.getParameters()) {
        teamParams.put(param.getKey(), param.getValue());
      }
    }
  }

  /*
   * Build up the reserved system Params
   * 
   * TODO: check this with the reserved Tekton ones
   */
  private void buildContextParams(Map<String, Object> contextParams, Workflow workflow) {
    contextParams.put("workflowrun-trigger", "");
    contextParams.put("workflowrun-initiator", "");
    contextParams.put("workflowrun-id", "");
    contextParams.put("workflow-name", workflow.getName());
    contextParams.put("workflow-id", workflow.getId());
    contextParams.put("workflow-version", workflow.getVersion());
    contextParams.put("taskrun-id", "");
    contextParams.put("taskrun-name", "");
    contextParams.put("taskrun-type", "");
    contextParams.put("webhook-url", this.settingsService.getWebhookURL());
    contextParams.put("wfe-url", this.settingsService.getWFEURL());
    contextParams.put("event-url", this.settingsService.getEventURL());
    
    Optional<List<TokenEntity>> tokens = tokenRepository.findByPrincipalAndType(workflow.getId(), AuthType.workflow);
    // Add Tokens
    if (tokens.isPresent() && !tokens.isEmpty()) {
      for (TokenEntity t : tokens.get()) {
        contextParams.put("tokens." + t.getName(), t.getToken());
      }
    }
  }
}
