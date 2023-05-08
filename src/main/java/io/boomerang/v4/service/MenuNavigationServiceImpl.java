package io.boomerang.v4.service;

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
public class MenuNavigationServiceImpl implements MenuNavigationService {

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
  public List<Navigation> getNavigation(boolean isUserAdmin, Optional<String> teamId) {

    FeaturesAndQuotas features = featureService.get();

    if (flowExternalUrlNavigation.isBlank()) {
      List<Navigation> response = new ArrayList<>();
      Navigation workflows = new Navigation();
      workflows.setName("Workflows");
      workflows.setType(NavigationType.link);
      workflows.setIcon("FlowData16");
      workflows.setLink(flowAppsUrl + "/workflows");
      response.add(workflows);

      Navigation activity = new Navigation();
      activity.setName("Activity");
      activity.setType(NavigationType.link);
      activity.setIcon("Activity16");
      activity.setLink(flowAppsUrl + "/activity");
      response.add(activity);

      Navigation actions = new Navigation();
      actions.setName("Actions");
      actions.setType(NavigationType.link);
      actions.setIcon("Stamp16");
      actions.setLink(flowAppsUrl + "/actions");
      response.add(actions);

      Navigation insights = new Navigation();
      insights.setName("Insights");
      insights.setType(NavigationType.link);
      insights.setIcon("ChartScatter16");
      insights.setLink(flowAppsUrl + "/insights");
      response.add(insights);

      Navigation schedules = new Navigation();
      schedules.setName("Schedules");
      schedules.setType(NavigationType.link);
      schedules.setIcon("CalendarHeatMap16");
      schedules.setLink(flowAppsUrl + "/schedules");
      response.add(schedules);

      Navigation management = new Navigation();
      management.setName("Manage");
      management.setIcon("SettingsAdjust16");
      management.setChildLinks(new ArrayList<>());
      management.setType(NavigationType.category);

      Navigation teamApprovers = new Navigation();
      teamApprovers.setName("Team Parameters");
      teamApprovers.setLink(flowAppsUrl + "/manage/team-parameters");
      teamApprovers.setType(NavigationType.link);
      management.getChildLinks().add(teamApprovers);

      Navigation teamProperties = new Navigation();
      teamProperties.setName("Team Approvers");
      teamProperties.setLink(flowAppsUrl + "/manage/approver-groups");
      teamProperties.setType(NavigationType.link);
      management.getChildLinks().add(teamProperties);

      Navigation teamTasks = new Navigation();
      teamTasks.setName("Team Tasks");
      teamTasks.setLink(flowAppsUrl + "/manage/task-templates");
      teamTasks.setType(NavigationType.link);
      management.getChildLinks().add(teamTasks);

      Navigation teamTokens = new Navigation();
      teamTokens.setName("Team Tokens ");
      teamTokens.setLink(flowAppsUrl + "/manage/team-tokens");
      teamTokens.setType(NavigationType.link);
      management.getChildLinks().add(teamTokens);

      response.add(management);

      if (isUserAdmin) {
        Navigation admin = new Navigation();
        admin.setName("Administer");
        admin.setType(NavigationType.category);
        admin.setIcon("Settings16");
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


        Navigation quotas = new Navigation();
        quotas.setName("Team Quotas");
        quotas.setLink(flowAppsUrl + "/admin/quotas");
        quotas.setType(NavigationType.link);
        admin.getChildLinks().add(quotas);

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

        Navigation systemWorkflows = new Navigation();
        systemWorkflows.setName("System Workflows");
        systemWorkflows.setLink(flowAppsUrl + "/admin/system-workflows");
        systemWorkflows.setType(NavigationType.link);

        admin.getChildLinks().add(systemWorkflows);

        response.add(admin);

      }

      return response;
    }

    else {

      UriComponentsBuilder uriComponentsBuilder =
          UriComponentsBuilder.fromHttpUrl(flowExternalUrlNavigation);
      UriComponents uriComponents = null;

      if (teamId.isEmpty() || teamId.get().isBlank()) {
        uriComponents = uriComponentsBuilder.build();
      } else {
        uriComponents = uriComponentsBuilder.queryParam("teamId", teamId.get()).build();
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
