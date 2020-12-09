package net.boomerangplatform.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import net.boomerangplatform.client.model.UserProfile;
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

  @Override
  public UserProfile getInternalUserProfile() {
    
    String userEmail = userDetailsService.getUserDetails().getEmail();
    UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(externalUserUrl).
        queryParam("userEmail", userEmail).build();
    HttpHeaders headers = new HttpHeaders();
  
    HttpEntity<String> requestUpdate = new HttpEntity<>("", headers);
    ResponseEntity<UserProfile> response =
        restTemplate.exchange(uriComponents.toUriString(), HttpMethod.GET, requestUpdate, UserProfile.class);
    return response.getBody();
  }

  @Override
  public UserProfile getUserProfileById(String id) {
    UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(externalUserUrl).
        queryParam("userId", id).build();
    HttpHeaders headers = new HttpHeaders();
  
    HttpEntity<String> requestUpdate = new HttpEntity<>("", headers);
    ResponseEntity<UserProfile> response =
        restTemplate.exchange(uriComponents.toUriString(), HttpMethod.GET, requestUpdate, UserProfile.class);
    return response.getBody();
  }

}
