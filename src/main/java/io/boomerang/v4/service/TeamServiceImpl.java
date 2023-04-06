package io.boomerang.v4.service;

import static io.boomerang.util.DataAdapterUtil.filterValueByFieldType;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Collectors;
import org.apache.commons.lang3.EnumUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
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
import io.boomerang.v4.data.entity.ApproverGroupEntity;
import io.boomerang.v4.data.entity.TeamEntity;
import io.boomerang.v4.data.entity.UserEntity;
import io.boomerang.v4.data.model.CurrentQuotas;
import io.boomerang.v4.data.model.Quotas;
import io.boomerang.v4.data.model.TeamParameter;
import io.boomerang.v4.data.model.TeamSettings;
import io.boomerang.v4.data.repository.ApproverGroupRepository;
import io.boomerang.v4.data.repository.TeamRepository;
import io.boomerang.v4.data.repository.UserRepository;
import io.boomerang.v4.model.ApproverGroup;
import io.boomerang.v4.model.ApproverGroupRequest;
import io.boomerang.v4.model.TeamRequest;
import io.boomerang.v4.model.Team;
import io.boomerang.v4.model.TeamResponsePage;
import io.boomerang.v4.model.UserSummary;
import io.boomerang.v4.model.enums.RelationshipRefType;
import io.boomerang.v4.model.enums.TeamStatus;
import io.boomerang.v4.model.ref.WorkflowRunInsight;

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
  
  @Autowired
  private TeamRepository teamRepository;
  
  @Autowired
  private UserRepository userRepository;
  
  @Autowired
  private ApproverGroupRepository approverGroupRepository;

  @Autowired
  private SettingsService settingsService;

  @Autowired
  private RelationshipService relationshipService;

  @Autowired
  private MongoTemplate mongoTemplate;

  @Autowired
  private UserService userService;

  @Autowired
  private WorkflowRunService workflowRunService;

  /*
   * Creates a new Team - Only available to Global tokens / admins
   * 
   * - Name must not be blank 
   * 
   * TODO: check user is admin or global token
   */
  @Override
  public ResponseEntity<Team> create(TeamRequest request) {
    if (!request.getName().isBlank()) {
      TeamEntity teamEntity = new TeamEntity();
      teamEntity.setName(request.getName());

      if (request.getExternalRef() != null
          && !request.getExternalRef().isBlank()) {
        teamEntity.setExternalRef(request.getExternalRef());
      }

      if (request.getLabels() != null && !request.getLabels().isEmpty()) {
        teamEntity.setLabels(request.getLabels());
      }

      // Set custom quotas
      // Don't set default quotas as they can change over time and should be dynamic
      Quotas quotas = new Quotas();
      // Override quotas based on creation request
      setCustomQuotas(quotas, request.getQuotas());
      teamEntity.setQuotas(quotas);

      teamEntity = teamRepository.save(teamEntity);

      // Create Relationships for Users
      // If user does not exist, no relationship will be created
      // If Relationship already exists, don't create a new one.
      createUserRelationships(teamEntity.getId(), request);

      return ResponseEntity.ok(convertTeamEntityToTeam(teamEntity));
    } else {
      // TODO: make this a better error for unable to create team i.e. name is mandatory
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
  }

  /*
   * Update team
   */
  @Override
  public ResponseEntity<Team> update(TeamRequest request) {
    if (request != null) {
      if (request.getId() == null || request.getId().isBlank()) {
        throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
      }
      List<String> teamRefs = relationshipService.getFilteredRefs(RelationshipRefType.TEAM,
          Optional.empty(), Optional.empty(), Optional.empty());
      if (!teamRefs.contains(request.getId())) {
        throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
      }
      Optional<TeamEntity> optTeamEntity = teamRepository.findById(request.getId());
      if (!optTeamEntity.isPresent()) {
        throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
      }
      TeamEntity teamEntity = optTeamEntity.get();
      BeanUtils.copyProperties(request, teamEntity, "users", "quotas");

      // Set custom quotas
      // Don't set default quotas as they can change over time and should be dynamic
      Quotas quotas = new Quotas();
      // Override quotas based on creation request
      setCustomQuotas(quotas, request.getQuotas());
      teamEntity.setQuotas(quotas);

      // Create Relationships for Users
      // If user does not exist, no relationship will be created
      // If Relationship already exists, don't create a new one.
      createUserRelationships(teamEntity.getId(), request);
      return ResponseEntity.ok(convertTeamEntityToTeam(teamEntity));
    }
    throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
  }

  /*
   * Retrieve a single team by ID
   */
  @Override
  public ResponseEntity<Team> get(String teamId) {
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

    return ResponseEntity.ok(convertTeamEntityToTeam(entity.get()));
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
        teamEntities.forEach(teamEntity -> teams.add(convertTeamEntityToTeam(teamEntity)));
      }
      return new TeamResponsePage(teams, pageable, pages.getNumberOfElements());
    }

    return null;
  }

  /*
   * Make team active
   */
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

  /*
   * Make team inactive
   */
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

  @Override
  public ResponseEntity<List<UserSummary>> addMembers(String teamId,
      TeamRequest request) {
    if (request != null && request.getUsers() != null && !request.getUsers().isEmpty()) {
      if (request.getId() == null || request.getId().isBlank()) {
        throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
      }
      List<String> teamRefs = relationshipService.getFilteredRefs(RelationshipRefType.TEAM,
          Optional.empty(), Optional.empty(), Optional.empty());
      if (!teamRefs.contains(request.getId())) {
        throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
      }
      Optional<TeamEntity> optTeamEntity = teamRepository.findById(request.getId());
      if (!optTeamEntity.isPresent()) {
        throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
      }
      
      // Create Relationships for Users
      // If user does not exist, no relationship will be created
      // If Relationship already exists, don't create a new one.
      createUserRelationships(teamId, request);
    }
    
    return ResponseEntity.ok(getUsersForTeam(teamId));
  }

  @Override
  public ResponseEntity<List<UserSummary>> removeMembers(String teamId, TeamRequest request) {
    if (request != null && request.getUsers() != null && !request.getUsers().isEmpty()) {
      if (request.getId() == null || request.getId().isBlank()) {
        throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
      }
      List<String> teamRefs = relationshipService.getFilteredRefs(RelationshipRefType.TEAM,
          Optional.empty(), Optional.empty(), Optional.empty());
      if (!teamRefs.contains(request.getId())) {
        throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
      }
      Optional<TeamEntity> optTeamEntity = teamRepository.findById(request.getId());
      if (!optTeamEntity.isPresent()) {
        throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
      }

      List<String> userRefs = Collections.emptyList();
      for (UserSummary userSummary : request.getUsers()) {
        UserEntity userEntity = null;
        if (!userSummary.getId().isEmpty()) {
          userEntity = userService.getUserById(userSummary.getId()).orElseGet(null);
        } else if (!userSummary.getEmail().isEmpty()) {
          userEntity = userService.getUserWithEmail(userSummary.getEmail());
        }
        if (userEntity != null) {
          userRefs.add(userEntity.getId());
        }
      }
      relationshipService.removeRelationships(RelationshipRefType.USER, userRefs,
          RelationshipRefType.TEAM, List.of(request.getId()));
    }

    return ResponseEntity.ok(getUsersForTeam(teamId));
  }

  /*
   * Creates a Team Parameter
   */
  @Override
  public ResponseEntity<TeamParameter> createParameter(String teamId, TeamParameter parameter) {
    if (teamId == null || teamId.isBlank()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    List<String> teamRefs = relationshipService.getFilteredRefs(RelationshipRefType.TEAM,
        Optional.empty(), Optional.empty(), Optional.empty());
    if (!teamRefs.contains(teamId)) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    Optional<TeamEntity> optTeamEntity = teamRepository.findById(teamId);
    if (!optTeamEntity.isPresent()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    TeamEntity teamEntity = optTeamEntity.get();
    if (teamEntity.getSettings() == null) {
      teamEntity.setSettings(new TeamSettings());
    }

    List<TeamParameter> parameters = teamEntity.getSettings().getParameters();
    TeamParameter existingParameter = parameters.stream()
        .filter(p -> p.getKey().equals(parameter.getKey())).findAny().orElse(null);

    if (existingParameter != null) {
      // TODO better exception to say parameter already exists
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }

    parameters.add(parameter);
    teamRepository.save(teamEntity);
    // If the parameter is a password, do not return its value, for security reasons.
    filterValueByFieldType(List.of(parameter), false, FieldType.PASSWORD.value());

    return ResponseEntity.ok(parameter);
  }

  /*
   * Creates a Team Parameter
   */
  @Override
  public ResponseEntity<TeamParameter> updateParameter(String teamId, TeamParameter parameter) {
    if (teamId == null || teamId.isBlank()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    List<String> teamRefs = relationshipService.getFilteredRefs(RelationshipRefType.TEAM,
        Optional.empty(), Optional.empty(), Optional.empty());
    if (!teamRefs.contains(teamId)) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    Optional<TeamEntity> optTeamEntity = teamRepository.findById(teamId);
    if (!optTeamEntity.isPresent()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    TeamEntity teamEntity = optTeamEntity.get();
    if (teamEntity.getSettings() == null) {
      teamEntity.setSettings(new TeamSettings());
    }

    List<TeamParameter> parameters = teamEntity.getSettings().getParameters();
    TeamParameter existingParameter = parameters.stream()
        .filter(p -> p.getKey().equals(parameter.getKey())).findAny().orElse(null);

    if (existingParameter == null) {
      // TODO better exception to say parameter already exists
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    BeanUtils.copyProperties(parameter, existingParameter);
    parameters.remove(existingParameter);
    parameters.add(parameter);
    teamRepository.save(teamEntity);
    // If the parameter is a password, do not return its value, for security reasons.
    filterValueByFieldType(List.of(parameter), false, FieldType.PASSWORD.value());

    return ResponseEntity.ok(parameter);
  }

  /*
   * Delete single parameter by key
   */
  @Override
  public ResponseEntity<Void> deleteParameter(String teamId, String key) {
    if (teamId == null || teamId.isBlank()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    List<String> teamRefs = relationshipService.getFilteredRefs(RelationshipRefType.TEAM,
        Optional.empty(), Optional.empty(), Optional.empty());
    if (!teamRefs.contains(teamId)) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    Optional<TeamEntity> optTeamEntity = teamRepository.findById(teamId);
    if (!optTeamEntity.isPresent()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    TeamEntity teamEntity = optTeamEntity.get();

    if (teamEntity.getSettings() != null && teamEntity.getSettings().getParameters() != null) {
      List<TeamParameter> parameters = teamEntity.getSettings().getParameters();
      TeamParameter parameter =
          parameters.stream().filter(p -> p.getKey().equals(key)).findAny().orElse(null);

      if (parameter != null) {
        parameters.remove(parameter);
      }
      teamEntity.getSettings().setParameters(parameters);
      teamRepository.save(teamEntity);

      return ResponseEntity.noContent().build();
    }

    // TODO better exception for unable find key
    throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
  }

  /*
   * Return all team Parameters
   */
  @Override
  public ResponseEntity<List<TeamParameter>> getParameters(String teamId) {
    if (teamId == null || teamId.isBlank()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    List<String> teamRefs = relationshipService.getFilteredRefs(RelationshipRefType.TEAM,
        Optional.empty(), Optional.empty(), Optional.empty());
    if (!teamRefs.contains(teamId)) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    Optional<TeamEntity> optTeamEntity = teamRepository.findById(teamId);
    if (!optTeamEntity.isPresent()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    TeamEntity teamEntity = optTeamEntity.get();
    List<TeamParameter> parameters = Collections.emptyList();
    if (teamEntity.getSettings() != null && teamEntity.getSettings().getParameters() != null) {
      parameters = teamEntity.getSettings().getParameters();

      filterValueByFieldType(parameters, false, FieldType.PASSWORD.value());
    }
    return ResponseEntity.ok(parameters);
  }

  /*
   * Get Current and Default Quotas for a Team
   */
  @Override
  public ResponseEntity<CurrentQuotas> getQuotas(String teamId) {
    if (teamId == null || teamId.isBlank()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    List<String> teamRefs = relationshipService.getFilteredRefs(RelationshipRefType.TEAM,
        Optional.empty(), Optional.empty(), Optional.empty());
    if (!teamRefs.contains(teamId)) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    Optional<TeamEntity> optTeamEntity = teamRepository.findById(teamId);
    if (!optTeamEntity.isPresent()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    TeamEntity teamEntity = optTeamEntity.get();

    // Set default & custom stored Quotas
    Quotas quotas = setDefaultQuotas();
    setCustomQuotas(quotas, teamEntity.getQuotas());
    CurrentQuotas currentQuotas = new CurrentQuotas(quotas);
    setCurrentQuotas(currentQuotas, teamId);

    return ResponseEntity.ok(currentQuotas);
  }

  /*
   * Patch the quotas on a team.
   */
  @Override
  public ResponseEntity<Quotas> patchQuotas(String teamId, Quotas patchQuotas) {
    if (teamId == null || teamId.isBlank()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    List<String> teamRefs = relationshipService.getFilteredRefs(RelationshipRefType.TEAM,
        Optional.empty(), Optional.empty(), Optional.empty());
    if (!teamRefs.contains(teamId)) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    Optional<TeamEntity> optTeamEntity = teamRepository.findById(teamId);
    if (!optTeamEntity.isPresent()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    TeamEntity teamEntity = optTeamEntity.get();

    // Update and save the patched quotas
    setCustomQuotas(teamEntity.getQuotas(), patchQuotas);
    teamRepository.save(teamEntity);

    // Return with a full set of quotas (default + what is set)
    Quotas returnQuotas = setDefaultQuotas();
    setCustomQuotas(returnQuotas, teamEntity.getQuotas());

    return ResponseEntity.ok(returnQuotas);
  }

  /*
   * Reset quotas to default (i.e. delete custom quotas on the team)
   */
  @Override
  public ResponseEntity<Quotas> resetQuotas(String teamId) {
    if (teamId == null || teamId.isBlank()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    List<String> teamRefs = relationshipService.getFilteredRefs(RelationshipRefType.TEAM,
        Optional.empty(), Optional.empty(), Optional.empty());
    if (!teamRefs.contains(teamId)) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    Optional<TeamEntity> optTeamEntity = teamRepository.findById(teamId);
    if (!optTeamEntity.isPresent()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    TeamEntity teamEntity = optTeamEntity.get();

    // Delete any custom quotas set on the team
    // This will then reset and default to the Team Quotas set in Settings
    teamEntity.setQuotas(new Quotas());
    teamRepository.save(teamEntity);

    return ResponseEntity.ok(setDefaultQuotas());
  }

  /*
   * Reset quotas to default (i.e. delete custom quotas on the team)
   */
  @Override
  public ResponseEntity<Quotas> getDefaultQuotas() {
    return ResponseEntity.ok(setDefaultQuotas());
  }

  /*
   * Set default quotas
   */
  private Quotas setDefaultQuotas() {
    Quotas quotas = new Quotas();
    quotas.setMaxWorkflowCount(Integer
        .valueOf(settingsService.getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_COUNT).getValue()));
    quotas.setMaxWorkflowExecutionMonthly(Integer.valueOf(
        settingsService.getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_EXECUTION_MONTHLY).getValue()));
    quotas.setMaxWorkflowStorage(Integer.valueOf(settingsService
        .getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_STORAGE).getValue().replace("Gi", "")));
    quotas.setMaxWorkflowExecutionTime(Integer
        .valueOf(settingsService.getConfiguration(TEAMS, MAX_TEAM_WORKFLOW_DURATION).getValue()));
    quotas.setMaxConcurrentWorkflows(Integer
        .valueOf(settingsService.getConfiguration(TEAMS, MAX_TEAM_CONCURRENT_WORKFLOW).getValue()));
    return quotas;
  }

  private void setCustomQuotas(Quotas quotas, Quotas customQuotas) {
    if (customQuotas != null) {
      if (customQuotas.getMaxWorkflowCount() != null) {
        quotas.setMaxWorkflowCount(customQuotas.getMaxWorkflowCount());
      }
      if (customQuotas.getMaxWorkflowExecutionMonthly() != null) {
        quotas.setMaxWorkflowExecutionMonthly(customQuotas.getMaxWorkflowExecutionMonthly());
      }
      if (customQuotas.getMaxWorkflowStorage() != null) {
        quotas.setMaxWorkflowStorage(customQuotas.getMaxWorkflowStorage());
      }
      if (customQuotas.getMaxWorkflowExecutionTime() != null) {
        quotas.setMaxWorkflowExecutionTime(customQuotas.getMaxWorkflowExecutionTime());
      }
      if (customQuotas.getMaxConcurrentWorkflows() != null) {
        quotas.setMaxConcurrentWorkflows(customQuotas.getMaxConcurrentWorkflows());
      }
    }
  }

  private CurrentQuotas setCurrentQuotas(CurrentQuotas currentQuotas, String teamId) {
    // Set Quota Reset Date
    Calendar nextMonth = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    nextMonth.add(Calendar.MONTH, 1);
    nextMonth.set(Calendar.DAY_OF_MONTH, 1);
    nextMonth.set(Calendar.HOUR_OF_DAY, 0);
    nextMonth.set(Calendar.MINUTE, 0);
    nextMonth.set(Calendar.SECOND, 0);
    nextMonth.set(Calendar.MILLISECOND, 0);
    currentQuotas.setMonthlyResetDate(nextMonth.getTime());

    Calendar currentMonthStart = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    nextMonth.set(Calendar.DAY_OF_MONTH, 1);
    nextMonth.set(Calendar.HOUR_OF_DAY, 0);
    nextMonth.set(Calendar.MINUTE, 0);
    nextMonth.set(Calendar.SECOND, 0);
    nextMonth.set(Calendar.MILLISECOND, 0);

    Calendar currentMonthEnd = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    nextMonth.add(Calendar.MONTH, 1);
    nextMonth.set(Calendar.DAY_OF_MONTH, 1);
    nextMonth.set(Calendar.HOUR_OF_DAY, 0);
    nextMonth.set(Calendar.MINUTE, 0);
    nextMonth.set(Calendar.SECOND, 0);
    nextMonth.set(Calendar.MILLISECOND, 0);

    WorkflowRunInsight insight =
        workflowRunService.insight(Optional.of(currentMonthStart.getTimeInMillis()),
            Optional.of(currentMonthEnd.getTimeInMillis()), Optional.empty(),
            Optional.of(List.of(teamId)));
    currentQuotas.setCurrentConcurrentWorkflows(insight.getConcurrentRuns().intValue());
    currentQuotas.setCurrentWorkflowExecutionMonthly(insight.getTotalDuration().intValue());

    // TODO update to only be active workflows
    List<String> workflowRefs = relationshipService.getFilteredRefs(RelationshipRefType.WORKFLOW,
        Optional.empty(), Optional.of(List.of(teamId)), Optional.empty());
    currentQuotas.setCurrentWorkflowCount(workflowRefs.size());

    // TODO look into this one
    currentQuotas.setCurrentWorkflowsPersistentStorage(null);
    return currentQuotas;
  }

  /*
   * Converts the TeamEntity to Team and adds the extra Users, WorkflowRefs, ApproverGroupRefs,
   * Quotas
   */
  private Team convertTeamEntityToTeam(TeamEntity teamEntity) {
    Team team = new Team(teamEntity);

    // Get and Set WorkflowRefs
    List<String> teamWorkflowRefs =
        relationshipService.getFilteredRefs(RelationshipRefType.WORKFLOW, Optional.empty(),
            Optional.of(List.of(teamEntity.getId())), Optional.empty());
    team.setWorkflowRefs(teamWorkflowRefs);

    // Get and Set Users
    team.setUsers(getUsersForTeam(teamEntity.getId()));

    // Set default & custom stored Quotas
    Quotas quotas = setDefaultQuotas();
    setCustomQuotas(quotas, teamEntity.getQuotas());
    CurrentQuotas currentQuotas = new CurrentQuotas(quotas);
    setCurrentQuotas(currentQuotas, teamEntity.getId());
    team.setQuotas(currentQuotas);

    // Set Approver Groups
    List<String> approverGroupRefs =
        relationshipService.getFilteredRefs(RelationshipRefType.APPROVERGROUP, Optional.empty(),
            Optional.of(List.of(teamEntity.getId())), Optional.empty());
    List<ApproverGroupEntity> approverGroupEntities =
        approverGroupRepository.findByIdIn(approverGroupRefs);
    List<ApproverGroup> approverGroups = Collections.emptyList();
    approverGroupEntities.forEach(age -> {
      ApproverGroup ag = convertEntityToApproverGroup(age);
      approverGroups.add(ag);
    });
    team.setApproverGroups(approverGroups);

    // If the parameter is a password, do not return its value, for security reasons.
    if (team.getSettings() != null && team.getSettings().getParameters() != null) {
      filterValueByFieldType(team.getSettings().getParameters(), false, FieldType.PASSWORD.value());
    }

    return team;
  }

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

  @Override
  public ResponseEntity<List<ApproverGroup>> getApproverGroups(String teamId) {
    if (teamId == null || teamId.isBlank()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    List<String> teamRefs = relationshipService.getFilteredRefs(RelationshipRefType.TEAM,
        Optional.empty(), Optional.empty(), Optional.empty());
    if (!teamRefs.contains(teamId)) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    Optional<TeamEntity> optTeamEntity = teamRepository.findById(teamId);
    if (!optTeamEntity.isPresent()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    TeamEntity teamEntity = optTeamEntity.get();

    // Get and Set WorkflowRefs
    List<String> approverGroupRefs =
        relationshipService.getFilteredRefs(RelationshipRefType.APPROVERGROUP, Optional.empty(),
            Optional.of(List.of(teamEntity.getId())), Optional.empty());

    List<ApproverGroupEntity> approverGroupEntities =
        approverGroupRepository.findByIdIn(approverGroupRefs);
    List<ApproverGroup> approverGroups = Collections.emptyList();
    approverGroupEntities.forEach(age -> {
      ApproverGroup ag = convertEntityToApproverGroup(age);
      approverGroups.add(ag);
    });

    return ResponseEntity.ok(approverGroups);
  }

  @Override
  public ResponseEntity<Void> deleteApproverGroup(String teamId, String id) {
    if (teamId == null || teamId.isBlank()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    List<String> teamRefs = relationshipService.getFilteredRefs(RelationshipRefType.TEAM,
        Optional.empty(), Optional.empty(), Optional.empty());
    if (!teamRefs.contains(teamId)) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    Optional<TeamEntity> optTeamEntity = teamRepository.findById(teamId);
    if (!optTeamEntity.isPresent()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    TeamEntity teamEntity = optTeamEntity.get();

    List<String> approverGroupRefs =
        relationshipService.getFilteredRefs(RelationshipRefType.APPROVERGROUP, Optional.empty(),
            Optional.of(List.of(teamEntity.getId())), Optional.empty());

    if (approverGroupRefs.isEmpty() || !approverGroupRefs.contains(id)) {
      // TODO better exception for not the right Approver Group Ref
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    Optional<ApproverGroupEntity> ag = approverGroupRepository.findById(id);
    if (ag.isPresent()) {
      relationshipService.removeRelationship(RelationshipRefType.APPROVERGROUP, id);
    }
    approverGroupRepository.deleteById(id);

    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<ApproverGroup> createApproverGroup(String teamId,
      ApproverGroupRequest request) {
    if (teamId == null || teamId.isBlank()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    List<String> teamRefs = relationshipService.getFilteredRefs(RelationshipRefType.TEAM,
        Optional.empty(), Optional.empty(), Optional.empty());
    if (!teamRefs.contains(teamId)) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    Optional<TeamEntity> optTeamEntity = teamRepository.findById(teamId);
    if (!optTeamEntity.isPresent()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    TeamEntity teamEntity = optTeamEntity.get();

    if (request == null || request.getName().isBlank()) {
      // TODO better exception for invalid Team Parameter Name
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }

    List<String> approverGroupRefs =
        relationshipService.getFilteredRefs(RelationshipRefType.APPROVERGROUP, Optional.empty(),
            Optional.of(List.of(teamEntity.getId())), Optional.empty());
    List<ApproverGroupEntity> approverGroupEntities =
        approverGroupRepository.findByIdIn(approverGroupRefs);

    if (approverGroupEntities.stream()
        .anyMatch(ag -> ag.getName().equalsIgnoreCase(request.getName()))) {
      // TODO better exception for non unique team name
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }

    ApproverGroupEntity approverGroupEntity = new ApproverGroupEntity();
    approverGroupEntity.setName(request.getName());

    if (request.getApprovers() != null) {
      List<String> userRefs = relationshipService.getFilteredRefs(RelationshipRefType.USER,
          Optional.empty(), Optional.of(List.of(teamEntity.getId())), Optional.empty());
      request.getApprovers().forEach(a -> {
        if (userRefs.contains(a)) {
          approverGroupEntity.getApproverRefs().add(a);
        }
      });
    }
    approverGroupRepository.save(approverGroupEntity);

    return ResponseEntity.ok(convertEntityToApproverGroup(approverGroupEntity));
  }

  /*
   * Update Approver Group
   * 
   * Checks if the new name is unique
   */
  @Override
  public ResponseEntity<ApproverGroup> updateApproverGroup(String teamId,
      ApproverGroupRequest request) {
    if (teamId == null || teamId.isBlank()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    List<String> teamRefs = relationshipService.getFilteredRefs(RelationshipRefType.TEAM,
        Optional.empty(), Optional.empty(), Optional.empty());
    if (!teamRefs.contains(teamId)) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    Optional<TeamEntity> optTeamEntity = teamRepository.findById(teamId);
    if (!optTeamEntity.isPresent()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    TeamEntity teamEntity = optTeamEntity.get();

    if (request == null || request.getName().isBlank()) {
      // TODO better exception for invalid Team Parameter Name
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }

    List<String> approverGroupRefs =
        relationshipService.getFilteredRefs(RelationshipRefType.APPROVERGROUP, Optional.empty(),
            Optional.of(List.of(teamEntity.getId())), Optional.empty());
    List<ApproverGroupEntity> approverGroupEntities =
        approverGroupRepository.findByIdIn(approverGroupRefs);

    if (approverGroupEntities.stream().filter(ag -> !ag.getId().equals(request.getId()))
        .anyMatch(ag -> ag.getName().equalsIgnoreCase(request.getName()))) {
      // TODO better exception for non unique team name
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }

    Optional<ApproverGroupEntity> optEntity =
        approverGroupEntities.stream().filter(ag -> ag.getId().equals(request.getId())).findFirst();
    if (!optEntity.isPresent()) {
      // TODO better exception for approver group invalid ref
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    optEntity.get().setName(request.getName());

    List<String> approverRefs = Collections.emptyList();
    if (request.getApprovers() != null) {
      List<String> userRefs = relationshipService.getFilteredRefs(RelationshipRefType.USER,
          Optional.empty(), Optional.of(List.of(teamEntity.getId())), Optional.empty());
      request.getApprovers().forEach(a -> {
        if (userRefs.contains(a)) {
          approverRefs.add(a);
        }
      });
    }
    optEntity.get().setApproverRefs(approverRefs);
    approverGroupRepository.save(optEntity.get());
    return ResponseEntity.ok(convertEntityToApproverGroup(optEntity.get()));
  }

  private ApproverGroup convertEntityToApproverGroup(ApproverGroupEntity age) {
    ApproverGroup ag = new ApproverGroup(age);
    if (!age.getApproverRefs().isEmpty()) {
      age.getApproverRefs().forEach(ref -> {
        Optional<UserEntity> ue = userRepository.findById(ref);
        if (ue.isPresent()) {
          UserSummary u = new UserSummary(ue.get());
          ag.getApprovers().add(u);
        }
      });
    }
    return ag;
  }

  private List<UserSummary> getUsersForTeam(String teamId) {
    List<String> userRefs = relationshipService.getFilteredRefs(RelationshipRefType.USER,
        Optional.empty(), Optional.of(List.of(teamId)), Optional.empty());
    List<UserSummary> teamUsers = new LinkedList<>();
    if (!userRefs.isEmpty()) {
      userRefs.forEach(ref -> {
        Optional<UserEntity> ue = userRepository.findById(ref);
        if (ue.isPresent()) {
          UserSummary u = new UserSummary(ue.get());
          teamUsers.add(u);
        }
      });
    }
    return teamUsers;
  }

  private void createUserRelationships(String teamId, TeamRequest request) {
    List<String> userRefs = relationshipService.getFilteredRefs(RelationshipRefType.USER,
        Optional.empty(), Optional.of(List.of(request.getId())), Optional.empty());
    if (request.getUsers() != null && !request.getUsers().isEmpty()) {
      for (UserSummary userSummary : request.getUsers()) {
        UserEntity userEntity = null;
        if (!userSummary.getId().isEmpty()) {
          userEntity = userService.getUserById(userSummary.getId()).orElseGet(null);
        } else if (!userSummary.getEmail().isEmpty()) {
          userEntity = userService.getUserWithEmail(userSummary.getEmail());
        }
        if (userEntity != null && !userRefs.contains(userEntity.getId())) {
          relationshipService.createRelationshipRef(RelationshipRefType.USER, userEntity.getId(),
              RelationshipRefType.TEAM, teamId);
        }
      }
    }
  }
}
