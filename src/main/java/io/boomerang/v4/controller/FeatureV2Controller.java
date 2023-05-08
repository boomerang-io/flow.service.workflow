package io.boomerang.v4.controller;

import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.v4.model.FeaturesAndQuotas;
import io.boomerang.v4.service.FeatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/*
 * TODO: determine if the features should go on a User endpoint or System endpoint
 */
@RestController
@RequestMapping("/api/v2/features")
@Tag(name = "Features Management")
public class FeatureV2Controller {

  @Autowired
  private FeatureService featureService;
  
  @GetMapping(value = "/")
  @Operation(summary = "Retrieve feature flags.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<FeaturesAndQuotas> getFlowFeatures() {
    CacheControl cacheControl = CacheControl.maxAge(1, TimeUnit.HOURS);
    return ResponseEntity.ok().cacheControl(cacheControl).body(featureService.get());
  }
}
