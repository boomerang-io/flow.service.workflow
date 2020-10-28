package net.boomerangplatform.service.crud;

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
import org.springframework.stereotype.Service;
import net.boomerangplatform.client.BoomerangTeamService;
import net.boomerangplatform.client.BoomerangUserService;
import net.boomerangplatform.client.model.Team;
import net.boomerangplatform.client.model.UserProfile;
import net.boomerangplatform.model.FlowTeam;
import net.boomerangplatform.model.FlowWorkflowRevision;
import net.boomerangplatform.model.TeamQueryResult;
import net.boomerangplatform.model.TeamWorkflowSummary;
import net.boomerangplatform.model.WorkflowQuotas;
import net.boomerangplatform.model.WorkflowSummary;
import net.boomerangplatform.mongo.entity.ActivityEntity;
import net.boomerangplatform.mongo.entity.FlowTeamConfiguration;
import net.boomerangplatform.mongo.entity.FlowTeamEntity;
import net.boomerangplatform.mongo.entity.FlowUserEntity;
import net.boomerangplatform.mongo.entity.WorkflowEntity;
import net.boomerangplatform.mongo.model.Quotas;
import net.boomerangplatform.mongo.model.Settings;
import net.boomerangplatform.mongo.model.TaskStatus;
import net.boomerangplatform.mongo.service.FlowTeamService;
import net.boomerangplatform.mongo.service.FlowUserService;
import net.boomerangplatform.mongo.service.FlowWorkflowActivityService;
import net.boomerangplatform.mongo.service.FlowWorkflowService;
import net.boomerangplatform.service.UserIdentityService;

@Service
public class TeamServiceImpl implements TeamService {

  @Autowired
  private FlowTeamService flowTeamService;

  @Autowired
  private WorkflowService workflowService;

  @Autowired
  private WorkflowVersionService workflowVersionService;

  @Autowired
  private BoomerangTeamService boomerangTeamService;

  @Autowired
  private BoomerangUserService boomerangUserService;

  @Autowired
  private UserIdentityService userIdentiyService;
  
  @Autowired
  private FlowWorkflowActivityService flowWorkflowActivityService;
  
  @Autowired
  private FlowWorkflowService flowWorkflowService;

  @Value("${boomerang.standalone}")
  private boolean standAloneMode;
  
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

  @Autowired
  private FlowUserService flowUserService;

  @Override
  public FlowTeam createStandaloneTeam(String name) {
    FlowTeamEntity flowTeamEntity = new FlowTeamEntity();
    flowTeamEntity.setName(name);
    flowTeamEntity = flowTeamService.save(flowTeamEntity);
    flowTeamEntity.setHigherLevelGroupId(flowTeamEntity.getId());
    flowTeamEntity.setIsActive(true);
    if(flowTeamEntity.getQuotas() == null) {
      Quotas quotas = new Quotas();
      quotas.setMaxWorkflowCount(maxWorkflowCount);
      quotas.setMaxWorkflowExecutionMonthly(maxWorkflowExecutionMonthly);
      quotas.setMaxWorkflowStorage(maxWorkflowStorage);
      quotas.setMaxWorkflowExecutionTime(maxWorkflowExecutionTime);
      quotas.setMaxConcurrentWorkflows(maxConcurrentWorkflows);
      flowTeamEntity.setQuotas(quotas);
    }
    flowTeamEntity = flowTeamService.save(flowTeamEntity);
    
    FlowTeam team = new FlowTeam();

    BeanUtils.copyProperties(flowTeamEntity, team);

    return team;
  }

  @Override
  public void createFlowTeam(String higherLevelGroupId, String teamName) {


    final FlowTeamEntity flowTeamEntity = new FlowTeamEntity();
    flowTeamEntity.setName(teamName);
    flowTeamEntity.setHigherLevelGroupId(higherLevelGroupId);
    
    flowTeamEntity.setIsActive(true);
    if(flowTeamEntity.getQuotas() == null) {
      Quotas quotas = new Quotas();
      quotas.setMaxWorkflowCount(maxWorkflowCount);
      quotas.setMaxWorkflowExecutionMonthly(maxWorkflowExecutionMonthly);
      quotas.setMaxWorkflowStorage(maxWorkflowStorage);
      quotas.setMaxWorkflowExecutionTime(maxWorkflowExecutionTime);
      quotas.setMaxConcurrentWorkflows(maxConcurrentWorkflows);
      flowTeamEntity.setQuotas(quotas);
    }
    flowTeamService.save(flowTeamEntity);
  }

  @Override
  public List<TeamWorkflowSummary> getAllTeams() {
    final List<TeamWorkflowSummary> teamWorkFlowSummary = new LinkedList<>();
    final Page<FlowTeamEntity> flowTeams = flowTeamService.findAllTeams(Pageable.unpaged());
    for (final FlowTeamEntity entity : flowTeams.getContent()) {
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
  public List<TeamWorkflowSummary> getUserTeams(FlowUserEntity userEntity) {

    List<String> highLevelGroupIds = new LinkedList<>();
    if (standAloneMode) {
      highLevelGroupIds = userEntity.getFlowTeams();
    } else {
      UserProfile profile = boomerangUserService.getInternalUserProfile();
      List<Team> teams = profile.getTeams();
      if (teams != null) {
        highLevelGroupIds = teams.stream().map(Team::getId).collect(Collectors.toList());
      }
    }

    final List<TeamWorkflowSummary> teamWorkFlowSummary = new LinkedList<>();
    final List<FlowTeamEntity> flowTeam =
        flowTeamService.findTeamsWithHighLevelGroups(highLevelGroupIds);
    for (final FlowTeamEntity entity : flowTeam) {
      final List<WorkflowSummary> workflowSummary =
          workflowService.getWorkflowsForTeam(entity.getId());
      final TeamWorkflowSummary teamWorkFlow = new TeamWorkflowSummary(entity, workflowSummary);
      updateSummaryWithUpgradeFlags(teamWorkFlow);
      updateSummaryWithQuotas(entity, workflowSummary, teamWorkFlow);
      teamWorkFlowSummary.add(teamWorkFlow);
    }
    return teamWorkFlowSummary;
  }

  private void updateSummaryWithQuotas(final FlowTeamEntity entity,
      final List<WorkflowSummary> workflowSummary, final TeamWorkflowSummary teamWorkFlow) {

    Quotas quotas = setTeamQuotas(entity);
    
    teamWorkFlow.setQuotas(quotas);
    
    List<ActivityEntity> concurrentActivities = getConcurrentWorkflowActivities(entity.getId());
    Pageable page = Pageable.unpaged();
    Page<ActivityEntity> activitiesMonthly = getMonthlyWorkflowActivities(page);
    
    WorkflowQuotas workflowQuotas = new WorkflowQuotas();
    workflowQuotas.setMaxWorkflowCount(quotas.getMaxWorkflowCount());
    workflowQuotas.setMaxWorkflowExecutionMonthly(quotas.getMaxWorkflowExecutionMonthly());
    workflowQuotas.setMaxWorkflowStorage(quotas.getMaxWorkflowStorage());
    workflowQuotas.setMaxWorkflowExecutionTime(quotas.getMaxWorkflowExecutionTime());
    workflowQuotas.setMaxConcurrentWorkflows(quotas.getMaxConcurrentWorkflows());
    
    workflowQuotas.setCurrentWorkflowCount(workflowSummary.size());
    workflowQuotas.setCurrentConcurrentWorkflows(concurrentActivities.size());
    workflowQuotas.setCurrentWorkflowExecutionMonthly(activitiesMonthly.getContent().size());
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
  public List<FlowTeamConfiguration> getAllTeamProperties(String teamId) {
    FlowTeamEntity flowTeamEntity = flowTeamService.findById(teamId);

    if (flowTeamEntity.getSettings().getProperties() != null) {
      return flowTeamEntity.getSettings().getProperties();
    } else {
      return Collections.emptyList();
    }
  }

  @Override
  public void deleteTeamProperty(String teamId, String configurationId) {
    FlowTeamEntity flowTeamEntity = flowTeamService.findById(teamId);

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
  public List<FlowTeamConfiguration> updateTeamProperty(String teamId,
      FlowTeamConfiguration property) {
    FlowTeamEntity flowTeamEntity = flowTeamService.findById(teamId);

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
  public FlowTeamConfiguration createNewTeamProperty(String teamId,
      FlowTeamConfiguration property) {
    FlowTeamEntity flowTeamEntity = flowTeamService.findById(teamId);

    if (flowTeamEntity.getSettings() == null) {
      flowTeamEntity.setSettings(new Settings());
      flowTeamEntity = flowTeamService.save(flowTeamEntity);
    }

    if (flowTeamEntity.getSettings().getProperties() == null) {
      flowTeamEntity.getSettings().setProperties(new LinkedList<FlowTeamConfiguration>());
    }

    List<FlowTeamConfiguration> configItems = flowTeamEntity.getSettings().getProperties();
    String newUuid = UUID.randomUUID().toString();
    property.setId(newUuid);
    configItems.add(property);
    flowTeamService.save(flowTeamEntity);

    return property;
  }

  @Override
  public TeamQueryResult getAllAdminTeams(Pageable pageable) {

    final TeamQueryResult result = new TeamQueryResult();

    final Page<FlowTeamEntity> teamList = flowTeamService.findAllTeams(pageable);
    final List<FlowTeam> teams = new LinkedList<>();

    for (FlowTeamEntity team : teamList.getContent()) {
      FlowTeam flowTeam = createFlowTeam(team);

      teams.add(flowTeam);

    }
    result.setRecords(teams);
    result.setPageable(teamList);
    return result;
  }

  private FlowTeam createFlowTeam(FlowTeamEntity team) {
    FlowTeam flowTeam = new FlowTeam();
    BeanUtils.copyProperties(team, flowTeam);

    List<String> teamIds = new LinkedList<>();
    teamIds.add(flowTeam.getId());
    List<FlowUserEntity> users = this.userIdentiyService.getUsersForTeams(teamIds);
    flowTeam.setUsers(users);

    if (team.getIsActive() == null) {
      flowTeam.setIsActive(true);
    }

    final List<WorkflowSummary> workflows = workflowService.getWorkflowsForTeam(team.getId());
    flowTeam.setWorkflows(workflows);

    return flowTeam;
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
  public FlowTeam getTeamById(String teamId) {
    FlowTeamEntity flowEntity = flowTeamService.findById(teamId);
    FlowTeam flowTeam = new FlowTeam();
    if (flowEntity != null) {
      BeanUtils.copyProperties(flowEntity, flowTeam);
    }
    

    return flowTeam;
  }

  @Override
  public void updateTeam(String teamId, FlowTeam flow) {
    FlowTeamEntity team = flowTeamService.findById(teamId);
    if (flow.getName() != null) {
      team.setName(flow.getName());
    }
    if (flow.getIsActive() != null) {
      team.setIsActive(flow.getIsActive());
    }
    
    this.flowTeamService.save(team);
  }

  @Override
  public WorkflowQuotas getTeamQuotas(String teamId) {
    FlowTeamEntity team = flowTeamService.findById(teamId);
    List<WorkflowSummary> workflows = workflowService.getWorkflowsForTeam(team.getId());
    Pageable page = Pageable.unpaged();
    List<ActivityEntity> concurrentActivities = getConcurrentWorkflowActivities(teamId);
    Page<ActivityEntity> activitiesMonthly = getMonthlyWorkflowActivities(page);
    
    Quotas quotas = setTeamQuotas(team);
    
    team.setQuotas(quotas);
    FlowTeamEntity updatedTeam = this.flowTeamService.save(team);
    
    WorkflowQuotas workflowQuotas = new WorkflowQuotas();
    workflowQuotas.setMaxWorkflowCount(updatedTeam.getQuotas().getMaxWorkflowCount());
    workflowQuotas.setMaxWorkflowExecutionMonthly(updatedTeam.getQuotas().getMaxWorkflowExecutionMonthly());
    workflowQuotas.setMaxWorkflowStorage(updatedTeam.getQuotas().getMaxWorkflowStorage());
    workflowQuotas.setMaxWorkflowExecutionTime(updatedTeam.getQuotas().getMaxWorkflowExecutionTime());
    workflowQuotas.setMaxConcurrentWorkflows(updatedTeam.getQuotas().getMaxConcurrentWorkflows());
    
    workflowQuotas.setCurrentWorkflowCount(workflows.size());
    workflowQuotas.setCurrentConcurrentWorkflows(concurrentActivities.size());
    workflowQuotas.setCurrentWorkflowExecutionMonthly(activitiesMonthly.getContent().size());
    setWorkflowStorage(workflows, workflowQuotas);
    setWorkflowResetDate(workflowQuotas);
    return workflowQuotas;
  }

  private Quotas setTeamQuotas(FlowTeamEntity team) {
    if(team.getQuotas() == null) {
      team.setQuotas(new Quotas());
    }
    
    Quotas quotas = new Quotas();
    
    if(team.getQuotas().getMaxWorkflowCount() != null) {
      quotas.setMaxWorkflowCount(team.getQuotas().getMaxWorkflowCount());
    } else {
      quotas.setMaxWorkflowCount(maxWorkflowCount);
    }
    if(team.getQuotas().getMaxWorkflowExecutionMonthly() != null) {
      quotas.setMaxWorkflowExecutionMonthly(team.getQuotas().getMaxWorkflowExecutionMonthly());
    } else {
      quotas.setMaxWorkflowExecutionMonthly(maxWorkflowExecutionMonthly);
    }
    if(team.getQuotas().getMaxWorkflowStorage() != null) {
      quotas.setMaxWorkflowStorage(team.getQuotas().getMaxWorkflowStorage());
    } else {
      quotas.setMaxWorkflowStorage(maxWorkflowStorage);
    }
    if(team.getQuotas().getMaxWorkflowExecutionTime() != null) {
      quotas.setMaxWorkflowExecutionTime(team.getQuotas().getMaxWorkflowExecutionTime());
    } else {
      quotas.setMaxWorkflowExecutionTime(maxWorkflowExecutionTime);
    }
    if(team.getQuotas().getMaxConcurrentWorkflows() != null) {
      quotas.setMaxConcurrentWorkflows(team.getQuotas().getMaxConcurrentWorkflows());
    } else {
      quotas.setMaxConcurrentWorkflows(maxConcurrentWorkflows);
    }
    return quotas;
  }
  
  @Override
  public WorkflowQuotas resetTeamQuotas(String teamId) {
    FlowTeamEntity team = flowTeamService.findById(teamId);
    List<WorkflowSummary> workflows = workflowService.getWorkflowsForTeam(team.getId());
    Pageable page = Pageable.unpaged();
    List<ActivityEntity> concurrentActivities = getConcurrentWorkflowActivities(teamId);
    Page<ActivityEntity> activitiesMonthly = getMonthlyWorkflowActivities(page);
    
    Quotas teamQuotas = team.getQuotas();
    teamQuotas.setMaxWorkflowCount(maxWorkflowCount);
    teamQuotas.setMaxWorkflowExecutionMonthly(maxWorkflowExecutionMonthly);
    teamQuotas.setMaxWorkflowStorage(maxWorkflowStorage);
    teamQuotas.setMaxWorkflowExecutionTime(maxWorkflowExecutionTime);
    teamQuotas.setMaxConcurrentWorkflows(maxConcurrentWorkflows);
    FlowTeamEntity updatedTeam = this.flowTeamService.save(team);
    
    WorkflowQuotas workflowQuotas = new WorkflowQuotas();
    workflowQuotas.setMaxWorkflowCount(updatedTeam.getQuotas().getMaxWorkflowCount());
    workflowQuotas.setMaxWorkflowExecutionMonthly(updatedTeam.getQuotas().getMaxWorkflowExecutionMonthly());
    workflowQuotas.setMaxWorkflowStorage(updatedTeam.getQuotas().getMaxWorkflowStorage());
    workflowQuotas.setMaxWorkflowExecutionTime(updatedTeam.getQuotas().getMaxWorkflowExecutionTime());
    workflowQuotas.setMaxConcurrentWorkflows(updatedTeam.getQuotas().getMaxConcurrentWorkflows());
    workflowQuotas.setCurrentWorkflowCount(workflows.size());
    workflowQuotas.setCurrentConcurrentWorkflows(concurrentActivities.size());
    workflowQuotas.setCurrentWorkflowExecutionMonthly(activitiesMonthly.getContent().size());
    setWorkflowStorage(workflows, workflowQuotas);
    setWorkflowResetDate(workflowQuotas);
    return workflowQuotas;
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
    for(WorkflowSummary workflow : workflows) {
      if(workflow.isEnablePersistentStorage()) {
        currentWorkflowsPersistentStorage += 1;
      }
    }
    workflowQuotas.setCurrentWorkflowsPersistentStorage(currentWorkflowsPersistentStorage);
  }
  
  private Page<ActivityEntity> getMonthlyWorkflowActivities(Pageable page) {
    Calendar c = Calendar.getInstance();   
    c.set(Calendar.DAY_OF_MONTH, 1);
    return flowWorkflowActivityService.findAllActivities(
            Optional.of(c.getTime()), Optional.of(new Date()), page);
  }

  private List<ActivityEntity> getConcurrentWorkflowActivities(String teamId) {
    List<WorkflowEntity> teamWorkflows = flowWorkflowService.getWorkflowsForTeams(teamId);
    List<String> workflowIds = new ArrayList<>();
    for(WorkflowEntity workflow : teamWorkflows) {
      workflowIds.add(workflow.getId());
    }
    return flowWorkflowActivityService.findbyWorkflowIdsAndStatus(workflowIds, TaskStatus.inProgress);
  }

  @Override
  public Quotas updateTeamQuotas(String teamId, Quotas quotas) {
    FlowTeamEntity team = flowTeamService.findById(teamId);
    
    if(team.getQuotas() == null) {
      team.setQuotas(new Quotas());
    }
    if(quotas.getMaxWorkflowCount() != null) {
      team.getQuotas().setMaxWorkflowCount(quotas.getMaxWorkflowCount());
    }
    if(quotas.getMaxConcurrentWorkflows() != null) {
      team.getQuotas().setMaxConcurrentWorkflows(quotas.getMaxConcurrentWorkflows());
    }
    if(quotas.getMaxWorkflowExecutionMonthly() != null) {
      team.getQuotas().setMaxWorkflowExecutionMonthly(quotas.getMaxWorkflowExecutionMonthly());
    }
    if(quotas.getMaxWorkflowExecutionTime() != null) {
      team.getQuotas().setMaxWorkflowExecutionTime(quotas.getMaxWorkflowExecutionTime());
    }
    if(quotas.getMaxWorkflowStorage() != null) {
      team.getQuotas().setMaxWorkflowStorage(quotas.getMaxWorkflowStorage());
    }
    
    return flowTeamService.save(team).getQuotas();
  }

  @Override
  public Quotas updateQuotasForTeam(String teamId, Quotas quotas) {
    FlowTeamEntity team = flowTeamService.findById(teamId);
    team.setQuotas(quotas);
    return flowTeamService.save(team).getQuotas();
  }

  @Override
  public Quotas getDefaultQuotas() {
   Quotas quota = new Quotas();
   quota.setMaxConcurrentWorkflows(maxConcurrentWorkflows);
   quota.setMaxWorkflowCount(maxWorkflowCount);
   quota.setMaxWorkflowExecutionMonthly(maxWorkflowExecutionMonthly);
   quota.setMaxWorkflowExecutionTime(maxWorkflowExecutionTime);
   quota.setMaxWorkflowStorage(maxWorkflowStorage);
   return quota;
   
  }

  @Override
  public FlowTeam getTeamByIdDetailed(String teamId) {
    FlowTeamEntity flowEntity = flowTeamService.findById(teamId);
    FlowTeam flowTeam = new FlowTeam();
    if (flowEntity != null) {
      BeanUtils.copyProperties(flowEntity, flowTeam);
    }
    
    final List<WorkflowSummary> workflowSummary =
        workflowService.getWorkflowsForTeam(teamId);
    flowTeam.setWorkflows(workflowSummary);
    
    List<String> teamIds = new LinkedList<>();
    teamIds.add(teamId);
    
    List<FlowUserEntity> existingUsers = this.userIdentiyService.getUsersForTeams(teamIds);
    flowTeam.setUsers(existingUsers);
   
    return flowTeam;
  }
}
