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

    Config config = settingsService.getSetting("controller", "enable.tasks");

    if (config != null) {
      features.put(VERIFIED_TASK_EDIT_KEY, config.getBooleanValue());
    } else {
      features.put(VERIFIED_TASK_EDIT_KEY, false);
    }
    features.put("workflow.quotas",
        settingsService.getSetting("features", "workflowQuotas").getBooleanValue());
    features.put("workflow.triggers",
        settingsService.getSetting("features", "workflowTriggers").getBooleanValue());
    features.put("workflow.tokens",
        settingsService.getSetting("features", "workflowTokens").getBooleanValue());
    features.put("team.parameters",
        settingsService.getSetting("features", "teamParameters").getBooleanValue());
    features.put("global.parameters",
        settingsService.getSetting("features", "globalParameters").getBooleanValue());
    features.put("team.management",
        settingsService.getSetting("features", "teamManagement").getBooleanValue());
    features.put("user.management",
        settingsService.getSetting("features", "userManagement").getBooleanValue());
    features.put("activity",
        settingsService.getSetting("features", "activity").getBooleanValue());
    features.put("insights",
        settingsService.getSetting("features", "insights").getBooleanValue());
    features.put("team.tasks",
        settingsService.getSetting("features", "teamTasks").getBooleanValue());

    quotas.put("maxActivityStorageSize", settingsService
        .getSetting("activity", "max.storage.size").getValue().replace("Gi", ""));

    quotas.put("maxWorkflowStorageSize", settingsService
        .getSetting("workflow", "max.storage.size").getValue().replace("Gi", ""));

    flowFeatures.setFeatures(features);
    flowFeatures.setQuotas(quotas);
    return flowFeatures;
  }

}
