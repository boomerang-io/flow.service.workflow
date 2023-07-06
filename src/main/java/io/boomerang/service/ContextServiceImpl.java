package io.boomerang.service;

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import io.boomerang.security.service.ExternalTokenService;
import io.boomerang.security.service.IdentityService;
import io.boomerang.v4.data.entity.UserEntity;
import io.boomerang.v4.model.AbstractParam;
import io.boomerang.v4.model.HeaderFeatures;
import io.boomerang.v4.model.HeaderNavigation;
import io.boomerang.v4.model.HeaderNavigationResponse;
import io.boomerang.v4.model.HeaderPlatform;

@Service
public class ContextServiceImpl implements ContextService {

  @Value("${core.feature.notifications.enable}")
  private Boolean enableFeatureNotification;

  @Value("${core.feature.docs.enable}")
  private boolean enableDocs;

  @Value("${core.feature.support.enable}")
  private Boolean enableSupport;

  @Value("${core.platform.name}")
  private String platformName;

  @Value("${flow.signOutUrl}")
  private String platformSignOutUrl;

  @Value("${flow.baseUrl}")
  private String platformBaseUrl;

  @Value("${flow.version}")
  private String platformVersion;
  
  @Autowired
  @Qualifier("internalRestTemplate")
  private RestTemplate restTemplate;
  
  @Value("${flow.externalUrl.platformNavigation}")
  private String platformNavigationUrl;

  @Autowired
  private SettingsServiceImpl settingsService;
  
  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String TOKEN_PREFIX = "Bearer ";
  
  @Autowired
  private ExternalTokenService apiTokenService;
  
  @Autowired
  private IdentityService identityService;
  
  @Override
  public HeaderNavigationResponse getHeaderNavigation(boolean isUserAdmin) {
    UserEntity user = identityService.getCurrentUser();
    if (user == null) {
      return null;
    }
    String email = user.getEmail();
    
    if (platformNavigationUrl.isBlank()) {
      return getFlowNavigationResponse();
    }
    else {
      return getExternalNavigationResponse(email);
    }
  }

  private HeaderNavigationResponse getFlowNavigationResponse() {
    final List<HeaderNavigation> navList = new ArrayList<>();
    HeaderNavigationResponse navigationResponse = new HeaderNavigationResponse();
    navigationResponse.setNavigation(navList);

    HeaderFeatures features = new HeaderFeatures();
    features.setNotificationsEnabled(false);
    features.setDocsEnabled(false);
    features.setSupportEnabled(false);
    features.setConsentEnabled(false);
    
    navigationResponse.setFeatures(features);

    String appName = settingsService.getSettingConfig("customizations", "appName").getValue();
    String platformName = settingsService.getSettingConfig("customizations", "platformName").getValue();
    String displayLogo = settingsService.getSettingConfig("customizations", "displayLogo").getValue();
    String logoURL = settingsService.getSettingConfig("customizations", "logoURL").getValue();
    String name = platformName + " " + appName;
    HeaderPlatform platform = new HeaderPlatform();
    platform.setName(name.trim());
    platform.setVersion(platformVersion);
    platform.setSignOutUrl(platformBaseUrl + platformSignOutUrl);
    platform.setAppName(appName);
    platform.setPlatformName(platformName);
    platform.setDisplayLogo(Boolean.valueOf(displayLogo));
    platform.setLogoURL(logoURL);
    platform.setPrivateTeams(false);
    platform.setSendMail(false);
    navigationResponse.setPlatform(platform);

    return navigationResponse;
  }
  
  private HeaderNavigationResponse getExternalNavigationResponse(String email) {
    UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(platformNavigationUrl).build();
    HttpHeaders headers = buildHeaders(email);
    HttpEntity<String> requestUpdate = new HttpEntity<>("", headers);
    ResponseEntity<HeaderNavigationResponse> response =
        restTemplate.exchange(uriComponents.toUriString(), HttpMethod.GET, requestUpdate, HeaderNavigationResponse.class);
    HeaderNavigationResponse result = response.getBody();
    if (result != null && result.getPlatform() != null) {
      if(Strings.isBlank(result.getPlatform().getAppName())) {
        // set default appName from settings if the external Navigation API does NOT return appName.
        result.getPlatform().setAppName(this.getAppNameInSettings());
      }
      if(!Strings.isBlank(result.getPlatform().getPlatformName()) 
          && !Strings.isBlank(result.getPlatform().getAppName())) {
        /*
         *  add | to the end of the platformName when both platformName and appName exist. 
         *  The UI header will display like "platformName | appName"
         */
        result.getPlatform().setPlatformName(result.getPlatform().getPlatformName() + " |");
      }
    }
    return result;
  }
  
  private String getAppNameInSettings() {
    try {
      AbstractParam config = settingsService.getSettingConfig("customizations", "appName");
      return config == null ? null : config.getValue();
    } catch (Exception e) {
    }
    // return null instead of throwing exception when appName is not configured in settings.
    return null;
  }
  
  private HttpHeaders buildHeaders(String email) {
    final HttpHeaders headers = new HttpHeaders();
    headers.add("Accept", "application/json");
    headers.add(AUTHORIZATION_HEADER, TOKEN_PREFIX + apiTokenService.createJWTToken(email));
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }
}