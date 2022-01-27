package io.boomerang.service.crud;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import io.boomerang.client.ExternalTeamService;
import io.boomerang.client.ExternalUserService;
import io.boomerang.client.model.Team;
import io.boomerang.client.model.UserProfile;
import io.boomerang.model.FlowTeam;
import io.boomerang.model.FlowUser;
import io.boomerang.model.FlowWorkflowRevision;
import io.boomerang.model.TeamMember;
import io.boomerang.model.TeamQueryResult;
import io.boomerang.model.TeamWorkflowSummary;
import io.boomerang.model.WorkflowQuotas;
import io.boomerang.model.WorkflowSummary;
import io.boomerang.model.teams.ApproverGroupResponse;
import io.boomerang.model.teams.ApproverUser;
import io.boomerang.model.teams.CreateApproverGroupRequest;
import io.boomerang.mongo.entity.ActivityEntity;
import io.boomerang.mongo.entity.FlowTeamConfiguration;
import io.boomerang.mongo.entity.FlowUserEntity;
import io.boomerang.mongo.entity.TeamEntity;
import io.boomerang.mongo.entity.WorkflowEntity;
import io.boomerang.mongo.model.ActivityStorage;
import io.boomerang.mongo.model.ApproverGroup;
import io.boomerang.mongo.model.Quotas;
import io.boomerang.mongo.model.Settings;
import io.boomerang.mongo.model.Storage;
import io.boomerang.mongo.model.TaskStatus;
import io.boomerang.mongo.model.UserType;
import io.boomerang.mongo.service.FlowSettingsService;
import io.boomerang.mongo.service.FlowTeamService;
import io.boomerang.mongo.service.FlowUserService;
import io.boomerang.mongo.service.FlowWorkflowActivityService;
import io.boomerang.mongo.service.FlowWorkflowService;
import io.boomerang.service.UserIdentityService;

@Service
public class TeamServiceImpl implements TeamService {

  @Autowired
  private ExternalUserService boomerangUserService;

  @Autowired
  private ExternalTeamService externalTeamService;

  @Value("${flow.externalUrl.team}")
  private String flowExternalUrlTeam;

  @Value("${flow.externalUrl.user}")
  private String flowExternalUrlUser;

  @Autowired
  private FlowTeamService flowTeamService;

  @Autowired
  private FlowUserService flowUserService;

  @Autowired
  private FlowWorkflowActivityService flowWorkflowActivityService;

  @Autowired
  private FlowWorkflowService flowWorkflowService;

  @Autowired
  private FlowSettingsService flowSettingsService;

  public static final String TEAMS = "teams";
  public static final String MAX_TEAM_WORKFLOW_COUNT = "max.team.workflow.count";
  public static final String MAX_TEAM_CONCURRENT_WORKFLOW = "max.team.concurrent.workflows";
  public static final String MAX_TEAM_WORKFLOW_EXECUTION_MONTHLY =
      "max.team.workflow.execution.monthly";
  public static final String MAX_TEAM_WORKFLOW_STORAGE = "max.team.workflow.storage";
  public static final String MAX_TEAM_WORKFLOW_DURATION = "max.team.workflow.duration";


  @Autowired
  private UserIdentityService userIdentiyService;

  @Autowired
  private WorkflowService workflowService;

  @Autowired
  private WorkflowVersionService workflowVersionService;

  private FlowTeam createFlowTeam(TeamEntity team) {
    FlowTeam flowTeam = new FlowTeam();
    BeanUtils.copyProperties(team, flowTeam);

    List<String> teamIds = new LinkedList<>();
    teamIds.add(flowTeam.getId());

    List<FlowUserEntity> existingUsers = this.userIdentiyService.getUsersForTeams(teamIds);
    convertToFlowUserList(flowTeam, existingUsers);
    if (team.getIsActive() == null) {
      flowTeam.setIsActive(true);
    }

    final List<WorkflowSummary> workflows = workflowService.getWorkflowsForTeam(team.getId());
    flowTeam.setWorkflows(workflows);

    return flowTeam;
  }

  @Override
  public void createFlowTeam(String higherLevelGroupId, String teamName) {


    final TeamEntity flowTeamEntity = new TeamEntity();
    flowTeamEntity.setName(teamName);
    flowTeamEntity.setHigherLevelGroupId(higherLevelGroupId);

    flowTeamEntity.setIsActive(true);
    if (flowTeamEntity.getQuotas() == null) {
      Quotas quotas = new Quotas();
      quotas.setMaxWorkflowCount(Integer.valueOf(
          flowSettingsService.getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_COUNT).getValue()));
      quotas.setMaxWorkflowExecutionMonthly(Integer.valueOf(flowSettingsService
          .getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_EXECUTION_MONTHLY).getValue()));
      quotas.setMaxWorkflowStorage(Integer.valueOf(flowSettingsService
          .getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_STORAGE).getValue().replace("Gi", "")));
      quotas.setMaxWorkflowExecutionTime(Integer.valueOf(
          flowSettingsService.getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_DURATION).getValue()));
      quotas.setMaxConcurrentWorkflows(Integer.valueOf(
          flowSettingsService.getConfiguration(TEAMS, MAX_TEAM_CONCURRENT_WORKFLOW).getValue()));
      flowTeamEntity.setQuotas(quotas);
    }
    flowTeamService.save(flowTeamEntity);
  }

  @Override
  public FlowTeamConfiguration createNewTeamProperty(String teamId,
      FlowTeamConfiguration property) {
    TeamEntity flowTeamEntity = flowTeamService.findById(teamId);

    if (flowTeamEntity.getSettings() == null) {
      flowTeamEntity.setSettings(new Settings());
      flowTeamEntity = flowTeamService.save(flowTeamEntity);
    }

    if (flowTeamEntity.getSettings().getProperties() == null) {
      flowTeamEntity.getSettings().setProperties(new LinkedList<>());
    }

    List<FlowTeamConfiguration> configItems = flowTeamEntity.getSettings().getProperties();
    String newUuid = UUID.randomUUID().toString();
    property.setId(newUuid);
    configItems.add(property);
    flowTeamService.save(flowTeamEntity);

    return property;
  }

  @Override
  public FlowTeam createStandaloneTeam(String name, Quotas quota) {
    TeamEntity flowTeamEntity = new TeamEntity();
    flowTeamEntity.setName(name);
    flowTeamEntity.setIsActive(true);
    flowTeamEntity.setHigherLevelGroupId(flowTeamEntity.getId());
    flowTeamEntity = flowTeamService.save(flowTeamEntity);

    Quotas quotas = new Quotas();

    quotas.setMaxWorkflowCount(
        quota != null && quota.getMaxWorkflowCount() != null ? quota.getMaxWorkflowCount()
            : Integer.valueOf(
                flowSettingsService.getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_COUNT).getValue()));

    quotas.setMaxWorkflowExecutionMonthly(
        quota != null && quota.getMaxWorkflowExecutionMonthly() != null
            ? quota.getMaxWorkflowExecutionMonthly()
            : Integer.valueOf(flowSettingsService
                .getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_EXECUTION_MONTHLY).getValue()));

    quotas.setMaxWorkflowStorage(
        quota != null && quota.getMaxWorkflowStorage() != null ? quota.getMaxWorkflowStorage()
            : Integer.valueOf(flowSettingsService.getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_STORAGE)
                .getValue().replace("Gi", "")));


    quotas.setMaxWorkflowExecutionTime(quota != null && quota.getMaxWorkflowExecutionTime() != null
        ? quota.getMaxWorkflowExecutionTime()
        : Integer.valueOf(
            flowSettingsService.getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_DURATION).getValue()));

    quotas.setMaxConcurrentWorkflows(quota != null
        && quota.getMaxConcurrentWorkflows() != null
        ? quota.getMaxConcurrentWorkflows()
        : Integer.valueOf(
            flowSettingsService.getConfiguration(TEAMS, MAX_TEAM_CONCURRENT_WORKFLOW).getValue()));

    flowTeamEntity.setQuotas(quotas);

    flowTeamEntity = flowTeamService.save(flowTeamEntity);

    FlowTeam team = new FlowTeam();

    BeanUtils.copyProperties(flowTeamEntity, team);

    return team;
  }

  @Override
  public TeamEntity deactivateTeam(String teamId) {
    validateUser();
    TeamEntity entity = flowTeamService.findById(teamId);
    entity.setIsActive(false);
    return flowTeamService.save(entity);
  }

  @Override
  public void deleteTeamProperty(String teamId, String configurationId) {
    TeamEntity flowTeamEntity = flowTeamService.findById(teamId);

    if (flowTeamEntity.getSettings().getProperties() != null) {
      List<FlowTeamConfiguration> configItems = flowTeamEntity.getSettings().getProperties();
      FlowTeamConfiguration item = configItems.stream()
          .filter(config -> configurationId.equals(config.getId())).findAny().orElse(null);

      if (item != null) {
        configItems.remove(item);
      }
      flowTeamEntity.getSettings().setProperties(configItems);
      flowTeamService.save(flowTeamEntity);
    }
  }

  @Override
  public TeamQueryResult getAllAdminTeams(Pageable pageable) {

    final TeamQueryResult result = new TeamQueryResult();

    final Page<TeamEntity> teamList = flowTeamService.findAllTeams(pageable);
    final List<FlowTeam> teams = new LinkedList<>();

    for (TeamEntity team : teamList.getContent()) {
      FlowTeam flowTeam = createFlowTeam(team);

      teams.add(flowTeam);

    }
    result.setRecords(teams);
    result.setPageable(teamList);
    return result;
  }

  @Override
  public List<TeamWorkflowSummary> getAllTeamListing() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<FlowTeamConfiguration> getAllTeamProperties(String teamId) {
    TeamEntity flowTeamEntity = flowTeamService.findById(teamId);

    if (flowTeamEntity.getSettings() != null
        && flowTeamEntity.getSettings().getProperties() != null) {
      return flowTeamEntity.getSettings().getProperties();
    } else {
      return Collections.emptyList();
    }
  }

  @Override
  public List<TeamWorkflowSummary> getAllTeams() {

    List<TeamEntity> flowTeams = getAllTeamsListing();

    final List<TeamWorkflowSummary> teamWorkFlowSummary =
        populateWorkflowSummaryInformation(flowTeams);
    return teamWorkFlowSummary;
  }

  private List<TeamEntity> getAllTeamsListing() {
    List<TeamEntity> flowTeams = null;
    if (!flowExternalUrlTeam.isBlank()) {
      flowTeams = this.externalTeamService.getExternalTeams(flowExternalUrlTeam);
    } else {
      final Page<TeamEntity> paginatedTeamList =
          flowTeamService.findAllActiveTeams(Pageable.unpaged());
      flowTeams = paginatedTeamList.getContent();
    }
    return flowTeams;
  }

  private List<ActivityEntity> getConcurrentWorkflowActivities(String teamId) {
    List<WorkflowEntity> teamWorkflows = flowWorkflowService.getWorkflowsForTeam(teamId);
    List<String> workflowIds = new ArrayList<>();
    for (WorkflowEntity workflow : teamWorkflows) {
      workflowIds.add(workflow.getId());
    }
    return flowWorkflowActivityService.findbyWorkflowIdsAndStatus(workflowIds,
        TaskStatus.inProgress);
  }

  @Override
  public Quotas getDefaultQuotas() {
    Quotas quota = new Quotas();
    quota.setMaxWorkflowCount(Integer
        .valueOf(flowSettingsService.getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_COUNT).getValue()));
    quota.setMaxWorkflowExecutionMonthly(Integer.valueOf(flowSettingsService
        .getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_EXECUTION_MONTHLY).getValue()));
    quota.setMaxWorkflowStorage(Integer.valueOf(
        flowSettingsService.getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_STORAGE).getValue().replace("Gi", "")));
    quota.setMaxWorkflowExecutionTime(Integer.valueOf(
        flowSettingsService.getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_DURATION).getValue()));
    quota.setMaxConcurrentWorkflows(Integer.valueOf(
        flowSettingsService.getConfiguration(TEAMS, MAX_TEAM_CONCURRENT_WORKFLOW).getValue()));
    return quota;

  }

  private List<ActivityEntity> getMonthlyWorkflowActivities(Pageable page, String teamId) {
    Calendar c = Calendar.getInstance();
    c.set(Calendar.DAY_OF_MONTH, 1);
    return flowWorkflowActivityService
        .findAllActivitiesForTeam(Optional.of(c.getTime()), Optional.of(new Date()), teamId, page)
        .getContent();



  }

  @Override
  public FlowTeam getTeamById(String teamId) {
    if (!flowExternalUrlTeam.isBlank()) {
      List<TeamEntity> allFlowteams =
          this.externalTeamService.getExternalTeams(flowExternalUrlTeam);
      if (allFlowteams != null) {
        TeamEntity flowEntity =
            allFlowteams.stream().filter(t -> teamId.equals(t.getId())).findFirst().orElse(null);

        FlowTeam flowTeam = new FlowTeam();
        if (flowEntity != null) {
          BeanUtils.copyProperties(flowEntity, flowTeam);
        }
        return flowTeam;
      }

    } else {
      TeamEntity flowEntity = flowTeamService.findById(teamId);
      FlowTeam flowTeam = new FlowTeam();
      if (flowEntity != null) {
        BeanUtils.copyProperties(flowEntity, flowTeam);
      }

      return flowTeam;
    }
    return null;
  }

  @Override
  public FlowTeam getTeamByIdDetailed(String teamId) {
    TeamEntity flowEntity = flowTeamService.findById(teamId);
    FlowTeam flowTeam = new FlowTeam();
    if (flowEntity != null) {
      BeanUtils.copyProperties(flowEntity, flowTeam);
    }

    final List<WorkflowSummary> workflowSummary = workflowService.getWorkflowsForTeam(teamId);
    flowTeam.setWorkflows(workflowSummary);

    List<String> teamIds = new LinkedList<>();
    teamIds.add(teamId);

    List<FlowUserEntity> existingUsers = this.userIdentiyService.getUsersForTeams(teamIds);
    convertToFlowUserList(flowTeam, existingUsers);

    return flowTeam;
  }

  private void convertToFlowUserList(FlowTeam flowTeam, List<FlowUserEntity> existingUsers) {
    List<FlowUser> users = new LinkedList<>();

    for (FlowUserEntity user : existingUsers) {
      FlowUser flowUser = new FlowUser();
      BeanUtils.copyProperties(user, flowUser);

      users.add(flowUser);
    }
    flowTeam.setUsers(users);
  }

  @Override
  public List<TeamWorkflowSummary> getTeamListing(FlowUserEntity userEntity) {

    List<TeamEntity> flowTeams = getUsersTeamListing(userEntity);
    List<TeamWorkflowSummary> flowTeamListing = new LinkedList<>();

    if (flowTeams != null) {
      for (TeamEntity team : flowTeams) {
        TeamWorkflowSummary summary = new TeamWorkflowSummary(team, null);
        flowTeamListing.add(summary);
      }
    }

    return flowTeamListing;
  }

  @Override
  public WorkflowQuotas getTeamQuotas(String teamId) {
    TeamEntity team = flowTeamService.findById(teamId);

    if (team == null) {
      WorkflowQuotas quotas = new WorkflowQuotas();
      quotas.setMaxConcurrentWorkflows(Integer.MAX_VALUE);
      quotas.setMaxWorkflowExecutionMonthly(Integer.MAX_VALUE);
      quotas.setMaxWorkflowExecutionTime(Integer.MAX_VALUE);
      quotas.setCurrentConcurrentWorkflows(0);
      quotas.setCurrentWorkflowCount(0);
      quotas.setCurrentWorkflowExecutionMonthly(0);
      return quotas;
    }

    List<WorkflowSummary> workflows = workflowService.getWorkflowsForTeam(team.getId());
    Pageable page = Pageable.unpaged();
    List<ActivityEntity> concurrentActivities = getConcurrentWorkflowActivities(teamId);
    List<ActivityEntity> activitiesMonthly = getMonthlyWorkflowActivities(page, teamId);

    Quotas quotas = setTeamQuotas(team);

    team.setQuotas(quotas);
    TeamEntity updatedTeam = this.flowTeamService.save(team);

    WorkflowQuotas workflowQuotas = new WorkflowQuotas();
    workflowQuotas.setMaxWorkflowCount(updatedTeam.getQuotas().getMaxWorkflowCount());
    workflowQuotas
        .setMaxWorkflowExecutionMonthly(updatedTeam.getQuotas().getMaxWorkflowExecutionMonthly());
    workflowQuotas.setMaxWorkflowStorage(updatedTeam.getQuotas().getMaxWorkflowStorage());
    workflowQuotas
        .setMaxWorkflowExecutionTime(updatedTeam.getQuotas().getMaxWorkflowExecutionTime());
    workflowQuotas.setMaxConcurrentWorkflows(updatedTeam.getQuotas().getMaxConcurrentWorkflows());

    workflowQuotas.setCurrentWorkflowCount(workflows.size());
    workflowQuotas.setCurrentConcurrentWorkflows(concurrentActivities.size());
    workflowQuotas.setCurrentWorkflowExecutionMonthly(activitiesMonthly.size());
    setWorkflowStorage(workflows, workflowQuotas);
    setWorkflowResetDate(workflowQuotas);
    return workflowQuotas;
  }

  @Override
  public List<TeamEntity> getUsersTeamListing(FlowUserEntity userEntity) {
    List<String> highLevelGroupIds = new LinkedList<>();
    if (flowExternalUrlUser.isBlank()) {
      highLevelGroupIds = userEntity.getFlowTeams();
    } else {
      UserProfile profile = boomerangUserService.getInternalUserProfile();
      List<Team> teams = profile.getTeams();
      if (teams != null) {
        highLevelGroupIds = teams.stream().map(Team::getId).collect(Collectors.toList());
      }
    }

    List<TeamEntity> flowTeam = null;
    if (!flowExternalUrlTeam.isBlank()) {
      flowTeam = this.externalTeamService.getExternalTeams(flowExternalUrlTeam);
    } else {
      flowTeam = flowTeamService.findTeamsWithHighLevelGroups(highLevelGroupIds);
    }
    return flowTeam;
  }

  @Override
  public List<TeamWorkflowSummary> getUserTeams(FlowUserEntity userEntity) {

    List<TeamEntity> flowTeam = getUsersTeamListing(userEntity);

    final List<TeamWorkflowSummary> teamWorkFlowSummary =
        populateWorkflowSummaryInformation(flowTeam);
    return teamWorkFlowSummary;
  }

  private List<TeamWorkflowSummary> populateWorkflowSummaryInformation(List<TeamEntity> flowTeams) {
    final List<TeamWorkflowSummary> teamWorkFlowSummary = new LinkedList<>();
    for (final TeamEntity entity : flowTeams) {
      final List<WorkflowSummary> workflowSummary =
          workflowService.getWorkflowsForTeam(entity.getId());
      final TeamWorkflowSummary teamWorkFlow = new TeamWorkflowSummary(entity, workflowSummary);
      updateSummaryWithUpgradeFlags(teamWorkFlow);
      updateSummaryWithQuotas(entity, workflowSummary, teamWorkFlow);
      teamWorkFlowSummary.add(teamWorkFlow);
    }
    return teamWorkFlowSummary;
  }

  @Override
  public WorkflowQuotas resetTeamQuotas(String teamId) {
    TeamEntity team = flowTeamService.findById(teamId);
    List<WorkflowSummary> workflows = workflowService.getWorkflowsForTeam(team.getId());
    Pageable page = Pageable.unpaged();
    List<ActivityEntity> concurrentActivities = getConcurrentWorkflowActivities(teamId);
    List<ActivityEntity> activitiesMonthly = getMonthlyWorkflowActivities(page, teamId);

    Quotas teamQuotas = team.getQuotas();
    teamQuotas.setMaxWorkflowCount(Integer
        .valueOf(flowSettingsService.getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_COUNT).getValue()));
    teamQuotas.setMaxWorkflowExecutionMonthly(Integer.valueOf(flowSettingsService
        .getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_EXECUTION_MONTHLY).getValue()));
    teamQuotas.setMaxWorkflowStorage(Integer.valueOf(
        flowSettingsService.getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_STORAGE).getValue().replace("Gi", "")));
    teamQuotas.setMaxWorkflowExecutionTime(Integer.valueOf(
        flowSettingsService.getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_DURATION).getValue()));
    teamQuotas.setMaxConcurrentWorkflows(Integer.valueOf(
        flowSettingsService.getConfiguration(TEAMS, MAX_TEAM_CONCURRENT_WORKFLOW).getValue()));
    TeamEntity updatedTeam = this.flowTeamService.save(team);

    WorkflowQuotas workflowQuotas = new WorkflowQuotas();
    workflowQuotas.setMaxWorkflowCount(updatedTeam.getQuotas().getMaxWorkflowCount());
    workflowQuotas
        .setMaxWorkflowExecutionMonthly(updatedTeam.getQuotas().getMaxWorkflowExecutionMonthly());
    workflowQuotas.setMaxWorkflowStorage(updatedTeam.getQuotas().getMaxWorkflowStorage());
    workflowQuotas
        .setMaxWorkflowExecutionTime(updatedTeam.getQuotas().getMaxWorkflowExecutionTime());
    workflowQuotas.setMaxConcurrentWorkflows(updatedTeam.getQuotas().getMaxConcurrentWorkflows());
    workflowQuotas.setCurrentWorkflowCount(workflows.size());
    workflowQuotas.setCurrentConcurrentWorkflows(concurrentActivities.size());
    workflowQuotas.setCurrentWorkflowExecutionMonthly(activitiesMonthly.size());
    setWorkflowStorage(workflows, workflowQuotas);
    setWorkflowResetDate(workflowQuotas);
    return workflowQuotas;
  }

  private Quotas setTeamQuotas(TeamEntity team) {
    if (team.getQuotas() == null) {
      team.setQuotas(new Quotas());
    }

    Quotas quotas = new Quotas();

    if (team.getQuotas().getMaxWorkflowCount() != null) {
      quotas.setMaxWorkflowCount(team.getQuotas().getMaxWorkflowCount());
    } else {
      quotas.setMaxWorkflowCount(Integer.valueOf(
          flowSettingsService.getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_COUNT).getValue()));
    }
    if (team.getQuotas().getMaxWorkflowExecutionMonthly() != null) {
      quotas.setMaxWorkflowExecutionMonthly(team.getQuotas().getMaxWorkflowExecutionMonthly());
    } else {
      quotas.setMaxWorkflowExecutionMonthly(Integer.valueOf(flowSettingsService
          .getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_EXECUTION_MONTHLY).getValue()));
    }
    if (team.getQuotas().getMaxWorkflowStorage() != null) {
      quotas.setMaxWorkflowStorage(team.getQuotas().getMaxWorkflowStorage());
    } else {
      quotas.setMaxWorkflowStorage(Integer.valueOf(
          flowSettingsService.getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_STORAGE).getValue().replace("Gi", "")));
    }
    if (team.getQuotas().getMaxWorkflowExecutionTime() != null) {
      quotas.setMaxWorkflowExecutionTime(team.getQuotas().getMaxWorkflowExecutionTime());
    } else {
      quotas.setMaxWorkflowExecutionTime(Integer.valueOf(
          flowSettingsService.getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_DURATION).getValue()));
    }
    if (team.getQuotas().getMaxConcurrentWorkflows() != null) {
      quotas.setMaxConcurrentWorkflows(team.getQuotas().getMaxConcurrentWorkflows());
    } else {
      quotas.setMaxConcurrentWorkflows(Integer.valueOf(
          flowSettingsService.getConfiguration(TEAMS, MAX_TEAM_CONCURRENT_WORKFLOW).getValue()));
    }
    return quotas;
  }

  private void setWorkflowResetDate(WorkflowQuotas workflowQuotas) {
    Calendar nextMonth = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    nextMonth.add(Calendar.MONTH, 1);
    nextMonth.set(Calendar.DAY_OF_MONTH, 1);
    nextMonth.set(Calendar.HOUR_OF_DAY, 0);
    nextMonth.set(Calendar.MINUTE, 0);
    nextMonth.set(Calendar.SECOND, 0);
    nextMonth.set(Calendar.MILLISECOND, 0);
    workflowQuotas.setMonthlyResetDate(nextMonth.getTime());
  }

  private void setWorkflowStorage(List<WorkflowSummary> workflows, WorkflowQuotas workflowQuotas) {
    Integer currentWorkflowsPersistentStorage = 0;
    for (WorkflowSummary workflow : workflows) {
      if (workflow.getStorage() == null) {
        workflow.setStorage(new Storage());
      }
      if (workflow.getStorage().getActivity() == null) {
        workflow.getStorage().setActivity(new ActivityStorage());
      }

      if (workflow.getStorage().getActivity().getEnabled()) {
        currentWorkflowsPersistentStorage += 1;
      }
    }
    workflowQuotas.setCurrentWorkflowsPersistentStorage(currentWorkflowsPersistentStorage);
  }

  @Override
  public Quotas updateQuotasForTeam(String teamId, Quotas quotas) {
    TeamEntity team = flowTeamService.findById(teamId);
    team.setQuotas(quotas);
    return flowTeamService.save(team).getQuotas();
  }

  private void updateSummaryWithQuotas(final TeamEntity entity,
      final List<WorkflowSummary> workflowSummary, final TeamWorkflowSummary teamWorkFlow) {

    Quotas quotas = setTeamQuotas(entity);

    teamWorkFlow.setQuotas(quotas);

    List<ActivityEntity> concurrentActivities = getConcurrentWorkflowActivities(entity.getId());
    Pageable page = Pageable.unpaged();
    List<ActivityEntity> activitiesMonthly = getMonthlyWorkflowActivities(page, entity.getId());

    WorkflowQuotas workflowQuotas = new WorkflowQuotas();
    workflowQuotas.setMaxWorkflowCount(quotas.getMaxWorkflowCount());
    workflowQuotas.setMaxWorkflowExecutionMonthly(quotas.getMaxWorkflowExecutionMonthly());
    workflowQuotas.setMaxWorkflowStorage(quotas.getMaxWorkflowStorage());
    workflowQuotas.setMaxWorkflowExecutionTime(quotas.getMaxWorkflowExecutionTime());
    workflowQuotas.setMaxConcurrentWorkflows(quotas.getMaxConcurrentWorkflows());

    workflowQuotas.setCurrentWorkflowCount(workflowSummary.size());
    workflowQuotas.setCurrentConcurrentWorkflows(concurrentActivities.size());
    workflowQuotas.setCurrentWorkflowExecutionMonthly(activitiesMonthly.size());
    setWorkflowStorage(workflowSummary, workflowQuotas);
    setWorkflowResetDate(workflowQuotas);
    teamWorkFlow.setWorkflowQuotas(workflowQuotas);
  }

  private void updateSummaryWithUpgradeFlags(TeamWorkflowSummary teamSummary) {
    if (teamSummary.getWorkflows() != null) {
      for (WorkflowSummary summary : teamSummary.getWorkflows()) {
        String workflowId = summary.getId();
        FlowWorkflowRevision latestRevision =
            workflowVersionService.getLatestWorkflowVersion(workflowId);
        if (latestRevision != null) {
          summary.setTemplateUpgradesAvailable(latestRevision.isTemplateUpgradesAvailable());
        }
      }
    }
  }


  @Override
  public void updateSummaryWithUpgradeFlags(List<WorkflowSummary> workflowSummary) {

    for (WorkflowSummary summary : workflowSummary) {
      String workflowId = summary.getId();
      FlowWorkflowRevision latestRevision =
          workflowVersionService.getLatestWorkflowVersion(workflowId);
      if (latestRevision != null) {
        summary.setTemplateUpgradesAvailable(latestRevision.isTemplateUpgradesAvailable());
      }
    }

  }

  @Override
  public void updateTeam(String teamId, FlowTeam flow) {
    TeamEntity team = flowTeamService.findById(teamId);
    if (flow.getName() != null) {
      team.setName(flow.getName());
    }
    if (flow.getIsActive() != null) {
      team.setIsActive(flow.getIsActive());
    }

    this.flowTeamService.save(team);
  }

  @Override
  public void updateTeamMembers(String teamId, List<String> teamMembers) {
    List<String> teamIds = new LinkedList<>();
    teamIds.add(teamId);
    List<FlowUserEntity> existingUsers = this.userIdentiyService.getUsersForTeams(teamIds);

    List<String> existingTeamIds =
        existingUsers.stream().map(FlowUserEntity::getId).collect(Collectors.toList());

    List<String> removeUsers = existingUsers.stream().map(FlowUserEntity::getId)
        .filter(f -> !teamMembers.contains(f)).collect(Collectors.toList());

    List<String> addUsers =
        teamMembers.stream().filter(f -> !existingTeamIds.contains(f)).collect(Collectors.toList());

    for (String userId : addUsers) {
      Optional<FlowUserEntity> userEntity = flowUserService.getUserById(userId);
      if (userEntity.isPresent()) {
        FlowUserEntity flowUser = userEntity.get();
        if (flowUser.getFlowTeams() == null) {
          flowUser.setFlowTeams(new LinkedList<>());
        }
        flowUser.getFlowTeams().add(teamId);
        this.flowUserService.save(flowUser);
      }
    }

    for (String userId : removeUsers) {
      Optional<FlowUserEntity> userEntity = flowUserService.getUserById(userId);
      if (userEntity.isPresent()) {
        FlowUserEntity flowUser = userEntity.get();
        if (flowUser.getFlowTeams() == null) {
          flowUser.setFlowTeams(new LinkedList<>());
        }
        flowUser.getFlowTeams().remove(teamId);
        this.flowUserService.save(flowUser);
      }
    }
  }


  @Override
  public List<FlowTeamConfiguration> updateTeamProperty(String teamId,
      FlowTeamConfiguration property) {
    TeamEntity flowTeamEntity = flowTeamService.findById(teamId);

    if (flowTeamEntity.getSettings().getProperties() != null) {
      List<FlowTeamConfiguration> configItems = flowTeamEntity.getSettings().getProperties();
      String existingId = property.getId();
      FlowTeamConfiguration item = configItems.stream()
          .filter(config -> existingId.equals(config.getId())).findAny().orElse(null);

      if (item != null) {
        configItems.remove(item);
        configItems.add(property);
      }
      flowTeamEntity.getSettings().setProperties(configItems);
      flowTeamService.save(flowTeamEntity);

      return configItems;
    }

    return Collections.emptyList();
  }

  @Override
  public Quotas updateTeamQuotas(String teamId, Quotas quotas) {
    TeamEntity team = flowTeamService.findById(teamId);

    if (team.getQuotas() == null) {
      team.setQuotas(new Quotas());
    }
    if (quotas.getMaxWorkflowCount() != null) {
      team.getQuotas().setMaxWorkflowCount(quotas.getMaxWorkflowCount());
    }
    if (quotas.getMaxConcurrentWorkflows() != null) {
      team.getQuotas().setMaxConcurrentWorkflows(quotas.getMaxConcurrentWorkflows());
    }
    if (quotas.getMaxWorkflowExecutionMonthly() != null) {
      team.getQuotas().setMaxWorkflowExecutionMonthly(quotas.getMaxWorkflowExecutionMonthly());
    }
    if (quotas.getMaxWorkflowExecutionTime() != null) {
      team.getQuotas().setMaxWorkflowExecutionTime(quotas.getMaxWorkflowExecutionTime());
    }
    if (quotas.getMaxWorkflowStorage() != null) {
      team.getQuotas().setMaxWorkflowStorage(quotas.getMaxWorkflowStorage());
    }

    return flowTeamService.save(team).getQuotas();
  }

  protected void validateUser() {

    FlowUserEntity userEntity = userIdentiyService.getCurrentUser();
    if (userEntity == null || (!userEntity.getType().equals(UserType.admin)
        && !userEntity.getType().equals(UserType.operator))) {

      throw new HttpClientErrorException(HttpStatus.FORBIDDEN);
    }
  }

  @Override
  public List<TeamMember> getTeamMembers(String teamId) {
    List<TeamMember> members = new LinkedList<>();
    if (!flowExternalUrlUser.isBlank()) {
      TeamEntity flowTeam = this.flowTeamService.findById(teamId);
      String externalTeamId = flowTeam.getHigherLevelGroupId();
      List<FlowUserEntity> flowUsers =
          this.externalTeamService.getExternalTeamMemberListing(externalTeamId);
      mapToTeamMemberList(members, flowUsers);
    } else {
      List<String> teamIds = new LinkedList<>();
      teamIds.add(teamId);
      List<FlowUserEntity> existingUsers = this.userIdentiyService.getUsersForTeams(teamIds);
      mapToTeamMemberList(members, existingUsers);
    }
    return members;
  }

  private void mapToTeamMemberList(List<TeamMember> members, List<FlowUserEntity> existingUsers) {
    for (FlowUserEntity u : existingUsers) {
      TeamMember user = new TeamMember();
      user.setEmail(u.getEmail());
      user.setUserName(u.getName());
      user.setUserId(u.getId());
      members.add(user);
    }
  }

  @Override
  public void deleteApproverGroup(String teamId, String groupId) {
    TeamEntity team = flowTeamService.findById(teamId);

    if (team != null) {
      if (team.getApproverGroups() == null) {
        team.setApproverGroups(new LinkedList<ApproverGroup>());
      }
      ApproverGroup deletedGroup = team.getApproverGroups().stream()
          .filter(x -> groupId.equals(x.getId())).findFirst().orElse(null);
      if (deletedGroup != null) {
        team.getApproverGroups().remove(deletedGroup);
      }
      flowTeamService.save(team);
    }
  }

  @Override
  public List<ApproverGroupResponse> getTeamApproverGroups(String teamId) {
    TeamEntity team = flowTeamService.findById(teamId);
    List<ApproverGroupResponse> response = new LinkedList<>();

    if (team != null && team.getApproverGroups() != null) {

      for (ApproverGroup group : team.getApproverGroups()) {
        ApproverGroupResponse approverGroupResponse = this.createApproverGroupResponse(group, team);
        response.add(approverGroupResponse);
      }
    }
    return response;
  }

  @Override
  public ApproverGroupResponse createApproverGroup(String teamId,
      CreateApproverGroupRequest createApproverGroupRequest) {
    String newGroupId = UUID.randomUUID().toString();
    ApproverGroup group = new ApproverGroup();
    group.setId(newGroupId);
    group.setName(createApproverGroupRequest.getGroupName());

    if (createApproverGroupRequest.getApprovers() != null) {
      List<ApproverUser> users = new LinkedList<>();
      for (ApproverUser user : createApproverGroupRequest.getApprovers()) {
        ApproverUser newUser = new ApproverUser();
        newUser.setUserId(user.getUserId());

        users.add(newUser);
      }
      group.setApprovers(users);
    }

    TeamEntity team = flowTeamService.findById(teamId);

    if (team != null) {
      if (team.getApproverGroups() == null) {
        team.setApproverGroups(new LinkedList<ApproverGroup>());
      }
      List<ApproverGroup> approverGroups = team.getApproverGroups();
      approverGroups.add(group);
      flowTeamService.save(team);
      return this.createApproverGroupResponse(group, team);
    }
    return null;
  }

  private ApproverGroupResponse createApproverGroupResponse(ApproverGroup group, TeamEntity team) {

    ApproverGroupResponse approverGroupResponse = new ApproverGroupResponse();
    approverGroupResponse.setGroupId(group.getId());
    approverGroupResponse.setGroupName(group.getName());
    approverGroupResponse.setTeamId(team.getId());
    approverGroupResponse.setTeamName(team.getName());

    if (group.getApprovers() == null) {
      group.setApprovers(new LinkedList<>());
    }

    approverGroupResponse.setApprovers(group.getApprovers());

    for (ApproverUser approverUser : group.getApprovers()) {
      FlowUserEntity flowUser = this.userIdentiyService.getUserByID(approverUser.getUserId());
      approverUser.setUserEmail(flowUser.getEmail());
      approverUser.setUserName(flowUser.getName());
    }

    return approverGroupResponse;
  }

  @Override
  public ApproverGroupResponse updateApproverGroup(String teamId, String groupId,
      CreateApproverGroupRequest updatedRequest) {
    TeamEntity team = flowTeamService.findById(teamId);

    if (team != null) {
      if (team.getApproverGroups() == null) {
        team.setApproverGroups(new LinkedList<ApproverGroup>());
      }
      ApproverGroup updatedGroup = team.getApproverGroups().stream()
          .filter(x -> groupId.equals(x.getId())).findFirst().orElse(null);
      if (updatedGroup != null) {
        if (updatedRequest.getGroupName() != null) {
          updatedGroup.setName(updatedRequest.getGroupName());
        }
        if (updatedRequest.getApprovers() != null) {
          List<ApproverUser> users = new LinkedList<>();
          for (ApproverUser user : updatedRequest.getApprovers()) {
            ApproverUser newUser = new ApproverUser();
            newUser.setUserId(user.getUserId());
            users.add(newUser);
          }
          updatedGroup.setApprovers(users);
        }
        flowTeamService.save(team);
        return this.createApproverGroupResponse(updatedGroup, team);
      }
    }
    return null;
  }

  @Override
  public ApproverGroupResponse getSingleAproverGroup(String teamId, String groupId) {
    TeamEntity team = flowTeamService.findById(teamId);
    if (team != null) {
      if (team.getApproverGroups() == null) {
        team.setApproverGroups(new LinkedList<ApproverGroup>());
      }
      ApproverGroup group = team.getApproverGroups().stream().filter(x -> groupId.equals(x.getId()))
          .findFirst().orElse(null);
      if (group != null) {
        return this.createApproverGroupResponse(group, team);
      }
    }
    return null;
  }
}
