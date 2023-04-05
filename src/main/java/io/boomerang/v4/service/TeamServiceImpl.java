package io.boomerang.v4.service;

import static io.boomerang.util.DataAdapterUtil.filterValueByFieldType;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.EnumUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.util.DataAdapterUtil.FieldType;
import io.boomerang.v4.client.EngineClient;
import io.boomerang.v4.data.entity.TeamEntity;
import io.boomerang.v4.data.entity.UserEntity;
import io.boomerang.v4.data.model.Quotas;
import io.boomerang.v4.data.model.TeamAbstractConfiguration;
import io.boomerang.v4.data.model.TeamSettings;
import io.boomerang.v4.data.repository.TeamRepository;
import io.boomerang.v4.data.repository.UserRepository;
import io.boomerang.v4.model.CreateTeamRequest;
import io.boomerang.v4.model.Team;
import io.boomerang.v4.model.TeamResponsePage;
import io.boomerang.v4.model.User;
import io.boomerang.v4.model.UserSummary;
import io.boomerang.v4.model.enums.RelationshipRefType;
import io.boomerang.v4.model.enums.TeamStatus;

@Service
public class TeamServiceImpl implements TeamService {

  public static final String TEAMS = "teams";
  public static final String MAX_TEAM_WORKFLOW_COUNT = "max.team.workflow.count";
  public static final String MAX_TEAM_CONCURRENT_WORKFLOW = "max.team.concurrent.workflows";
  public static final String MAX_TEAM_WORKFLOW_EXECUTION_MONTHLY =
      "max.team.workflow.execution.monthly";
  public static final String MAX_TEAM_WORKFLOW_STORAGE = "max.team.workflow.storage";
  public static final String MAX_TEAM_WORKFLOW_DURATION = "max.team.workflow.duration";

  private static final Logger LOGGER = LogManager.getLogger();

  // @Autowired
  // private ExternalUserService boomerangUserService;
  //
  // @Autowired
  // private ExternalTeamService externalTeamService;
  //
  // @Value("${flow.externalUrl.team}")
  // private String flowExternalUrlTeam;
  //
  // @Value("${flow.externalUrl.user}")
  // private String flowExternalUrlUser;
  //
  @Autowired
  private TeamRepository teamRepository;
  //
  @Autowired
  private UserRepository userRepository;

  @Autowired
  private SettingsService settingsService;

  @Autowired
  private RelationshipService relationshipService;

  @Autowired
  private MongoTemplate mongoTemplate;
  //
  // @Autowired
  // private FlowUserService flowUserService;
  //
  // @Autowired
  // private FlowWorkflowActivityService flowWorkflowActivityService;
  //
  // @Autowired
  // private FlowWorkflowService flowWorkflowService;
  //

  // @Autowired
  // private UserIdentityService userIdentiyService;
  //
  // @Autowired
  // private WorkflowService workflowService;
  //
  // @Autowired
  // private WorkflowVersionService workflowVersionService;

  /*
   * Creates a new Team - Only available to Global tokens / admins
   */
  @Override
  public ResponseEntity<Team> create(CreateTeamRequest createTeamRequest) {
    // TODO: check user is admin or global token
    if (!createTeamRequest.getName().isBlank()) {
      final TeamEntity teamEntity = new TeamEntity();
      teamEntity.setName(createTeamRequest.getName());
      teamEntity.setExternalRef(createTeamRequest.getExternalRef());

      Quotas quotas = new Quotas();
      quotas.setMaxWorkflowCount(Integer
          .valueOf(settingsService.getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_COUNT).getValue()));
      quotas.setMaxWorkflowExecutionMonthly(Integer.valueOf(
          settingsService.getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_EXECUTION_MONTHLY).getValue()));
      quotas.setMaxWorkflowStorage(Integer.valueOf(settingsService
          .getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_STORAGE).getValue().replace("Gi", "")));
      quotas.setMaxWorkflowExecutionTime(Integer
          .valueOf(settingsService.getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_DURATION).getValue()));
      quotas.setMaxConcurrentWorkflows(Integer.valueOf(
          settingsService.getConfiguration(TEAMS, MAX_TEAM_CONCURRENT_WORKFLOW).getValue()));
      teamEntity.setQuotas(quotas);

      // TOOD: check createTeamRequest for Quotas / Users / Labels
      if (createTeamRequest.getUsers() != null && !createTeamRequest.getUsers().isEmpty()) {
        for (UserSummary us : createTeamRequest.getUsers()) {
          // if (flowUserService.getUserWithEmail(flowUser.getEmail()) != null) {
          // userIdsToAdd.add(flowUserService.getUserWithEmail(flowUser.getEmail()).getId());
          // } else {
          // String userName = flowUser.getName();
          // userIdsToAdd.add(flowUserService.getOrRegisterUser(flowUser.getEmail(),
          // userName,flowUser.getType()).getId());
          //
          // }
          if (!us.getId().isEmpty()) {
            // TODO: check if user exists, if so create relationship
          } else if (!us.getEmail().isEmpty()) {
            // TODO: check if user exists, if so create relationship
          } else {
            return ResponseEntity.badRequest().build();
          }
        }
      }

      teamRepository.save(teamEntity);
      return ResponseEntity.ok(new Team(teamEntity));
    } else {
      // TODO: make this a better error for unable to create team i.e. name is mandatory
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
  }

  /*
   * Query for Teams
   * 
   * Returns Teams plus each Teams UserRefs, WorkflowRefs, and Quotas
   */
  @Override
  public TeamResponsePage query(int page, int limit, Sort sort, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus) {
    final Pageable pageable = PageRequest.of(page, limit, sort);
    List<Team> teams = new LinkedList<>();
    List<String> teamRefs = relationshipService.getFilteredRefs(RelationshipRefType.TEAM,
        Optional.empty(), Optional.empty(), Optional.empty());

    if (!teamRefs.isEmpty()) {
      List<Criteria> criteriaList = new ArrayList<>();

      if (queryLabels.isPresent()) {
        queryLabels.get().stream().forEach(l -> {
          String decodedLabel = "";
          try {
            decodedLabel = URLDecoder.decode(l, "UTF-8");
          } catch (UnsupportedEncodingException e) {
            throw new BoomerangException(e, BoomerangError.QUERY_INVALID_FILTERS, "labels");
          }
          LOGGER.debug(decodedLabel.toString());
          String[] label = decodedLabel.split("[=]+");
          Criteria labelsCriteria =
              Criteria.where("labels." + label[0].replace(".", "#")).is(label[1]);
          criteriaList.add(labelsCriteria);
        });
      }

      if (queryStatus.isPresent()) {
        if (queryStatus.get().stream()
            .allMatch(q -> EnumUtils.isValidEnumIgnoreCase(TeamStatus.class, q))) {
          Criteria criteria = Criteria.where("status").in(queryStatus.get());
          criteriaList.add(criteria);
        } else {
          throw new BoomerangException(BoomerangError.QUERY_INVALID_FILTERS, "status");
        }
      }

      Criteria criteria = Criteria.where("id").in(teamRefs);
      criteriaList.add(criteria);

      Criteria[] criteriaArray = criteriaList.toArray(new Criteria[criteriaList.size()]);
      Criteria allCriteria = new Criteria();
      if (criteriaArray.length > 0) {
        allCriteria.andOperator(criteriaArray);
      }
      Query query = new Query(allCriteria);
      query.with(pageable);

      Page<TeamEntity> pages =
          PageableExecutionUtils.getPage(mongoTemplate.find(query.with(pageable), TeamEntity.class),
              pageable, () -> mongoTemplate.count(query, TeamEntity.class));

      List<TeamEntity> teamEntities = pages.getContent();

      if (!teamEntities.isEmpty()) {
        teamEntities.forEach(teamEntity -> {
          List<String> teamWorkflowRefs =
              relationshipService.getFilteredRefs(RelationshipRefType.WORKFLOW, Optional.empty(),
                  Optional.of(List.of(teamEntity.getId())), Optional.empty());
          Team team = new Team(teamEntity);
          team.setWorkflowRefs(teamWorkflowRefs);
          List<String> userRefs = relationshipService.getFilteredRefs(RelationshipRefType.USER,
              Optional.empty(), Optional.of(List.of(teamEntity.getId())), Optional.empty());
          if (!userRefs.isEmpty()) {
            List<UserSummary> teamUsers = new LinkedList<>();
            userRefs.forEach(ref -> {
              Optional<UserEntity> ue = userRepository.findById(ref);
              if (ue.isPresent()) {
                UserSummary u = new UserSummary(ue.get());
                teamUsers.add(u);
              }
            });
            team.setUsers(teamUsers);
          }

          // TODO: set current Quotas
          team.setQuotas(null);

          // TODO: set Approver Groups
          team.setApproverGroups(null);

          // If the parameter is a password, do not return its value, for security reasons.
          if (team.getSettings() != null && team.getSettings().getParameters() != null) {
            filterValueByFieldType(team.getSettings().getParameters(), false,
                FieldType.PASSWORD.value());
          }

          teams.add(team);
        });
      }
      return new TeamResponsePage(teams, pageable, pages.getNumberOfElements());
    }

    return null;
  }

  @Override
  public TeamAbstractConfiguration createParameter(String teamId,
      TeamAbstractConfiguration parameter) {
    List<String> teamRefs = relationshipService.getFilteredRefs(RelationshipRefType.TEAM,
        Optional.empty(), Optional.empty(), Optional.empty());
    if (!teamRefs.contains(teamId)) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    Optional<TeamEntity> optTeamEntity = teamRepository.findById(teamId);
    if (optTeamEntity.isPresent()) {
      TeamEntity teamEntity = optTeamEntity.get();
      if (teamEntity.getSettings() == null) {
        teamEntity.setSettings(new TeamSettings());
      }
      List<TeamAbstractConfiguration> configItems = teamEntity.getSettings().getParameters();

      String newUuid = UUID.randomUUID().toString();
      parameter.setId(newUuid);
      configItems.add(parameter);
      teamRepository.save(teamEntity);
      // If the parameter is a password, do not return its value, for security reasons.
      filterValueByFieldType(List.of(parameter), false, FieldType.PASSWORD.value());
    }

    return parameter;
  }

  @Override
  public ResponseEntity<Void> enable(String teamId) {
    if (teamId == null || teamId.isBlank()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }

    List<String> teamRefs = relationshipService.getFilteredRefs(RelationshipRefType.TEAM,
        Optional.empty(), Optional.empty(), Optional.empty());
    if (!teamRefs.contains(teamId)) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }

    Optional<TeamEntity> entity = teamRepository.findById(teamId);
    if (!entity.isPresent()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }

    entity.get().setStatus(TeamStatus.active);
    teamRepository.save(entity.get());
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> disable(String teamId) {
    if (teamId == null || teamId.isBlank()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }

    List<String> teamRefs = relationshipService.getFilteredRefs(RelationshipRefType.TEAM,
        Optional.empty(), Optional.empty(), Optional.empty());
    if (!teamRefs.contains(teamId)) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }

    Optional<TeamEntity> entity = teamRepository.findById(teamId);
    if (!entity.isPresent()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }

    entity.get().setStatus(TeamStatus.inactive);
    teamRepository.save(entity.get());
    return ResponseEntity.noContent().build();
  }

  //
  // @Override
  // public void deleteTeamProperty(String teamId, String configurationId) {
  // TeamEntity flowTeamEntity = flowTeamService.findById(teamId);
  //
  // if (flowTeamEntity.getSettings().getProperties() != null) {
  // List<FlowTeamConfiguration> configItems = flowTeamEntity.getSettings().getProperties();
  // FlowTeamConfiguration item = configItems.stream()
  // .filter(config -> configurationId.equals(config.getId())).findAny().orElse(null);
  //
  // if (item != null) {
  // configItems.remove(item);
  // }
  // flowTeamEntity.getSettings().setProperties(configItems);
  // flowTeamService.save(flowTeamEntity);
  // }
  // }
  //
  // @Override
  // public List<FlowTeamConfiguration> getAllTeamProperties(String teamId) {
  // TeamEntity flowTeamEntity = flowTeamService.findById(teamId);
  //
  // if (flowTeamEntity.getSettings() != null
  // && flowTeamEntity.getSettings().getProperties() != null) {
  // filterValueByFieldType(flowTeamEntity.getSettings() == null ?
  // null:flowTeamEntity.getSettings().getProperties(), false, FieldType.PASSWORD.value());
  // return flowTeamEntity.getSettings().getProperties();
  // } else {
  // return Collections.emptyList();
  // }
  // }
  //

  // private List<ActivityEntity> getConcurrentWorkflowActivities(String teamId) {
  // List<WorkflowEntity> teamWorkflows = flowWorkflowService.getWorkflowsForTeam(teamId);
  // List<String> workflowIds = new ArrayList<>();
  // for (WorkflowEntity workflow : teamWorkflows) {
  // workflowIds.add(workflow.getId());
  // }
  // return flowWorkflowActivityService.findbyWorkflowIdsAndStatus(workflowIds,
  // TaskStatus.inProgress);
  // }
  //
  // private Map<String, List<ActivityEntity>> getConcurrentWorkflowActivities(List<String> teamIds)
  // {
  // List<ActivityEntity> activities = flowWorkflowActivityService.findByTeamIdsAndStatus(teamIds,
  // TaskStatus.inProgress);
  // Map<String, List<ActivityEntity>> activitiesMap = Maps.newHashMap();
  // for(ActivityEntity activity: activities) {
  // if(activitiesMap.get(activity.getTeamId()) == null) {
  // activitiesMap.put(activity.getTeamId(), Lists.newArrayList());
  // }
  // activitiesMap.get(activity.getTeamId()).add(activity);
  // }
  // return activitiesMap;
  // }
  //
  // @Override
  // public Quotas getDefaultQuotas() {
  // Quotas quota = new Quotas();
  // quota.setMaxWorkflowCount(Integer
  // .valueOf(flowSettingsService.getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_COUNT).getValue()));
  // quota.setMaxWorkflowExecutionMonthly(Integer.valueOf(flowSettingsService
  // .getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_EXECUTION_MONTHLY).getValue()));
  // quota.setMaxWorkflowStorage(Integer.valueOf(flowSettingsService
  // .getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_STORAGE).getValue().replace("Gi", "")));
  // quota.setMaxWorkflowExecutionTime(Integer.valueOf(
  // flowSettingsService.getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_DURATION).getValue()));
  // quota.setMaxConcurrentWorkflows(Integer.valueOf(
  // flowSettingsService.getConfiguration(TEAMS, MAX_TEAM_CONCURRENT_WORKFLOW).getValue()));
  // return quota;
  //
  // }
  //
  // private List<ActivityEntity> getMonthlyWorkflowActivities(Pageable page, String teamId) {
  // Calendar c = Calendar.getInstance();
  // c.set(Calendar.DAY_OF_MONTH, 1);
  // return flowWorkflowActivityService
  // .findAllActivitiesForTeam(Optional.of(c.getTime()), Optional.of(new Date()), teamId, page)
  // .getContent();
  // }
  //
  // private Map<String, List<ActivityEntity>> getMonthlyWorkflowActivities(Pageable page,
  // List<String> teamIds) {
  // Calendar c = Calendar.getInstance();
  // c.set(Calendar.DAY_OF_MONTH, 1);
  // List<ActivityEntity> activities = flowWorkflowActivityService
  // .findAllActivitiesForTeams(c.getTime(), new Date(), teamIds, page)
  // .getContent();
  // Map<String, List<ActivityEntity>> activitiesMap = Maps.newHashMap();
  // for(ActivityEntity activity: activities) {
  // if(activitiesMap.get(activity.getTeamId()) == null) {
  // activitiesMap.put(activity.getTeamId(), Lists.newArrayList());
  // }
  // activitiesMap.get(activity.getTeamId()).add(activity);
  // }
  // return activitiesMap;
  // }
  //
  // @Override
  // public FlowTeam getTeamById(String teamId) {
  // if (!flowExternalUrlTeam.isBlank()) {
  // List<TeamEntity> allFlowteams =
  // this.externalTeamService.getExternalTeams(flowExternalUrlTeam);
  // if (allFlowteams != null) {
  // TeamEntity flowEntity =
  // allFlowteams.stream().filter(t -> teamId.equals(t.getId())).findFirst().orElse(null);
  //
  // FlowTeam flowTeam = new FlowTeam();
  // if (flowEntity != null) {
  // BeanUtils.copyProperties(flowEntity, flowTeam);
  // }
  // return flowTeam;
  // }
  //
  // } else {
  // TeamEntity flowEntity = flowTeamService.findById(teamId);
  // FlowTeam flowTeam = new FlowTeam();
  // if (flowEntity != null) {
  // BeanUtils.copyProperties(flowEntity, flowTeam);
  // }
  //
  // return flowTeam;
  // }
  // return null;
  // }
  //
  // @Override
  // public FlowTeam getTeamByIdDetailed(String teamId) {
  // TeamEntity flowEntity = flowTeamService.findById(teamId);
  // FlowTeam flowTeam = new FlowTeam();
  // if (flowEntity != null) {
  // BeanUtils.copyProperties(flowEntity, flowTeam);
  // }
  //
  // final List<WorkflowSummary> workflowSummary = workflowService.getWorkflowsForTeam(teamId);
  // flowTeam.setWorkflows(workflowSummary);
  //
  // List<String> teamIds = new LinkedList<>();
  // teamIds.add(teamId);
  //
  // List<FlowUserEntity> existingUsers = this.userIdentiyService.getUsersForTeams(teamIds);
  // convertToFlowUserList(flowTeam, existingUsers);
  //
  // return flowTeam;
  // }
  //
  // private void convertToFlowUserList(FlowTeam flowTeam, List<FlowUserEntity> existingUsers) {
  // List<FlowUser> users = new LinkedList<>();
  //
  // for (FlowUserEntity user : existingUsers) {
  // FlowUser flowUser = new FlowUser();
  // BeanUtils.copyProperties(user, flowUser);
  //
  // users.add(flowUser);
  // }
  // flowTeam.setUsers(users);
  // }
  //
  // @Override
  // public List<TeamWorkflowSummary> getTeamListing(FlowUserEntity userEntity) {
  //
  // List<TeamEntity> flowTeams = getUsersTeamListing(userEntity);
  // List<TeamWorkflowSummary> flowTeamListing = new LinkedList<>();
  //
  // if (flowTeams != null) {
  // for (TeamEntity team : flowTeams) {
  // TeamWorkflowSummary summary = new TeamWorkflowSummary(team, null);
  // flowTeamListing.add(summary);
  // }
  // }
  //
  // return flowTeamListing;
  // }
  //
  // @Override
  // public WorkflowQuotas getTeamQuotas(String teamId) {
  // TeamEntity team = flowTeamService.findById(teamId);
  //
  // if (team == null) {
  // WorkflowQuotas quotas = new WorkflowQuotas();
  // quotas.setMaxConcurrentWorkflows(Integer.MAX_VALUE);
  // quotas.setMaxWorkflowExecutionMonthly(Integer.MAX_VALUE);
  // quotas.setMaxWorkflowExecutionTime(Integer.MAX_VALUE);
  // quotas.setCurrentConcurrentWorkflows(0);
  // quotas.setCurrentWorkflowCount(0);
  // quotas.setCurrentWorkflowExecutionMonthly(0);
  // return quotas;
  // }
  //
  // List<WorkflowSummary> workflows = workflowService.getWorkflowsForTeam(team.getId());
  // Pageable page = Pageable.unpaged();
  // List<ActivityEntity> concurrentActivities = getConcurrentWorkflowActivities(teamId);
  // List<ActivityEntity> activitiesMonthly = getMonthlyWorkflowActivities(page, teamId);
  //
  // Quotas quotas = setTeamQuotas(team);
  //
  // team.setQuotas(quotas);
  // TeamEntity updatedTeam = this.flowTeamService.save(team);
  //
  // WorkflowQuotas workflowQuotas = new WorkflowQuotas();
  // workflowQuotas.setMaxWorkflowCount(updatedTeam.getQuotas().getMaxWorkflowCount());
  // workflowQuotas
  // .setMaxWorkflowExecutionMonthly(updatedTeam.getQuotas().getMaxWorkflowExecutionMonthly());
  // workflowQuotas.setMaxWorkflowStorage(updatedTeam.getQuotas().getMaxWorkflowStorage());
  // workflowQuotas
  // .setMaxWorkflowExecutionTime(updatedTeam.getQuotas().getMaxWorkflowExecutionTime());
  // workflowQuotas.setMaxConcurrentWorkflows(updatedTeam.getQuotas().getMaxConcurrentWorkflows());
  //
  // workflowQuotas.setCurrentWorkflowCount(workflows.size());
  // workflowQuotas.setCurrentConcurrentWorkflows(concurrentActivities.size());
  // workflowQuotas.setCurrentWorkflowExecutionMonthly(activitiesMonthly.size());
  // setWorkflowStorage(workflows, workflowQuotas);
  // setWorkflowResetDate(workflowQuotas);
  // return workflowQuotas;
  // }
  //
  @Override
  public List<TeamEntity> getUsersTeamListing(UserEntity userEntity) {
    /*
     * For the case that both flowExternalUrlUser and flowExternalUrlTeam are not configured 1. Get
     * team id list from flow user entity 2. query flow_teamcollection against "_id"
     */
    List<String> teamIds = userEntity.getFlowTeams();
    if (teamIds == null || teamIds.isEmpty()) {
      return Lists.newArrayList();
    }
    return teamRepository.findByIdInAndIsActive(teamIds, true);
  }
  //
  // @Override
  // public List<TeamWorkflowSummary> getUserTeams(FlowUserEntity userEntity) {
  //
  // List<TeamEntity> flowTeam = getUsersTeamListing(userEntity);
  //
  // final List<TeamWorkflowSummary> teamWorkFlowSummary =
  // populateWorkflowSummaryInformation(flowTeam);
  // return teamWorkFlowSummary;
  // }
  //
  // private List<TeamWorkflowSummary> populateWorkflowSummaryInformation(List<TeamEntity>
  // flowTeams) {
  // final List<TeamWorkflowSummary> teamWorkFlowSummary = new LinkedList<>();
  // if(flowTeams == null || flowTeams.isEmpty()) {
  // return teamWorkFlowSummary;
  // }
  // // Collect Team ID.
  // List<String> flowTeamIds = flowTeams.stream().map(flowTeam -> flowTeam.getId())
  // .collect(Collectors.toList());
  //
  // // Batch query workflow summaries map(key=flowTeamId, value=List<WorkflowSummary>) for a list
  // of teams
  // Map<String, List<WorkflowSummary>> workflowSummaryMap =
  // workflowService.getWorkflowsForTeams(flowTeamIds);
  // for (TeamEntity teamEntity : flowTeams) {
  // teamWorkFlowSummary.add(new TeamWorkflowSummary(teamEntity,
  // workflowSummaryMap.get(teamEntity.getId())));
  // }
  //
  // updateTeamWorkflowSummaryWithQuotas(teamWorkFlowSummary);
  // updateTeamWorkflowSummaryWithUpgradeFlags(teamWorkFlowSummary);
  // return teamWorkFlowSummary;
  // }
  //
  // @Override
  // public WorkflowQuotas resetTeamQuotas(String teamId) {
  // TeamEntity team = flowTeamService.findById(teamId);
  // List<WorkflowSummary> workflows = workflowService.getWorkflowsForTeam(team.getId());
  // Pageable page = Pageable.unpaged();
  // List<ActivityEntity> concurrentActivities = getConcurrentWorkflowActivities(teamId);
  // List<ActivityEntity> activitiesMonthly = getMonthlyWorkflowActivities(page, teamId);
  //
  // Quotas teamQuotas = team.getQuotas();
  // teamQuotas.setMaxWorkflowCount(Integer
  // .valueOf(flowSettingsService.getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_COUNT).getValue()));
  // teamQuotas.setMaxWorkflowExecutionMonthly(Integer.valueOf(flowSettingsService
  // .getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_EXECUTION_MONTHLY).getValue()));
  // teamQuotas.setMaxWorkflowStorage(Integer.valueOf(flowSettingsService
  // .getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_STORAGE).getValue().replace("Gi", "")));
  // teamQuotas.setMaxWorkflowExecutionTime(Integer.valueOf(
  // flowSettingsService.getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_DURATION).getValue()));
  // teamQuotas.setMaxConcurrentWorkflows(Integer.valueOf(
  // flowSettingsService.getConfiguration(TEAMS, MAX_TEAM_CONCURRENT_WORKFLOW).getValue()));
  // TeamEntity updatedTeam = this.flowTeamService.save(team);
  //
  // WorkflowQuotas workflowQuotas = new WorkflowQuotas();
  // workflowQuotas.setMaxWorkflowCount(updatedTeam.getQuotas().getMaxWorkflowCount());
  // workflowQuotas
  // .setMaxWorkflowExecutionMonthly(updatedTeam.getQuotas().getMaxWorkflowExecutionMonthly());
  // workflowQuotas.setMaxWorkflowStorage(updatedTeam.getQuotas().getMaxWorkflowStorage());
  // workflowQuotas
  // .setMaxWorkflowExecutionTime(updatedTeam.getQuotas().getMaxWorkflowExecutionTime());
  // workflowQuotas.setMaxConcurrentWorkflows(updatedTeam.getQuotas().getMaxConcurrentWorkflows());
  // workflowQuotas.setCurrentWorkflowCount(workflows.size());
  // workflowQuotas.setCurrentConcurrentWorkflows(concurrentActivities.size());
  // workflowQuotas.setCurrentWorkflowExecutionMonthly(activitiesMonthly.size());
  // setWorkflowStorage(workflows, workflowQuotas);
  // setWorkflowResetDate(workflowQuotas);
  // return workflowQuotas;
  // }
  //
  // private Quotas setTeamQuotas(TeamEntity team) {
  // if (team.getQuotas() == null) {
  // team.setQuotas(new Quotas());
  // }
  //
  // Quotas quotas = new Quotas();
  //
  // if (team.getQuotas().getMaxWorkflowCount() != null) {
  // quotas.setMaxWorkflowCount(team.getQuotas().getMaxWorkflowCount());
  // } else {
  // quotas.setMaxWorkflowCount(Integer.valueOf(
  // flowSettingsService.getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_COUNT).getValue()));
  // }
  // if (team.getQuotas().getMaxWorkflowExecutionMonthly() != null) {
  // quotas.setMaxWorkflowExecutionMonthly(team.getQuotas().getMaxWorkflowExecutionMonthly());
  // } else {
  // quotas.setMaxWorkflowExecutionMonthly(Integer.valueOf(flowSettingsService
  // .getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_EXECUTION_MONTHLY).getValue()));
  // }
  // if (team.getQuotas().getMaxWorkflowStorage() != null) {
  // quotas.setMaxWorkflowStorage(team.getQuotas().getMaxWorkflowStorage());
  // } else {
  // quotas.setMaxWorkflowStorage(Integer.valueOf(flowSettingsService
  // .getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_STORAGE).getValue().replace("Gi", "")));
  // }
  // if (team.getQuotas().getMaxWorkflowExecutionTime() != null) {
  // quotas.setMaxWorkflowExecutionTime(team.getQuotas().getMaxWorkflowExecutionTime());
  // } else {
  // quotas.setMaxWorkflowExecutionTime(Integer.valueOf(
  // flowSettingsService.getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_DURATION).getValue()));
  // }
  // if (team.getQuotas().getMaxConcurrentWorkflows() != null) {
  // quotas.setMaxConcurrentWorkflows(team.getQuotas().getMaxConcurrentWorkflows());
  // } else {
  // quotas.setMaxConcurrentWorkflows(Integer.valueOf(
  // flowSettingsService.getConfiguration(TEAMS, MAX_TEAM_CONCURRENT_WORKFLOW).getValue()));
  // }
  // return quotas;
  // }
  //
  // private void setWorkflowResetDate(WorkflowQuotas workflowQuotas) {
  // Calendar nextMonth = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
  // nextMonth.add(Calendar.MONTH, 1);
  // nextMonth.set(Calendar.DAY_OF_MONTH, 1);
  // nextMonth.set(Calendar.HOUR_OF_DAY, 0);
  // nextMonth.set(Calendar.MINUTE, 0);
  // nextMonth.set(Calendar.SECOND, 0);
  // nextMonth.set(Calendar.MILLISECOND, 0);
  // workflowQuotas.setMonthlyResetDate(nextMonth.getTime());
  // }
  //
  // private void setWorkflowStorage(List<WorkflowSummary> workflows, WorkflowQuotas workflowQuotas)
  // {
  // Integer currentWorkflowsPersistentStorage = 0;
  // if(workflows != null) {
  // for (WorkflowSummary workflow : workflows) {
  // if (workflow.getStorage() == null) {
  // workflow.setStorage(new Storage());
  // }
  // if (workflow.getStorage().getActivity() == null) {
  // workflow.getStorage().setActivity(new ActivityStorage());
  // }
  //
  // if (workflow.getStorage().getActivity().getEnabled()) {
  // currentWorkflowsPersistentStorage += 1;
  // }
  // }
  // }
  // workflowQuotas.setCurrentWorkflowsPersistentStorage(currentWorkflowsPersistentStorage);
  // }
  //
  // @Override
  // public Quotas updateQuotasForTeam(String teamId, Quotas quotas) {
  // TeamEntity team = flowTeamService.findById(teamId);
  // team.setQuotas(quotas);
  // return flowTeamService.save(team).getQuotas();
  // }
  //
  // private void updateTeamWorkflowSummaryWithQuotas(List<TeamWorkflowSummary>
  // teamWorkflowSummaryList){
  // setTeamQuotas(teamWorkflowSummaryList);
  // setWorkflowQuotas(teamWorkflowSummaryList);
  // }
  //
  // private void setTeamQuotas(List<Team> teams) {
  // int maxWorkflowCount = Integer.valueOf(settingsService
  // .getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_COUNT).getValue());
  // int maxTeamFlowExecutionMonthly = Integer.valueOf(settingsService
  // .getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_EXECUTION_MONTHLY).getValue());
  // int maxTeamWorkflowStorageInGB = Integer.valueOf(settingsService
  // .getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_STORAGE).getValue().replace("Gi", ""));
  // int maxTeamWorkflowDuration = Integer.valueOf(settingsService
  // .getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_DURATION).getValue());
  // int maxTeamWorkflowConcurrent = Integer.valueOf(settingsService
  // .getConfiguration(TEAMS, MAX_TEAM_CONCURRENT_WORKFLOW).getValue());
  // for(Team team: teams) {
  // Quotas quotas = new Quotas();
  // if (team.getQuotas() != null && team.getQuotas().getMaxWorkflowCount() != null) {
  // quotas.setMaxWorkflowCount(team.getQuotas().getMaxWorkflowCount());
  // } else {
  // quotas.setMaxWorkflowCount(maxWorkflowCount);
  // }
  // if (team.getQuotas() != null && team.getQuotas().getMaxWorkflowExecutionMonthly() != null) {
  // quotas.setMaxWorkflowExecutionMonthly(team.getQuotas().getMaxWorkflowExecutionMonthly());
  // } else {
  // quotas.setMaxWorkflowExecutionMonthly(maxTeamFlowExecutionMonthly);
  // }
  // if (team.getQuotas() != null && team.getQuotas().getMaxWorkflowStorage() != null) {
  // quotas.setMaxWorkflowStorage(team.getQuotas().getMaxWorkflowStorage());
  // } else {
  // quotas.setMaxWorkflowStorage(maxTeamWorkflowStorageInGB);
  // }
  // if (team.getQuotas() != null && team.getQuotas().getMaxWorkflowExecutionTime() != null) {
  // quotas.setMaxWorkflowExecutionTime(team.getQuotas().getMaxWorkflowExecutionTime());
  // } else {
  // quotas.setMaxWorkflowExecutionTime(maxTeamWorkflowDuration);
  // }
  // if (team.getQuotas() != null && team.getQuotas().getMaxConcurrentWorkflows() != null) {
  // quotas.setMaxConcurrentWorkflows(team.getQuotas().getMaxConcurrentWorkflows());
  // } else {
  // quotas.setMaxConcurrentWorkflows(maxTeamWorkflowConcurrent);
  // }
  // team.setQuotas(quotas);
  // }
  // }
  //
  // private void setWorkflowQuotas(List<Team> teams) {
  // List<String> teamIds = teams.stream().map(team -> team.getId())
  // .collect(Collectors.toList());
  // Map<String, List<ActivityEntity>> concurrentActivitiesMap =
  // getConcurrentWorkflowActivities(teamIds);
  // Map<String, List<ActivityEntity>> monthlyActivitiesMap =
  // getMonthlyWorkflowActivities(Pageable.unpaged(), teamIds);
  // for(TeamWorkflowSummary teamWorkFlow: teamWorkflowSummaryList) {
  // WorkflowQuotas workflowQuotas = new WorkflowQuotas();
  // Quotas quotas = teamWorkFlow.getQuotas();
  // List<ActivityEntity> concurrentActivities = concurrentActivitiesMap.get(teamWorkFlow.getId());
  // List<ActivityEntity> monthlyActivities = monthlyActivitiesMap.get(teamWorkFlow.getId());
  // workflowQuotas.setMaxWorkflowCount(quotas.getMaxWorkflowCount());
  // workflowQuotas.setMaxWorkflowExecutionMonthly(quotas.getMaxWorkflowExecutionMonthly());
  // workflowQuotas.setMaxWorkflowStorage(quotas.getMaxWorkflowStorage());
  // workflowQuotas.setMaxWorkflowExecutionTime(quotas.getMaxWorkflowExecutionTime());
  // workflowQuotas.setMaxConcurrentWorkflows(quotas.getMaxConcurrentWorkflows());
  //
  // workflowQuotas.setCurrentWorkflowCount(teamWorkFlow.getWorkflows() == null?
  // 0:teamWorkFlow.getWorkflows().size());
  // workflowQuotas.setCurrentConcurrentWorkflows(concurrentActivities == null? 0 :
  // concurrentActivities.size());
  // workflowQuotas.setCurrentWorkflowExecutionMonthly(monthlyActivities == null? 0 :
  // monthlyActivities.size());
  // setWorkflowStorage(teamWorkFlow.getWorkflows(), workflowQuotas);
  // setWorkflowResetDate(workflowQuotas);
  // teamWorkFlow.setWorkflowQuotas(workflowQuotas);
  // }
  // }
  //
  // private void updateTeamWorkflowSummaryWithUpgradeFlags(List<TeamWorkflowSummary>
  // teamWorkflowSummaries) {
  // // Collect workflow IDs.
  // List<String> workflowIds = new ArrayList<String>();
  // for(TeamWorkflowSummary teamWorkflowSummary: teamWorkflowSummaries) {
  // if (teamWorkflowSummary.getWorkflows() == null) {
  // continue;
  // }
  // teamWorkflowSummary.getWorkflows().stream().forEach(workflow ->{
  // workflowIds.add(workflow.getId());
  // });
  // }
  //
  // // Batch query latest workflow revisions with upgrade flags updated.
  // List<FlowWorkflowRevision> latestRevisions =
  // workflowVersionService.getLatestWorkflowVersionWithUpgradeFlags(workflowIds);
  // Map<String, FlowWorkflowRevision> latestRevisionsMap = Maps.newHashMap();
  // latestRevisions.stream().forEach(latestFlowRevision->{
  // latestRevisionsMap.put(latestFlowRevision.getWorkFlowId(), latestFlowRevision);
  // });
  //
  // // Set TemplateUpgradesAvailable of team flow summary with the value of its latest revision.
  // for(TeamWorkflowSummary flowSummary: teamWorkflowSummaries) {
  // if (flowSummary.getWorkflows() == null) {
  // continue;
  // }
  // flowSummary.getWorkflows().stream().forEach(workflow ->{
  // if(latestRevisionsMap.get(workflow.getId()) != null) {
  // workflow.setTemplateUpgradesAvailable(latestRevisionsMap.get(workflow.getId()).isTemplateUpgradesAvailable());
  // }
  // });
  // }
  // }
  //
  // @Override
  // public void updateSummaryWithUpgradeFlags(List<WorkflowSummary> workflowSummary) {
  //
  // for (WorkflowSummary summary : workflowSummary) {
  // String workflowId = summary.getId();
  // FlowWorkflowRevision latestRevision =
  // workflowVersionService.getLatestWorkflowVersion(workflowId);
  // if (latestRevision != null) {
  // summary.setTemplateUpgradesAvailable(latestRevision.isTemplateUpgradesAvailable());
  // }
  // }
  //
  // }
  //
  // @Override
  // public void updateTeam(String teamId, FlowTeam flow) {
  // validateUser();
  // TeamEntity team = flowTeamService.findById(teamId);
  // if (flow.getName() != null) {
  // team.setName(flow.getName());
  // }
  // if (flow.getIsActive() != null) {
  // team.setIsActive(flow.getIsActive());
  // }
  //
  // this.flowTeamService.save(team);
  // }
  //
  // @Override
  // public void updateTeamMembers(String teamId, List<String> teamMembers) {
  // List<String> teamIds = new LinkedList<>();
  // teamIds.add(teamId);
  // List<FlowUserEntity> existingUsers = this.userIdentiyService.getUsersForTeams(teamIds);
  //
  // List<String> existingTeamIds =
  // existingUsers.stream().map(FlowUserEntity::getId).collect(Collectors.toList());
  //
  // List<String> removeUsers = existingUsers.stream().map(FlowUserEntity::getId)
  // .filter(f -> !teamMembers.contains(f)).collect(Collectors.toList());
  //
  // List<String> addUsers =
  // teamMembers.stream().filter(f -> !existingTeamIds.contains(f)).collect(Collectors.toList());
  //
  // for (String userId : addUsers) {
  // Optional<FlowUserEntity> userEntity = flowUserService.getUserById(userId);
  // if (userEntity.isPresent()) {
  // FlowUserEntity flowUser = userEntity.get();
  // if (flowUser.getFlowTeams() == null) {
  // flowUser.setFlowTeams(new LinkedList<>());
  // }
  // flowUser.getFlowTeams().add(teamId);
  // this.flowUserService.save(flowUser);
  // }
  // }
  //
  // for (String userId : removeUsers) {
  // Optional<FlowUserEntity> userEntity = flowUserService.getUserById(userId);
  // if (userEntity.isPresent()) {
  // FlowUserEntity flowUser = userEntity.get();
  // if (flowUser.getFlowTeams() == null) {
  // flowUser.setFlowTeams(new LinkedList<>());
  // }
  // flowUser.getFlowTeams().remove(teamId);
  // this.flowUserService.save(flowUser);
  // }
  // }
  // }
  //
  //
  // @Override
  // public List<FlowTeamConfiguration> updateTeamProperty(String teamId,
  // FlowTeamConfiguration property) {
  // TeamEntity flowTeamEntity = flowTeamService.findById(teamId);
  //
  // if (flowTeamEntity.getSettings().getProperties() != null) {
  // List<FlowTeamConfiguration> configItems = flowTeamEntity.getSettings().getProperties();
  // String existingId = property.getId();
  // FlowTeamConfiguration item = configItems.stream()
  // .filter(config -> existingId.equals(config.getId())).findAny().orElse(null);
  //
  // if (item != null) {
  // configItems.remove(item);
  // configItems.add(property);
  // }
  // flowTeamEntity.getSettings().setProperties(configItems);
  // flowTeamService.save(flowTeamEntity);
  //
  // return configItems;
  // }
  //
  // return Collections.emptyList();
  // }
  //
  // @Override
  // public Quotas updateTeamQuotas(String teamId, Quotas quotas) {
  // TeamEntity team = flowTeamService.findById(teamId);
  //
  // if (team.getQuotas() == null) {
  // team.setQuotas(new Quotas());
  // }
  // if (quotas.getMaxWorkflowCount() != null) {
  // team.getQuotas().setMaxWorkflowCount(quotas.getMaxWorkflowCount());
  // }
  // if (quotas.getMaxConcurrentWorkflows() != null) {
  // team.getQuotas().setMaxConcurrentWorkflows(quotas.getMaxConcurrentWorkflows());
  // }
  // if (quotas.getMaxWorkflowExecutionMonthly() != null) {
  // team.getQuotas().setMaxWorkflowExecutionMonthly(quotas.getMaxWorkflowExecutionMonthly());
  // }
  // if (quotas.getMaxWorkflowExecutionTime() != null) {
  // team.getQuotas().setMaxWorkflowExecutionTime(quotas.getMaxWorkflowExecutionTime());
  // }
  // if (quotas.getMaxWorkflowStorage() != null) {
  // team.getQuotas().setMaxWorkflowStorage(quotas.getMaxWorkflowStorage());
  // }
  //
  // return flowTeamService.save(team).getQuotas();
  // }
  //
  // protected void validateUser() {
  //
  // FlowUserEntity userEntity = userIdentiyService.getCurrentUser();
  // if (userEntity == null || (!userEntity.getType().equals(UserType.admin)
  // && !userEntity.getType().equals(UserType.operator))) {
  //
  // throw new HttpClientErrorException(HttpStatus.FORBIDDEN);
  // }
  // }
  //
  // @Override
  // public List<TeamMember> getTeamMembers(String teamId) {
  // List<TeamMember> members = new LinkedList<>();
  // if (!flowExternalUrlUser.isBlank()) {
  // TeamEntity flowTeam = this.flowTeamService.findById(teamId);
  // String externalTeamId = flowTeam.getHigherLevelGroupId();
  // List<FlowUserEntity> flowUsers =
  // this.externalTeamService.getExternalTeamMemberListing(externalTeamId);
  // mapToTeamMemberList(members, flowUsers);
  // } else {
  // List<String> teamIds = new LinkedList<>();
  // teamIds.add(teamId);
  // List<FlowUserEntity> existingUsers = this.userIdentiyService.getUsersForTeams(teamIds);
  // mapToTeamMemberList(members, existingUsers);
  // }
  // return members;
  // }
  //
  // private void mapToTeamMemberList(List<TeamMember> members, List<FlowUserEntity> existingUsers)
  // {
  // for (FlowUserEntity u : existingUsers) {
  // TeamMember user = new TeamMember();
  // user.setEmail(u.getEmail());
  // user.setUserName(u.getName());
  // user.setUserId(u.getId());
  // members.add(user);
  // }
  // }
  //
  // @Override
  // public void deleteApproverGroup(String teamId, String groupId) {
  // TeamEntity team = flowTeamService.findById(teamId);
  //
  // if (team != null) {
  // if (team.getApproverGroups() == null) {
  // team.setApproverGroups(new LinkedList<ApproverGroup>());
  // }
  // ApproverGroup deletedGroup = team.getApproverGroups().stream()
  // .filter(x -> groupId.equals(x.getId())).findFirst().orElse(null);
  // if (deletedGroup != null) {
  // team.getApproverGroups().remove(deletedGroup);
  // }
  // flowTeamService.save(team);
  // }
  // }
  //
  // @Override
  // public List<ApproverGroupResponse> getTeamApproverGroups(String teamId) {
  // TeamEntity team = flowTeamService.findById(teamId);
  // List<ApproverGroupResponse> response = new LinkedList<>();
  //
  // if (team != null && team.getApproverGroups() != null) {
  //
  // for (ApproverGroup group : team.getApproverGroups()) {
  // ApproverGroupResponse approverGroupResponse = this.createApproverGroupResponse(group, team);
  // response.add(approverGroupResponse);
  // }
  // }
  // return response;
  // }
  //
  // @Override
  // public ApproverGroupResponse createApproverGroup(String teamId,
  // CreateApproverGroupRequest createApproverGroupRequest) {
  // String newGroupId = UUID.randomUUID().toString();
  // ApproverGroup group = new ApproverGroup();
  // group.setId(newGroupId);
  // group.setName(createApproverGroupRequest.getGroupName());
  //
  // if (createApproverGroupRequest.getApprovers() != null) {
  // List<ApproverUser> users = new LinkedList<>();
  // for (ApproverUser user : createApproverGroupRequest.getApprovers()) {
  // ApproverUser newUser = new ApproverUser();
  // newUser.setUserId(user.getUserId());
  //
  // users.add(newUser);
  // }
  // group.setApprovers(users);
  // }
  //
  // TeamEntity team = flowTeamService.findById(teamId);
  //
  // if (team != null) {
  // if (team.getApproverGroups() == null) {
  // team.setApproverGroups(new LinkedList<ApproverGroup>());
  // }
  // List<ApproverGroup> approverGroups = team.getApproverGroups();
  // approverGroups.add(group);
  // flowTeamService.save(team);
  // return this.createApproverGroupResponse(group, team);
  // }
  // return null;
  // }
  //
  // private ApproverGroupResponse createApproverGroupResponse(ApproverGroup group, TeamEntity team)
  // {
  //
  // ApproverGroupResponse approverGroupResponse = new ApproverGroupResponse();
  // approverGroupResponse.setGroupId(group.getId());
  // approverGroupResponse.setGroupName(group.getName());
  // approverGroupResponse.setTeamId(team.getId());
  // approverGroupResponse.setTeamName(team.getName());
  //
  // if (group.getApprovers() == null) {
  // group.setApprovers(new LinkedList<>());
  // }
  //
  // approverGroupResponse.setApprovers(group.getApprovers());
  //
  // for (ApproverUser approverUser : group.getApprovers()) {
  // FlowUserEntity flowUser = this.userIdentiyService.getUserByID(approverUser.getUserId());
  // approverUser.setUserEmail(flowUser.getEmail());
  // approverUser.setUserName(flowUser.getName());
  // }
  //
  // return approverGroupResponse;
  // }
  //
  // @Override
  // public ApproverGroupResponse updateApproverGroup(String teamId, String groupId,
  // CreateApproverGroupRequest updatedRequest) {
  // TeamEntity team = flowTeamService.findById(teamId);
  //
  // if (team != null) {
  // if (team.getApproverGroups() == null) {
  // team.setApproverGroups(new LinkedList<ApproverGroup>());
  // }
  // ApproverGroup updatedGroup = team.getApproverGroups().stream()
  // .filter(x -> groupId.equals(x.getId())).findFirst().orElse(null);
  // if (updatedGroup != null) {
  // if (updatedRequest.getGroupName() != null) {
  // updatedGroup.setName(updatedRequest.getGroupName());
  // }
  // if (updatedRequest.getApprovers() != null) {
  // List<ApproverUser> users = new LinkedList<>();
  // for (ApproverUser user : updatedRequest.getApprovers()) {
  // ApproverUser newUser = new ApproverUser();
  // newUser.setUserId(user.getUserId());
  // users.add(newUser);
  // }
  // updatedGroup.setApprovers(users);
  // }
  // flowTeamService.save(team);
  // return this.createApproverGroupResponse(updatedGroup, team);
  // }
  // }
  // return null;
  // }
  //
  // @Override
  // public ApproverGroupResponse getSingleAproverGroup(String teamId, String groupId) {
  // TeamEntity team = flowTeamService.findById(teamId);
  // if (team != null) {
  // if (team.getApproverGroups() == null) {
  // team.setApproverGroups(new LinkedList<ApproverGroup>());
  // }
  // ApproverGroup group = team.getApproverGroups().stream().filter(x -> groupId.equals(x.getId()))
  // .findFirst().orElse(null);
  // if (group != null) {
  // return this.createApproverGroupResponse(group, team);
  // }
  // }
  // return null;
  // }
  //
  // @Override
  // public TeamEntity updateTeamLabels(String teamId, List<KeyValuePair> labels) {
  // TeamEntity team = flowTeamService.findById(teamId);
  // team.setLabels(labels);
  // return flowTeamService.save(team);
  // }

}
