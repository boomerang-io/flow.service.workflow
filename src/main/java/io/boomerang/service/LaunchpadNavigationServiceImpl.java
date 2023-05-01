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
import io.boomerang.model.profile.Features;
import io.boomerang.model.profile.Navigation;
import io.boomerang.model.profile.NavigationResponse;
import io.boomerang.model.profile.Platform;
import io.boomerang.security.model.UserToken;
import io.boomerang.security.service.ApiTokenService;
import io.boomerang.security.service.UserIdentityService;
import io.boomerang.v4.model.AbstractParam;
import io.boomerang.v4.service.SettingsServiceImpl;

@Service
public class LaunchpadNavigationServiceImpl implements LaunchpadNavigationService {

  @Value("${core.feature.notifications.enable}")
  private Boolean enableFeatureNotification;

  @Value("${core.feature.docs.enable}")
  private boolean enableDocs;

  @Value("${core.feature.support.enable}")
  private Boolean enableSupport;

  @Value("${core.platform.name}")
  private String platformName;

  @Value("${boomerang.signOutUrl}")
  private String platformSignOutUrl;

  @Value("${boomerang.baseUrl}")
  private String platformBaseUrl;

  @Value("${boomerang.version}")
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
  private ApiTokenService apiTokenService;
  
  @Autowired
  private UserIdentityService identityService;
  
  @Override
  public NavigationResponse getLaunchpadNavigation(boolean isUserAdmin) {
    
    UserToken userToken = identityService.getUserDetails();
    if (userToken == null) {
      return null;
    }
    String email = userToken.getEmail();
    
    if (platformNavigationUrl.isBlank()) {
      return getFlowNavigationResponse();
    }
    else {
      return getExternalNavigationResponse(email);
    }
  }

  private NavigationResponse getFlowNavigationResponse() {
    final List<Navigation> navList = new ArrayList<>();
    NavigationResponse navigationResponse = new NavigationResponse();
    navigationResponse.setNavigation(navList);

    Features features = new Features();
    features.setNotificationsEnabled(false);
    features.setDocsEnabled(false);
    features.setSupportEnabled(false);
    features.setConsentEnabled(false);
    
    navigationResponse.setFeatures(features);

    String appName = settingsService.getSetting("customizations", "appName").getValue();
    String platformName = settingsService.getSetting("customizations", "platformName").getValue();
    String displayLogo = settingsService.getSetting("customizations", "displayLogo").getValue();
    String logoURL = settingsService.getSetting("customizations", "logoURL").getValue();
    String name = platformName + " " + appName;
    Platform platform = new Platform();
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
  
  private NavigationResponse getExternalNavigationResponse(String email) {
    UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(platformNavigationUrl).build();
    HttpHeaders headers = buildHeaders(email);
    HttpEntity<String> requestUpdate = new HttpEntity<>("", headers);
    ResponseEntity<NavigationResponse> response =
        restTemplate.exchange(uriComponents.toUriString(), HttpMethod.GET, requestUpdate, NavigationResponse.class);
    NavigationResponse result = response.getBody();
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
      AbstractParam config = settingsService.getSetting("customizations", "appName");
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