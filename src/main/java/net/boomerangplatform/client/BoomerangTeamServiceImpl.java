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
import net.boomerangplatform.client.model.Team;
import net.boomerangplatform.security.service.ApiTokenService;

@Service
public class BoomerangTeamServiceImpl implements BoomerangTeamService {

  @Value("${admin.team.url}")
  private String teamUrl;

  @Autowired
  @Qualifier("internalRestTemplate")
  private RestTemplate restTemplate;
  @Autowired
  private ApiTokenService apiTokenService;

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String TOKEN_PREFIX = "Bearer ";

  @Override
  public Team getTeam(String highLevelGroupId) {
    HttpHeaders headers = new HttpHeaders();
    headers.add(AUTHORIZATION_HEADER, TOKEN_PREFIX + apiTokenService.getUserToken());

    HttpEntity<String> requestUpdate = new HttpEntity<>("", headers);
    ResponseEntity<Team> response = restTemplate.exchange(teamUrl + "/" + highLevelGroupId,
        HttpMethod.GET, requestUpdate, Team.class);
    return response.getBody();
  }

}
