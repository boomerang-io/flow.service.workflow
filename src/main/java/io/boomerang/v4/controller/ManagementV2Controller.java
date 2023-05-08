package io.boomerang.v4.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.security.interceptors.AuthScope;
import io.boomerang.security.model.TokenAccess;
import io.boomerang.security.model.TokenObject;
import io.boomerang.security.model.TokenType;
import io.boomerang.security.service.IdentityService;
import io.boomerang.v4.model.OneTimeCode;
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

  @Autowired
  private IdentityService identityService;
  
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
  
  //TODO move this to another location
  @PutMapping(value = "/register")
  @AuthScope(access = TokenAccess.any, object = TokenObject.user, types = {TokenType.session})
  public ResponseEntity<Boolean> register(@RequestBody(required = false) OneTimeCode otc) {
    return identityService.activateSetup(otc);
  }
}


