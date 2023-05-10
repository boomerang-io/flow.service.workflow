package io.boomerang.v4.service;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.boomerang.v4.model.AbstractParam;
import io.boomerang.v4.model.FeaturesAndQuotas;

@Service
public class FeatureServiceImpl implements FeatureService {

  private static final String VERIFIED_TASK_EDIT_KEY = "enable.verified.tasks.edit";
  @Autowired
  private SettingsService settingsService;

  @Override
  public FeaturesAndQuotas get() {
    FeaturesAndQuotas flowFeatures = new FeaturesAndQuotas();
    Map<String, Object> features = new HashMap<>();
    Map<String, Object> quotas = new HashMap<>();

    AbstractParam config = settingsService.getSettingConfig("controller", "enable.tasks");

    if (config != null) {
      features.put(VERIFIED_TASK_EDIT_KEY, config.getBooleanValue());
    } else {
      features.put(VERIFIED_TASK_EDIT_KEY, false);
    }
    features.put("workflow.quotas",
        settingsService.getSettingConfig("features", "workflowQuotas").getBooleanValue());
    features.put("workflow.triggers",
        settingsService.getSettingConfig("features", "workflowTriggers").getBooleanValue());
    features.put("workflow.tokens",
        settingsService.getSettingConfig("features", "workflowTokens").getBooleanValue());
    features.put("team.parameters",
        settingsService.getSettingConfig("features", "teamParameters").getBooleanValue());
    features.put("global.parameters",
        settingsService.getSettingConfig("features", "globalParameters").getBooleanValue());
    features.put("team.management",
        settingsService.getSettingConfig("features", "teamManagement").getBooleanValue());
    features.put("user.management",
        settingsService.getSettingConfig("features", "userManagement").getBooleanValue());
    features.put("activity",
        settingsService.getSettingConfig("features", "activity").getBooleanValue());
    features.put("insights",
        settingsService.getSettingConfig("features", "insights").getBooleanValue());
    features.put("team.tasks",
        settingsService.getSettingConfig("features", "teamTasks").getBooleanValue());

    quotas.put("maxActivityStorageSize", settingsService
        .getSettingConfig("activity", "max.storage.size").getValue().replace("Gi", ""));

    quotas.put("maxWorkflowStorageSize", settingsService
        .getSettingConfig("workflow", "max.storage.size").getValue().replace("Gi", ""));

    flowFeatures.setFeatures(features);
    flowFeatures.setQuotas(quotas);
    return flowFeatures;
  }

}
