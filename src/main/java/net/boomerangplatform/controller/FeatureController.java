package net.boomerangplatform.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import net.boomerangplatform.model.FlowFeatures;
import net.boomerangplatform.service.FeatureService;

@RestController
@RequestMapping("/workflow")
public class FeatureController {

  @Autowired
  private FeatureService featureService;
  
  @GetMapping(value = "/features")
  public FlowFeatures getFlowFeatures() {
    return featureService.getFlowFeatures();
  }
}
