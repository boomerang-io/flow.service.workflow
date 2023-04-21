package io.boomerang.v4.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.v4.model.ref.ParamLayers;
import io.boomerang.v4.service.ParameterManager;
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

  @GetMapping(value = "/workflow/{workflowId}/paramlayers")
  public ParamLayers getParamLayers(
      @Parameter(name = "workflowId", description = "ID of the Workflow",
      required = true) @PathVariable String workflowId) {
    return parameterManager.buildParamLayers(workflowId);
  }
}
