package net.boomerangplatform.client;

import java.util.LinkedList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import net.boomerangplatform.client.model.CICDTeam;
import net.boomerangplatform.mongo.entity.FlowTeamEntity;
import net.boomerangplatform.mongo.model.Quotas;
import net.boomerangplatform.security.service.ApiTokenService;

@Service
public class BoomerangCICDServiceImpl implements BoomerangCICDService {

  @Value("${cicd.teams.url}")
  private String teamUrl;

  @Autowired
  @Qualifier("internalRestTemplate")
  private RestTemplate restTemplate;

  @Autowired
  private ApiTokenService apiTokenService;

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String TOKEN_PREFIX = "Bearer ";
  
  @Value("${max.workflow.count}")
  private Integer maxWorkflowCount;
  
  @Value("${max.workflow.execution.monthly}")
  private Integer maxWorkflowExecutionMonthly;
  
  @Value("${max.workflow.storage}")
  private Integer maxWorkflowStorage;
  
  @Value("${max.workflow.execution.time}")
  private Integer maxWorkflowExecutionTime;
  
  @Value("${max.concurrent.workflows}")
  private Integer maxConcurrentWorkflows;

  @Override
  public List<FlowTeamEntity> getCICDTeams() {
    final HttpHeaders headers = buildHeaders();
    final HttpEntity<String> request = new HttpEntity<>(headers);

    ResponseEntity<List<CICDTeam>> response = restTemplate.exchange(teamUrl, HttpMethod.GET,
        request, new ParameterizedTypeReference<List<CICDTeam>>() {});
    List<CICDTeam> allTeams = response.getBody();
    
    List<FlowTeamEntity> flowTeams = new LinkedList<>();
    
    for (CICDTeam team : allTeams) {
      FlowTeamEntity newTeam = new FlowTeamEntity();
      newTeam.setId(team.getId());
      newTeam.setName(team.getName());
      newTeam.setIsActive(true);
      newTeam.setHigherLevelGroupId(team.getId());
      
      if(newTeam.getQuotas() == null) {
        Quotas quotas = new Quotas();
        quotas.setMaxWorkflowCount(maxWorkflowCount);
        quotas.setMaxWorkflowExecutionMonthly(maxWorkflowExecutionMonthly);
        quotas.setMaxWorkflowStorage(maxWorkflowStorage);
        quotas.setMaxWorkflowExecutionTime(maxWorkflowExecutionTime);
        quotas.setMaxConcurrentWorkflows(maxConcurrentWorkflows);
        newTeam.setQuotas(quotas);
      }
      
      flowTeams.add(newTeam);
    }
    
    return flowTeams;
  }

  private HttpHeaders buildHeaders() {

    final HttpHeaders headers = new HttpHeaders();
    headers.add("Accept", "application/json");
    headers.add(AUTHORIZATION_HEADER, TOKEN_PREFIX + apiTokenService.getUserToken());

    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }
}
