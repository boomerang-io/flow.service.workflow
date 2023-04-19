package io.boomerang.controller.api;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.mongo.model.TokenScope;
import io.boomerang.security.interceptors.AuthenticationScope;
import io.boomerang.service.crud.ConfigurationService;
import io.boomerang.v4.model.Settings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/apis/v1")
@Tag(name = "Boomerang Flow Management",
    description = "Read and update Boomerang Flow Settings")
public class FlowManagementV1Controller {
  
  @Autowired
  private ConfigurationService configurationService;
  
  @GetMapping(value = "/flow/settings")
  @AuthenticationScope(scopes = {TokenScope.global})
  @Operation(summary = "Retrieve Boomerang Flow Settings")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public List<Settings> getAppConfiguration() {
    return configurationService.getAllSettings();
  }

  @PutMapping(value = "/flow/settings")
  @AuthenticationScope(scopes = {TokenScope.global})
  @Operation(summary = "Update Boomerang Flow Settings")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public List<Settings> updateSettings(@RequestBody List<Settings> settings) {
    return configurationService.updateSettings(settings);
  }
}


