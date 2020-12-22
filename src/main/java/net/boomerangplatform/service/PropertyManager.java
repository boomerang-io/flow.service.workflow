package net.boomerangplatform.service;

import java.util.Map;
import net.boomerangplatform.model.Task;
import net.boomerangplatform.service.refactor.ControllerRequestProperties;

public interface PropertyManager {

  ControllerRequestProperties buildRequestPropertyLayering(Task task, String activityId);

  String replaceValueWithProperty(String value, String activityId,
      Map<String, String> executionProperties);

}
