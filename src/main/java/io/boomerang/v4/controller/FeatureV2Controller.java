package io.boomerang.v4.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.v4.model.FeaturesAndQuotas;
import io.boomerang.v4.service.FeatureService;

/*
 * TODO: determine if the features should go on a User endpoint or System endpoint
 */
@RestController
@RequestMapping("/api/v2/features")
public class FeatureV2Controller {

  @Autowired
  private FeatureService featureService;
  
  @GetMapping(value = "/")
  public FeaturesAndQuotas getFlowFeatures() {
    return featureService.get();
  }
}
