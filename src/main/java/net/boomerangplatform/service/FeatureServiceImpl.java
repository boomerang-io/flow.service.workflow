package net.boomerangplatform.service;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import net.boomerangplatform.model.FlowFeatures;
import net.boomerangplatform.mongo.model.Config;
import net.boomerangplatform.mongo.service.FlowSettingsService;

@Service
public class FeatureServiceImpl implements FeatureService {
  
  private static final String VERIFIED_TASK_EDIT_KEY = "enable.verified.tasks.edit";
  @Autowired
  private FlowSettingsService settingsService;

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
    
    flowFeatures.setFeatures(features);
    return flowFeatures;
  }

}
