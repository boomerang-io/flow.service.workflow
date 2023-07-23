package io.boomerang.controller;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.security.interceptors.AuthScope;
import io.boomerang.security.model.TokenAccess;
import io.boomerang.security.model.TokenObject;
import io.boomerang.security.model.TokenScope;
import io.boomerang.security.service.IdentityService;
import io.boomerang.service.ContextService;
import io.boomerang.service.FeatureService;
import io.boomerang.service.GlobalParamService;
import io.boomerang.service.NavigationService;
import io.boomerang.service.SettingsService;
import io.boomerang.v4.model.FeaturesAndQuotas;
import io.boomerang.v4.model.GlobalParam;
import io.boomerang.v4.model.HeaderNavigationResponse;
import io.boomerang.v4.model.Navigation;
import io.boomerang.v4.model.OneTimeCode;
import io.boomerang.v4.model.Setting;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v2")
@Tag(name = "Instance Management",
    description = "Register the instance, retrieve context and navigation, and manage Settings and Global Parameters")
public class ManagementV2Controller {
  
  @Autowired
  private SettingsService settingsService;

  @Autowired
  private IdentityService identityService;
  
  @Autowired
  private GlobalParamService paramService;

  @Autowired
  NavigationService navigationService;

  @Autowired
  private ContextService contextService;

  @Autowired
  private FeatureService featureService;
  
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
  @PutMapping(value = "/activate")
  @AuthScope(access = TokenAccess.action, object = TokenObject.instance, types = {TokenScope.session, TokenScope.global})
  @Operation(summary = "Register and activate an installation of Flow")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<Boolean> register(@RequestBody(required = false) OneTimeCode otc) {
    return identityService.activateSetup(otc);
  }

  @GetMapping(value = "/context")
  @Operation(summary = "Retrieve this instances context, features, and navigation.")
  @AuthScope(access = TokenAccess.read, object = TokenObject.instance, types = {TokenScope.session, TokenScope.global})
  public HeaderNavigationResponse getHeaderNavigation() {
    return this.contextService.getHeaderNavigation(identityService.isCurrentUserAdmin());
  }
  
  @GetMapping(value = "/features")
  @Operation(summary = "Retrieve feature flags.")
  @AuthScope(access = TokenAccess.read, object = TokenObject.instance, types = {TokenScope.session, TokenScope.global})
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<FeaturesAndQuotas> getFlowFeatures() {
    CacheControl cacheControl = CacheControl.maxAge(5, TimeUnit.MINUTES);
    return ResponseEntity.ok().cacheControl(cacheControl).body(featureService.get());
  }

  @GetMapping(value = "/navigation")
  @Operation(summary = "Retrieve navigation.")
  @AuthScope(access = TokenAccess.read, object = TokenObject.instance, types = {TokenScope.session, TokenScope.global})
  public ResponseEntity<List<Navigation>> getNavigation(@Parameter(name = "teamId", description = "The id of the Team that the user is currently on", example = "123143412312310",
      required = false) @RequestParam(required = false) Optional<String> teamId) {
    List<Navigation> response = navigationService.getNavigation(identityService.isCurrentUserAdmin(), teamId);
    
    CacheControl cacheControl = CacheControl.maxAge(1, TimeUnit.HOURS);

    return ResponseEntity.ok().cacheControl(cacheControl).body(response);
  }

  @GetMapping(value = "/global-params")
  @AuthScope(types = {TokenScope.global}, access = TokenAccess.read, object = TokenObject.parameter)
  @Operation(summary = "Get all global Params")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public List<GlobalParam> getAll() {
    return paramService.getAll();
  }

  @PostMapping(value = "/global-params")
//  @AuthenticationScope(scopes = {TokenPermission.global})
  @Operation(summary = "Create new global Param")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public GlobalParam create(@RequestBody GlobalParam request) {
    return paramService.create(request);
  }

  @PutMapping(value = "/global-params")
  public GlobalParam update(@RequestBody GlobalParam request) {
    return paramService.update(request);
  }

  @DeleteMapping(value = "/global-params/{key}")
//  @AuthenticationScope(scopes = {TokenPermission.global})
  @Operation(summary = "Delete specific global Param")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public void delete(@PathVariable String key) {
    paramService.delete(key);
  }
}


