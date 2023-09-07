package io.boomerang.controller;

import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.service.ParameterManager;
import io.boomerang.service.SettingsService;
import io.boomerang.v4.data.entity.SettingEntity;
import io.boomerang.v4.model.AbstractParam;
import io.boomerang.v4.model.ref.ParamLayers;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Parameter;

/*
 * Endpoints in this controller are unauthenticated and only meant to be used by the Engine
 */
@RestController
@RequestMapping("/internal")
@Hidden
public class InternalController {
  
  @Autowired
  private ParameterManager parameterManager;
  
  @Autowired
  private SettingsService settingsService;

  @GetMapping(value = "/workflow/{workflowId}/paramlayers")
  public ParamLayers getParamLayers(
      @Parameter(name = "workflowId", description = "ID of the Workflow",
      required = true) @PathVariable String workflowId) {
    return parameterManager.buildParamLayers(workflowId);
  }
  
  @GetMapping(value = "/settings/{key}")
  public ResponseEntity<Map<String, String>> getTaskSettings(
      @Parameter(name = "key", description = "Key of the Settings collection",
      required = true) @PathVariable String key) {
    try {
      SettingEntity settings = settingsService.getSettingByKey(key);
      return ResponseEntity.ok(settings.getConfig().stream().collect(Collectors.toMap(AbstractParam::getKey, AbstractParam::getValue)));
    } catch (Exception e) {
      return ResponseEntity.noContent().build();
    }
  }
}
