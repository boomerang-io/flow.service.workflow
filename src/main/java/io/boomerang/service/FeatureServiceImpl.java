package io.boomerang.service;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import io.boomerang.model.FlowFeatures;
import io.boomerang.mongo.model.Config;
import io.boomerang.mongo.service.FlowSettingsService;

@Service
public class FeatureServiceImpl implements FeatureService {
  
  private static final String VERIFIED_TASK_EDIT_KEY = "enable.verified.tasks.edit";
  @Autowired
  private FlowSettingsService settingsService;
  
  @Value("${flow.feature.workflow.quotas}")
  private boolean flowFeatureWorkflowQuotas;
  
  @Value("${flow.feature.workflow.triggers}")
  private boolean flowFeatureWorkflowTriggers;
  
  @Value("${flow.feature.workflow.tokens}")
  private boolean flowFeatureWorkflowTokens;
  
  @Value("${flow.feature.team.parameters}")
  private boolean flowFeatureTeamParameters;
  
  @Value("${flow.feature.global.parameters}")
  private boolean flowFeatureGlobalParameters;
  
  @Value("${flow.feature.team.management}")
  private boolean flowFeatureTeamManagement;
  
  @Value("${flow.feature.user.management}")
  private boolean flowFeatureUserManagement;
  
  @Value("${flow.feature.taskManager}")
  private boolean flowFeatureTaskManager;
  
  @Value("${flow.feature.settings}")
  private boolean flowFeatureSettings;
  
  @Value("${flow.feature.activity}")
  private boolean flowFeatureActivity;
  
  @Value("${flow.feature.insights}")
  private boolean flowFeatureInsights;
  

  @Override
  public FlowFeatures getFlowFeatures() {
    FlowFeatures flowFeatures = new FlowFeatures();
    Map<String, Object> features = new HashMap<>();
    
    Config config = settingsService.getConfiguration("controller", "enable.tasks");
    
    if (config != null) {
      features.put(VERIFIED_TASK_EDIT_KEY, config.getBooleanValue());
    }
    else {
      features.put(VERIFIED_TASK_EDIT_KEY, false);
    }
    features.put("workflow.quotas", flowFeatureWorkflowQuotas);
    features.put("workflow.triggers", flowFeatureWorkflowTriggers);
    features.put("workflow.tokens", flowFeatureWorkflowTokens);
    features.put("team.parameters", flowFeatureTeamParameters);
    features.put("global.parameters", flowFeatureGlobalParameters);
    features.put("team.management", flowFeatureTeamManagement);
    features.put("user.management", flowFeatureUserManagement);
    features.put("taskManager", flowFeatureTaskManager);
    features.put("settings", flowFeatureSettings);
    features.put("activity",flowFeatureActivity);
    features.put("insights",flowFeatureInsights);
    
    flowFeatures.setFeatures(features); 
    return flowFeatures;
  }

}
