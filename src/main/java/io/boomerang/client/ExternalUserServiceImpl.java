package io.boomerang.client;

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
import io.boomerang.security.service.ExternalTokenService;

@Service
public class ExternalUserServiceImpl implements ExternalUserService {

  @Value("${flow.externalUrl.user}")
  private String externalUserUrl;

  @Autowired
  @Qualifier("internalRestTemplate")
  private RestTemplate restTemplate;

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String TOKEN_PREFIX = "Bearer ";

  @Autowired
  private ExternalTokenService apiTokenService;

  @Override
  public ExternalUserProfile getUserProfileByEmail(String email) {
    try {
      UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(externalUserUrl).
          queryParam("userEmail", email).build();
      HttpHeaders headers = buildHeaders(email);
    
      HttpEntity<String> requestUpdate = new HttpEntity<>("", headers);
      ResponseEntity<ExternalUserProfile> response =
          restTemplate.exchange(uriComponents.toUriString(), HttpMethod.GET, requestUpdate, ExternalUserProfile.class);
      return response.getBody();
    } catch (RestClientException e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public ExternalUserProfile getUserProfileById(String id) {
    UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(externalUserUrl).
        queryParam("userId", id).build();
    HttpHeaders headers = buildHeaders(null);
  
    HttpEntity<String> requestUpdate = new HttpEntity<>("", headers);
    ResponseEntity<ExternalUserProfile> response =
        restTemplate.exchange(uriComponents.toUriString(), HttpMethod.GET, requestUpdate, ExternalUserProfile.class);
    return response.getBody();
  }
  
  private HttpHeaders buildHeaders(String email) {

    final HttpHeaders headers = new HttpHeaders();
    headers.add("Accept", "application/json");
    
    if (email != null) {
      headers.add(AUTHORIZATION_HEADER, TOKEN_PREFIX + apiTokenService.createJWTToken(email));     headers.add(AUTHORIZATION_HEADER, TOKEN_PREFIX + apiTokenService.createJWTToken(email));
    } else {
      headers.add(AUTHORIZATION_HEADER, TOKEN_PREFIX + apiTokenService.createJWTToken());
    }
    
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

}
