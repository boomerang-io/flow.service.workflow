package io.boomerang.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import io.boomerang.security.service.ApiTokenService;
import io.boomerang.security.service.IdentityService;
import io.boomerang.v4.model.FeaturesAndQuotas;
import io.boomerang.v4.model.Navigation;
import io.boomerang.v4.model.NavigationType;

@Service
public class NavigationServiceImpl implements NavigationService {

  @Value("${flow.externalUrl.navigation}")
  private String flowExternalUrlNavigation;

  @Autowired
  private ApiTokenService apiTokenService;

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String TOKEN_PREFIX = "Bearer ";

  @Autowired
  @Qualifier("internalRestTemplate")
  private RestTemplate restTemplate;

  @Autowired
  private FeatureService featureService;

  @Value("${flow.apps.flow.url}")
  private String flowAppsUrl;

  @Autowired
  private IdentityService identityService;

  @Override
  public List<Navigation> getNavigation(boolean isUserAdmin, Optional<String> optTeamId) {

    FeaturesAndQuotas features = featureService.get();
    
    boolean disabled = optTeamId.isPresent() ? false : true;
    String teamIdURLContext = optTeamId.isPresent() ? "/" + optTeamId.get() : "";
    
    if (flowExternalUrlNavigation.isBlank()) {
      List<Navigation> response = new ArrayList<>();
      Navigation home = new Navigation();
      home.setName("Home");
      home.setType(NavigationType.link);
      home.setDisabled(false);
      home.setIcon("Home");
      home.setLink(flowAppsUrl + "/home");
      response.add(home);
      
      Navigation divider = new Navigation();
      divider.setType(NavigationType.divider);
      
      Navigation workflows = new Navigation();
      workflows.setName("Workflows");
      workflows.setType(NavigationType.link);
      workflows.setDisabled(disabled);
      workflows.setIcon("FlowData");
      workflows.setLink(flowAppsUrl + teamIdURLContext + "/workflows");
      response.add(workflows);

      Navigation activity = new Navigation();
      activity.setName("Activity");
      activity.setType(NavigationType.link);
      activity.setDisabled(disabled);
      activity.setIcon("Activity");
      activity.setLink(flowAppsUrl + teamIdURLContext + "/activity");
      response.add(activity);

      Navigation actions = new Navigation();
      actions.setName("Actions");
      actions.setType(NavigationType.link);
      actions.setDisabled(disabled);
      actions.setIcon("Stamp");
      actions.setLink(flowAppsUrl + teamIdURLContext + "/actions");
      response.add(actions);

      Navigation insights = new Navigation();
      insights.setName("Insights");
      insights.setType(NavigationType.link);
      insights.setDisabled(disabled);
      insights.setIcon("ChartScatter");
      insights.setLink(flowAppsUrl + teamIdURLContext + "/insights");
      response.add(insights);

      Navigation schedules = new Navigation();
      schedules.setName("Schedules");
      schedules.setType(NavigationType.link);
      schedules.setDisabled(disabled);
      schedules.setIcon("CalendarHeatMap");
      schedules.setLink(flowAppsUrl + teamIdURLContext + "/schedules");
      response.add(schedules);
      
      divider.setType(NavigationType.divider);

      Navigation teamTasks = new Navigation();
      teamTasks.setName("Task Manager");
      teamTasks.setType(NavigationType.link);
      teamTasks.setDisabled(disabled);
      teamTasks.setIcon("TaskSettings");
      teamTasks.setLink(flowAppsUrl + teamIdURLContext + "/task-templates");
      response.add(teamTasks);

      Navigation teamParameters = new Navigation();
      teamParameters.setName("Parameters");
      teamParameters.setType(NavigationType.link);
      teamParameters.setDisabled(disabled);
      teamParameters.setIcon("Parameters");
      teamParameters.setLink(flowAppsUrl + teamIdURLContext + "/parameters");
      response.add(teamParameters);

      Navigation teamTokens = new Navigation();
      teamTokens.setName("Tokens");
      teamTokens.setType(NavigationType.link);
      teamTokens.setDisabled(disabled);
      teamTokens.setIcon("Password");
      teamTokens.setLink(flowAppsUrl + teamIdURLContext + "/tokens");
      response.add(teamTokens);

      Navigation management = new Navigation();
      management.setName("Manage Team");
      management.setType(NavigationType.link);
      management.setDisabled(false);
      management.setIcon("SettingsAdjust");
      management.setChildLinks(new ArrayList<>());
      response.add(management);

      if (isUserAdmin) {
        Navigation admin = new Navigation();
        admin.setName("Administer");
        admin.setType(NavigationType.category);
        admin.setIcon("Settings");
        admin.setChildLinks(new ArrayList<>());

        if (((Boolean) features.getFeatures().get("team.management"))) {
          Navigation teams = new Navigation();
          teams.setName("Teams");
          teams.setLink(flowAppsUrl + "/admin/teams");
          teams.setType(NavigationType.link);
          admin.getChildLinks().add(teams);
        }

        if (((Boolean) features.getFeatures().get("user.management"))) {
          Navigation users = new Navigation();
          users.setName("Users");
          users.setLink(flowAppsUrl + "/admin/users");
          users.setType(NavigationType.link);
          admin.getChildLinks().add(users);
        }

        Navigation properties = new Navigation();
        properties.setName("Global Parameters");
        properties.setLink(flowAppsUrl + "/admin/parameters");
        properties.setType(NavigationType.link);
        admin.getChildLinks().add(properties);

        Navigation tokens = new Navigation();
        tokens.setName("Global Tokens");
        tokens.setLink(flowAppsUrl + "/admin/tokens");
        tokens.setType(NavigationType.link);
        admin.getChildLinks().add(tokens);

        Navigation settings = new Navigation();
        settings.setName("Settings");
        settings.setLink(flowAppsUrl + "/admin/settings");
        settings.setType(NavigationType.link);
        admin.getChildLinks().add(settings);

        Navigation taskManager = new Navigation();
        taskManager.setName("Task Manager");
        taskManager.setLink(flowAppsUrl + "/admin/task-templates");
        taskManager.setType(NavigationType.link);
        admin.getChildLinks().add(taskManager);

        Navigation templateWorkflows = new Navigation();
        templateWorkflows.setName("Template Workflows");
        templateWorkflows.setLink(flowAppsUrl + "/admin/template-workflows");
        templateWorkflows.setType(NavigationType.link);
        admin.getChildLinks().add(templateWorkflows);

        response.add(admin);

      }

      return response;
    }

    else {

      UriComponentsBuilder uriComponentsBuilder =
          UriComponentsBuilder.fromHttpUrl(flowExternalUrlNavigation);
      UriComponents uriComponents = null;

      if (optTeamId.isEmpty() || optTeamId.get().isBlank()) {
        uriComponents = uriComponentsBuilder.build();
      } else {
        uriComponents = uriComponentsBuilder.queryParam("teamId", optTeamId.get()).build();
      }

      HttpHeaders headers = new HttpHeaders();
      headers.add(AUTHORIZATION_HEADER, TOKEN_PREFIX
          + apiTokenService.createJWTToken(identityService.getCurrentUser().getEmail()));
      HttpEntity<String> request = new HttpEntity<>(headers);
      ResponseEntity<List<Navigation>> response = restTemplate.exchange(uriComponents.toUriString(),
          HttpMethod.GET, request, new ParameterizedTypeReference<List<Navigation>>() {});
      return response.getBody();
    }
  }
}
