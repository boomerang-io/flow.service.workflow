package io.boomerang.client;

import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import io.boomerang.client.model.ExternalTeam;
import io.boomerang.mongo.entity.FlowTeamEntity;
import io.boomerang.mongo.model.Quotas;
import io.boomerang.security.service.ApiTokenService;

@Service
public class ExternalTeamServiceImpl implements ExternalTeamService {

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

  private static final Logger LOGGER = LogManager.getLogger(ExternalTeamServiceImpl.class);
  
  @Override
  public List<FlowTeamEntity> getExternalTeams(String url) {
    
    List<FlowTeamEntity> flowTeams = new LinkedList<>();
    
    try {
      final HttpHeaders headers = buildHeaders();
      final HttpEntity<String> request = new HttpEntity<>(headers);

      ResponseEntity<List<ExternalTeam>> response = restTemplate.exchange(url, HttpMethod.GET,
          request, new ParameterizedTypeReference<List<ExternalTeam>>() {});
      List<ExternalTeam> allTeams = response.getBody();
      
    
      for (ExternalTeam team : allTeams) {
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
    } catch (RestClientException e) {
      LOGGER.error("Error retrievign teams");
      LOGGER.error(ExceptionUtils.getStackTrace(e));
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
