package io.boomerang.v4.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.v4.model.Setting;
import io.boomerang.v4.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/apis/v2")
@Tag(name = "Boomerang Flow Management",
    description = "Read and update Boomerang Flow Settings")
public class ManagementV2Controller {
  
  @Autowired
  private SettingsService settingsService;
  
  @GetMapping(value = "/settings")
//  @AuthenticationScope(scopes = {TokenPermission.global})
  @Operation(summary = "Retrieve Boomerang Flow Settings")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public List<Setting> getAppConfiguration() {
    return settingsService.getAllSettings();
  }

  @PutMapping(value = "/settings")
//  @AuthenticationScope(scopes = {TokenPermission.global})
  @Operation(summary = "Update Boomerang Flow Settings")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public List<Setting> updateSettings(@RequestBody List<Setting> settings) {
    return settingsService.updateSettings(settings);
  }
}


