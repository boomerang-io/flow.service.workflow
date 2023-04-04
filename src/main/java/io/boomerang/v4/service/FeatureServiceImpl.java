package io.boomerang.v4.service;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.boomerang.mongo.model.Config;
import io.boomerang.v4.model.FeaturesAndQuotas;

@Service
public class FeatureServiceImpl implements FeatureService {

  private static final String VERIFIED_TASK_EDIT_KEY = "enable.verified.tasks.edit";
  @Autowired
  private SettingsService settingsService;

  @Override
  public FeaturesAndQuotas getFlowFeatures() {
    FeaturesAndQuotas flowFeatures = new FeaturesAndQuotas();
    Map<String, Object> features = new HashMap<>();
    Map<String, Object> quotas = new HashMap<>();

    Config config = settingsService.getConfiguration("controller", "enable.tasks");

    if (config != null) {
      features.put(VERIFIED_TASK_EDIT_KEY, config.getBooleanValue());
    } else {
      features.put(VERIFIED_TASK_EDIT_KEY, false);
    }
    features.put("workflow.quotas",
        settingsService.getConfiguration("features", "workflowQuotas").getBooleanValue());
    features.put("workflow.triggers",
        settingsService.getConfiguration("features", "workflowTriggers").getBooleanValue());
    features.put("workflow.tokens",
        settingsService.getConfiguration("features", "workflowTokens").getBooleanValue());
    features.put("team.parameters",
        settingsService.getConfiguration("features", "teamParameters").getBooleanValue());
    features.put("global.parameters",
        settingsService.getConfiguration("features", "globalParameters").getBooleanValue());
    features.put("team.management",
        settingsService.getConfiguration("features", "teamManagement").getBooleanValue());
    features.put("user.management",
        settingsService.getConfiguration("features", "userManagement").getBooleanValue());
    features.put("activity",
        settingsService.getConfiguration("features", "activity").getBooleanValue());
    features.put("insights",
        settingsService.getConfiguration("features", "insights").getBooleanValue());
    features.put("team.tasks",
        settingsService.getConfiguration("features", "teamTasks").getBooleanValue());

    quotas.put("maxActivityStorageSize", settingsService
        .getConfiguration("activity", "max.storage.size").getValue().replace("Gi", ""));

    quotas.put("maxWorkflowStorageSize", settingsService
        .getConfiguration("workflow", "max.storage.size").getValue().replace("Gi", ""));

    flowFeatures.setFeatures(features);
    flowFeatures.setQuotas(quotas);
    return flowFeatures;
  }

}
