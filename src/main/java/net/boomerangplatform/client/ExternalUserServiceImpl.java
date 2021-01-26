package net.boomerangplatform.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import net.boomerangplatform.client.model.UserProfile;
import net.boomerangplatform.security.service.ApiTokenService;
import net.boomerangplatform.security.service.UserDetailsService;

@Service
public class ExternalUserServiceImpl implements ExernalUserService {

  @Value("${flow.externalUrl.user}")
  private String externalUserUrl;

  @Autowired
  @Qualifier("internalRestTemplate")
  private RestTemplate restTemplate;
  
  @Autowired
  private UserDetailsService userDetailsService;

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String TOKEN_PREFIX = "Bearer ";

  @Autowired
  private ApiTokenService apiTokenService;

  @Override
  public UserProfile getInternalUserProfile() {
    try {
      String userEmail = userDetailsService.getUserDetails().getEmail();
      UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(externalUserUrl).
          queryParam("userEmail", userEmail).build();
      HttpHeaders headers = buildHeaders();
    
      HttpEntity<String> requestUpdate = new HttpEntity<>("", headers);
      ResponseEntity<UserProfile> response =
          restTemplate.exchange(uriComponents.toUriString(), HttpMethod.GET, requestUpdate, UserProfile.class);
      return response.getBody();
    } catch (RestClientException e) {
      return null;
    }
  }

  @Override
  public UserProfile getUserProfileById(String id) {
    UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(externalUserUrl).
        queryParam("userId", id).build();
    HttpHeaders headers = buildHeaders();
  
    HttpEntity<String> requestUpdate = new HttpEntity<>("", headers);
    ResponseEntity<UserProfile> response =
        restTemplate.exchange(uriComponents.toUriString(), HttpMethod.GET, requestUpdate, UserProfile.class);
    return response.getBody();
  }
  
  private HttpHeaders buildHeaders() {

    final HttpHeaders headers = new HttpHeaders();
    headers.add("Accept", "application/json");
    headers.add(AUTHORIZATION_HEADER, TOKEN_PREFIX + apiTokenService.getUserToken());

    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

}
