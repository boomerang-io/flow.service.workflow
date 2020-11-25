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
import net.boomerangplatform.client.model.UserProfile;
import net.boomerangplatform.security.service.ApiTokenService;

@Service
public class BoomerangUserServiceImpl implements BoomerangUserService {

  @Value("${launchpad.profile.url}")
  private String profileUrl;

  @Value("${users.profile.url}")
  private String userProfileById;

  @Autowired
  @Qualifier("internalRestTemplate")
  private RestTemplate restTemplate;

  @Autowired
  private ApiTokenService apiTokenService;

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String TOKEN_PREFIX = "Bearer ";

  @Override
  public UserProfile getInternalUserProfile() {
    HttpHeaders headers = new HttpHeaders();
    headers.add(AUTHORIZATION_HEADER, TOKEN_PREFIX + apiTokenService.getUserToken());

    HttpEntity<String> requestUpdate = new HttpEntity<>("", headers);
    ResponseEntity<UserProfile> response =
        restTemplate.exchange(profileUrl, HttpMethod.GET, requestUpdate, UserProfile.class);
    return response.getBody();
  }

  @Override
  public UserProfile getUserProfileById(String id) {

    String url = userProfileById + "/" + id;

    HttpHeaders headers = new HttpHeaders();
    headers.add(AUTHORIZATION_HEADER, TOKEN_PREFIX + apiTokenService.getUserToken());

    HttpEntity<String> requestUpdate = new HttpEntity<>("", headers);
    ResponseEntity<UserProfile> response =
        restTemplate.exchange(url, HttpMethod.GET, requestUpdate, UserProfile.class);
    return response.getBody();
  }

}
