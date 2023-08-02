package io.boomerang.service;

import static io.boomerang.util.DataAdapterUtil.filterValueByFieldType;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.security.entity.RoleEntity;
import io.boomerang.security.model.Role;
import io.boomerang.security.model.TeamRoleEnum;
import io.boomerang.security.repository.RoleRepository;
import io.boomerang.security.service.IdentityService;
import io.boomerang.util.DataAdapterUtil.FieldType;
import io.boomerang.v4.data.entity.ApproverGroupEntity;
import io.boomerang.v4.data.entity.RelationshipEntity;
import io.boomerang.v4.data.entity.TeamEntity;
import io.boomerang.v4.data.entity.UserEntity;
import io.boomerang.v4.data.model.CurrentQuotas;
import io.boomerang.v4.data.model.Quotas;
import io.boomerang.v4.data.repository.ApproverGroupRepository;
import io.boomerang.v4.data.repository.TeamRepository;
import io.boomerang.v4.model.AbstractParam;
import io.boomerang.v4.model.ApproverGroup;
import io.boomerang.v4.model.ApproverGroupRequest;
import io.boomerang.v4.model.Team;
import io.boomerang.v4.model.TeamNameCheckRequest;
import io.boomerang.v4.model.TeamRequest;
import io.boomerang.v4.model.User;
import io.boomerang.v4.model.UserSummary;
import io.boomerang.v4.model.enums.RelationshipRef;
import io.boomerang.v4.model.enums.RelationshipType;
import io.boomerang.v4.model.enums.TeamStatus;
import io.boomerang.v4.model.enums.TeamType;
import io.boomerang.v4.model.enums.UserType;
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

//  @Autowired
//  private UserRepository userRepository;

  @Autowired
  private IdentityService identityService;

  @Autowired
  private ApproverGroupRepository approverGroupRepository;

  @Autowired
  private RoleRepository roleRepository;

  @Autowired
  private SettingsService settingsService;

  @Autowired
  private RelationshipService relationshipService;

  @Autowired
  private MongoTemplate mongoTemplate;

  @Autowired
  private WorkflowRunService workflowRunService;
  
  @Autowired
  private WorkflowService workflowService;

  /*
   * Validate the team name - used by the UI to determine if a team can be created
   */
  @Override
  public ResponseEntity<?> validateName(TeamNameCheckRequest request) {
    if (request.getName() != null && !request.getName().isBlank()) {
      if (teamRepository.countByNameIgnoreCase(request.getName()) > 0) {
        return ResponseEntity.unprocessableEntity().build();
      }
      return ResponseEntity.ok().build();
    }
    // TODO: make this a better error for unable to verify team name i.e. name is mandatory
    throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
  }

  /*
   * Creates a new Team - Only available to Global tokens / admins
   * 
   * - Name must not be blank
   */
  @Override
  public ResponseEntity<Team> create(TeamRequest request) {
    return create(request, TeamType.team);
  }

  @Override
  public ResponseEntity<Team> create(TeamRequest request, TeamType type) {
    if (!request.getName().isBlank()) {
      TeamEntity teamEntity = new TeamEntity();
      teamEntity.setName(request.getName());
      teamEntity.setType(type); // Only create TeamType.Team in most cases - User and System are internally created.
      if (request.getExternalRef() != null && !request.getExternalRef().isBlank()) {
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
      createUserRelationships(teamEntity.getId(), request.getUsers());

      return ResponseEntity.ok(convertTeamEntityToTeam(teamEntity));
    } else {
      // TODO: make this a better error for unable to create team i.e. name is mandatory
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
  }

  /*
   * Patch team
   */
  @Override
  public ResponseEntity<Team> patch(TeamRequest request) {
    if (request != null) {
      if (request.getId() == null || request.getId().isBlank()) {
        throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
      }
      List<String> teamRefs = relationshipService.getFilteredToRefs(Optional.empty(),
          Optional.empty(), Optional.of(RelationshipType.MEMBEROF),
          Optional.of(RelationshipRef.TEAM), Optional.of(List.of(request.getId())));
      if (teamRefs.isEmpty()) {
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
      createUserRelationships(teamEntity.getId(), request.getUsers());
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
    List<String> teamRefs = relationshipService.getFilteredToRefs(Optional.empty(),
        Optional.empty(), Optional.of(RelationshipType.MEMBEROF),
        Optional.of(RelationshipRef.TEAM), Optional.of(List.of(teamId)));
    if (teamRefs.isEmpty()) {
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
  public Page<Team> mine(Optional<Integer> queryPage, Optional<Integer> queryLimit, Optional<Direction> queryOrder, Optional<String> querySort, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus) {    
    List<String> teamRefs = relationshipService.getMyTeamRefs();
    return findByCriteria(queryPage, queryLimit, queryOrder, querySort, queryLabels, queryStatus, teamRefs);
  }

  /*
   * Query for Teams
   * 
   * Returns Teams plus each Teams UserRefs, WorkflowRefs, and Quotas
   */
  @Override
  public Page<Team> query(Optional<Integer> queryPage, Optional<Integer> queryLimit, Optional<Direction> queryOrder, Optional<String> querySort, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus, Optional<List<String>> queryIds) {    
    List<String> teamRefs = relationshipService.getFilteredToRefs(Optional.empty(), Optional.empty(),
        Optional.of(RelationshipType.MEMBEROF), Optional.of(RelationshipRef.TEAM), queryIds);
    
    LOGGER.debug("TeamRefs: " + teamRefs.toString());

    return findByCriteria(queryPage, queryLimit, queryOrder, querySort, queryLabels, queryStatus, teamRefs);
  }

  private Page<Team> findByCriteria(Optional<Integer> queryPage, Optional<Integer> queryLimit, Optional<Direction> queryOrder, Optional<String> querySort, 
      Optional<List<String>> queryLabels, Optional<List<String>> queryStatus, List<String> teamRefs) {
    Pageable pageable = Pageable.unpaged();
    final Sort sort = Sort.by(new Order(queryOrder.orElse(Direction.ASC), querySort.orElse("name")));
    if (queryLimit.isPresent()) {
      pageable = PageRequest.of(queryPage.get(), queryLimit.get(), sort);
    }
    
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
    if (queryLimit.isPresent()) {
      query.with(pageable);
    } else {
      query.with(sort);
    }

    List<TeamEntity> teamEntities = mongoTemplate.find(query, TeamEntity.class);

    LOGGER.debug("Found " + teamEntities.size() + " teams.");
    List<Team> teams = new LinkedList<>();
    if (!teamEntities.isEmpty()) {
      teamEntities.forEach(teamEntity -> teams.add(convertTeamEntityToTeam(teamEntity)));
    }
    
    Page<Team> pages =
        PageableExecutionUtils.getPage(teams,
            pageable, () -> mongoTemplate.count(query, TeamEntity.class));

    return pages;
  }

  @Override
  public ResponseEntity<List<UserSummary>> addMembers(String teamId, List<UserSummary> request) {
    if (request != null && !request.isEmpty()) {
      if (teamId == null || teamId.isBlank()) {
        throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
      }
      List<String> teamRefs = relationshipService.getFilteredToRefs(Optional.empty(),
          Optional.empty(), Optional.of(RelationshipType.MEMBEROF),
          Optional.of(RelationshipRef.TEAM), Optional.of(List.of(teamId)));
      if (teamRefs.isEmpty()) {
        throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
      }
      LOGGER.info("Team Valid");     
      Optional<TeamEntity> optTeamEntity = teamRepository.findById(teamId);
      if (!optTeamEntity.isPresent()) {
        throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
      }

      // Create Relationships for Users
      createUserRelationships(teamId, request);
    } else {
      //TODO make this invalid request
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }

    return ResponseEntity.ok(getUsersForTeam(teamId));
  }

  @Override
  public void removeMembers(String teamId, List<UserSummary> request) {
    if (request != null && !request.isEmpty()) {
      if (teamId == null || teamId.isBlank()) {
        throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
      }
      List<String> teamRefs = relationshipService.getFilteredToRefs(Optional.empty(),
          Optional.empty(), Optional.of(RelationshipType.MEMBEROF),
          Optional.of(RelationshipRef.TEAM), Optional.of(List.of(teamId)));
      if (teamRefs.isEmpty()) {
        throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
      }
      Optional<TeamEntity> optTeamEntity = teamRepository.findById(teamId);
      if (!optTeamEntity.isPresent()) {
        throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
      }
      List<String> userRefs = new LinkedList<>();
      for (UserSummary userSummary : request) {
        Optional<User> userEntity = null;
        if (!userSummary.getId().isEmpty()) {
          userEntity = identityService.getUserByID(userSummary.getId());
        } else if (!userSummary.getEmail().isEmpty()) {
          userEntity = identityService.getUserByEmail(userSummary.getEmail());
        }
        if (userEntity.isPresent()) {
          userRefs.add(userEntity.get().getId());
        }
      }
      if (!userRefs.isEmpty()) {
        relationshipService.removeRelationships(RelationshipRef.USER, userRefs, RelationshipRef.TEAM,
            List.of(teamId));
      }
    }
  }

  /*
   * Creates a Team Parameter
   */
  @Override
  public ResponseEntity<AbstractParam> createParameter(String teamId, AbstractParam parameter) {
    if (teamId == null || teamId.isBlank()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    List<String> teamRefs = relationshipService.getFilteredToRefs(Optional.empty(),
        Optional.empty(), Optional.of(RelationshipType.MEMBEROF),
        Optional.of(RelationshipRef.TEAM), Optional.of(List.of(teamId)));
    if (teamRefs.isEmpty()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    Optional<TeamEntity> optTeamEntity = teamRepository.findById(teamId);
    if (!optTeamEntity.isPresent()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    TeamEntity teamEntity = optTeamEntity.get();
    List<AbstractParam> parameters = teamEntity.getParameters();
    AbstractParam existingParameter = parameters.stream()
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
  public ResponseEntity<AbstractParam> updateParameter(String teamId, AbstractParam parameter) {
    if (teamId == null || teamId.isBlank()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    List<String> teamRefs = relationshipService.getFilteredToRefs(Optional.empty(),
        Optional.empty(), Optional.of(RelationshipType.MEMBEROF),
        Optional.of(RelationshipRef.TEAM), Optional.of(List.of(teamId)));
    if (teamRefs.isEmpty()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    Optional<TeamEntity> optTeamEntity = teamRepository.findById(teamId);
    if (!optTeamEntity.isPresent()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    TeamEntity teamEntity = optTeamEntity.get();
    List<AbstractParam> parameters = teamEntity.getParameters();
    AbstractParam existingParameter = parameters.stream()
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
    List<String> teamRefs = relationshipService.getFilteredToRefs(Optional.empty(),
        Optional.empty(), Optional.of(RelationshipType.MEMBEROF),
        Optional.of(RelationshipRef.TEAM), Optional.of(List.of(teamId)));
    if (teamRefs.isEmpty()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    Optional<TeamEntity> optTeamEntity = teamRepository.findById(teamId);
    if (!optTeamEntity.isPresent()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    TeamEntity teamEntity = optTeamEntity.get();

    if (teamEntity.getParameters() != null) {
      List<AbstractParam> parameters = teamEntity.getParameters();
      AbstractParam parameter =
          parameters.stream().filter(p -> p.getKey().equals(key)).findAny().orElse(null);

      if (parameter != null) {
        parameters.remove(parameter);
      }
      teamEntity.setParameters(parameters);
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
  public ResponseEntity<List<AbstractParam>> getParameters(String teamId) {
    if (teamId == null || teamId.isBlank()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    List<String> teamRefs = relationshipService.getFilteredToRefs(Optional.empty(),
        Optional.empty(), Optional.of(RelationshipType.MEMBEROF),
        Optional.of(RelationshipRef.TEAM), Optional.of(List.of(teamId)));
    if (teamRefs.isEmpty()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    Optional<TeamEntity> optTeamEntity = teamRepository.findById(teamId);
    if (!optTeamEntity.isPresent()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    TeamEntity teamEntity = optTeamEntity.get();
    List<AbstractParam> parameters = Collections.emptyList();
    if (teamEntity.getParameters() != null) {
      parameters = teamEntity.getParameters();

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
    List<String> teamRefs = relationshipService.getFilteredToRefs(Optional.empty(),
        Optional.empty(), Optional.of(RelationshipType.MEMBEROF),
        Optional.of(RelationshipRef.TEAM), Optional.of(List.of(teamId)));
    if (teamRefs.isEmpty()) {
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
    List<String> teamRefs = relationshipService.getFilteredToRefs(Optional.empty(),
        Optional.empty(), Optional.of(RelationshipType.MEMBEROF),
        Optional.of(RelationshipRef.TEAM), Optional.of(List.of(teamId)));
    if (teamRefs.isEmpty()) {
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
    List<String> teamRefs = relationshipService.getFilteredToRefs(Optional.empty(),
        Optional.empty(), Optional.of(RelationshipType.MEMBEROF),
        Optional.of(RelationshipRef.TEAM), Optional.of(List.of(teamId)));
    if (teamRefs.isEmpty()) {
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

  /*
   * Retrieve the ApproverGroups for a team
   */
  @Override
  public ResponseEntity<List<ApproverGroup>> getApproverGroups(String teamId) {
    if (teamId == null || teamId.isBlank()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    List<String> teamRefs = relationshipService.getFilteredToRefs(Optional.empty(),
        Optional.empty(), Optional.of(RelationshipType.MEMBEROF),
        Optional.of(RelationshipRef.TEAM), Optional.of(List.of(teamId)));
    if (teamRefs.isEmpty()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    Optional<TeamEntity> optTeamEntity = teamRepository.findById(teamId);
    if (!optTeamEntity.isPresent()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    TeamEntity teamEntity = optTeamEntity.get();

    // Get and Set WorkflowRefs
    List<String> approverGroupRefs =
        relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.APPROVERGROUP),
            Optional.empty(), Optional.of(RelationshipType.BELONGSTO),
            Optional.of(RelationshipRef.TEAM), Optional.of(List.of(teamEntity.getId())));

    List<ApproverGroupEntity> approverGroupEntities =
        approverGroupRepository.findByIdIn(approverGroupRefs);
    List<ApproverGroup> approverGroups = Collections.emptyList();
    approverGroupEntities.forEach(age -> {
      ApproverGroup ag = convertEntityToApproverGroup(age);
      approverGroups.add(ag);
    });

    return ResponseEntity.ok(approverGroups);
  }
  
  /*
   * Return all team level roles
   */
  public ResponseEntity<List<Role>> getRoles() {
    List<RoleEntity> roleEntities = roleRepository.findByType("team");
    List<Role> roles = new LinkedList<>();
    roleEntities.forEach(re -> {
      roles.add(new Role(re));
    });
    return ResponseEntity.ok(roles);
  }

  /*
   * Create Approver Group
   * 
   * - Only creates a relationship against a team - Names must be unique per team
   */
  @Override
  public ResponseEntity<ApproverGroup> createApproverGroup(String teamId,
      ApproverGroupRequest request) {
    if (teamId == null || teamId.isBlank()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    List<String> teamRefs = relationshipService.getFilteredToRefs(Optional.empty(),
        Optional.empty(), Optional.of(RelationshipType.MEMBEROF),
        Optional.of(RelationshipRef.TEAM), Optional.of(List.of(teamId)));
    if (teamRefs.isEmpty()) {
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
        relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.APPROVERGROUP),
            Optional.empty(), Optional.of(RelationshipType.BELONGSTO),
            Optional.of(RelationshipRef.TEAM), Optional.of(List.of(teamId)));
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
      List<String> userRefs = relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.USER),
          Optional.empty(), Optional.of(RelationshipType.MEMBEROF),
          Optional.of(RelationshipRef.TEAM), Optional.of(List.of(teamEntity.getId())));
      LOGGER.debug("User Refs: " + userRefs.toString());
      List<String> validApproverRefs = request.getApprovers().stream()
          .filter(a -> userRefs.contains(a)).collect(Collectors.toList());
      LOGGER.debug("Valid Approver Refs: " + validApproverRefs.toString());
      approverGroupEntity.setApproverRefs(validApproverRefs);
    }
    approverGroupEntity = approverGroupRepository.save(approverGroupEntity);
    relationshipService.addRelationshipRef(RelationshipRef.APPROVERGROUP,
        approverGroupEntity.getId(), RelationshipType.BELONGSTO, RelationshipRef.TEAM, Optional.of(teamId), Optional.empty());

    return ResponseEntity.ok(convertEntityToApproverGroup(approverGroupEntity));
  }

  /*
   * Update Approver Group
   * 
   * - Checks if the new name is unique per team
   */
  @Override
  public ResponseEntity<ApproverGroup> updateApproverGroup(String teamId,
      ApproverGroupRequest request) {
    if (teamId == null || teamId.isBlank()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    List<String> teamRefs = relationshipService.getFilteredToRefs(Optional.empty(),
        Optional.empty(), Optional.of(RelationshipType.MEMBEROF),
        Optional.of(RelationshipRef.TEAM), Optional.of(List.of(teamId)));
    if (teamRefs.isEmpty()) {
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
        relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.APPROVERGROUP),
            Optional.empty(), Optional.of(RelationshipType.BELONGSTO),
            Optional.of(RelationshipRef.TEAM), Optional.of(List.of(teamEntity.getId())));
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
      List<String> userRefs = relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.USER),
          Optional.empty(), Optional.of(RelationshipType.MEMBEROF),
          Optional.of(RelationshipRef.TEAM), Optional.of(List.of(teamEntity.getId())));
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

  /*
   * Delete an Approver Group
   * 
   * - Removes relationship as well
   */
  @Override
  public ResponseEntity<Void> deleteApproverGroup(String teamId, String id) {
    if (teamId == null || teamId.isBlank()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    List<String> teamRefs = relationshipService.getFilteredToRefs(Optional.empty(),
        Optional.empty(), Optional.of(RelationshipType.MEMBEROF),
        Optional.of(RelationshipRef.TEAM), Optional.of(List.of(teamId)));
    if (teamRefs.isEmpty()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    Optional<TeamEntity> optTeamEntity = teamRepository.findById(teamId);
    if (!optTeamEntity.isPresent()) {
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    TeamEntity teamEntity = optTeamEntity.get();

    List<String> approverGroupRefs =
        relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.APPROVERGROUP),
            Optional.empty(), Optional.of(RelationshipType.BELONGSTO),
            Optional.of(RelationshipRef.TEAM), Optional.of(List.of(teamEntity.getId())));

    if (approverGroupRefs.isEmpty()) {
      // TODO better exception for not the right Approver Group Ref
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
    Optional<ApproverGroupEntity> ag = approverGroupRepository.findById(id);
    if (ag.isPresent()) {
      relationshipService.removeRelationships(RelationshipRef.APPROVERGROUP, List.of(id),
          RelationshipRef.TEAM, List.of(teamId));
    }
    approverGroupRepository.deleteById(id);

    return ResponseEntity.noContent().build();
  }

  /*
   * Converts the Team Entity to Model and adds the extra Users, WorkflowRefs, ApproverGroupRefs,
   * Quotas
   */
  private Team convertTeamEntityToTeam(TeamEntity teamEntity) {
    Team team = new Team(teamEntity);
    
//    List<WorkflowSummary> summary = new LinkedList<>();
//    try {
//      WorkflowResponsePage response = workflowService.query(Optional.empty(), Optional.empty(), Optional.of(Direction.ASC), Optional.empty(), Optional.empty(), Optional.of(List.of(teamEntity.getId())), Optional.empty());
//      if (response.getContent() != null && !response.getContent().isEmpty()) {
//        List<Workflow> workflows = response.getContent();
//        workflows.forEach(w -> summary.add(new WorkflowSummary(w)));
//      }
//    } catch (BoomerangException e) {
//      LOGGER.error("convertTeamEntityToTeam() - issue in retrieving Workflows for this team. Most likely cause is page size is being returned as 0");
//    }
//    team.setWorkflows(summary);
    
    // Get and Set Users
    team.setMembers(getUsersForTeam(teamEntity.getId()));

    // Set default & custom stored Quotas
    Quotas quotas = setDefaultQuotas();
    setCustomQuotas(quotas, teamEntity.getQuotas());
    CurrentQuotas currentQuotas = new CurrentQuotas(quotas);
    setCurrentQuotas(currentQuotas, teamEntity.getId());
    team.setQuotas(currentQuotas);

    // Set Approver Groups
    List<String> approverGroupRefs =
        relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.APPROVERGROUP),
            Optional.empty(), Optional.of(RelationshipType.BELONGSTO),
            Optional.of(RelationshipRef.TEAM), Optional.of(List.of(teamEntity.getId())));
    List<ApproverGroupEntity> approverGroupEntities =
        approverGroupRepository.findByIdIn(approverGroupRefs);
    List<ApproverGroup> approverGroups = new LinkedList<>();
    approverGroupEntities.forEach(age -> {
      approverGroups.add(convertEntityToApproverGroup(age));
    });
    team.setApproverGroups(approverGroups);

    // If the parameter is a password, do not return its value, for security reasons.
    if (team.getParameters() != null) {
      filterValueByFieldType(team.getParameters(), false, FieldType.PASSWORD.value());
    }

    return team;
  }

  /*
   * Set default quotas
   * 
   * - Don't save the defaults against a team. Only retrieve dynamically.
   */
  private Quotas setDefaultQuotas() {
    Quotas quotas = new Quotas();
    quotas.setMaxWorkflowCount(Integer
        .valueOf(settingsService.getSettingConfig(TEAMS, MAX_TEAM_WORKFLOW_COUNT).getValue()));
    quotas.setMaxWorkflowExecutionMonthly(Integer.valueOf(
        settingsService.getSettingConfig(TEAMS, MAX_TEAM_WORKFLOW_EXECUTION_MONTHLY).getValue()));
    quotas.setMaxWorkflowStorage(Integer.valueOf(settingsService
        .getSettingConfig(TEAMS, MAX_TEAM_WORKFLOW_STORAGE).getValue().replace("Gi", "")));
    quotas.setMaxWorkflowExecutionTime(Integer
        .valueOf(settingsService.getSettingConfig(TEAMS, MAX_TEAM_WORKFLOW_DURATION).getValue()));
    quotas.setMaxConcurrentWorkflows(Integer
        .valueOf(settingsService.getSettingConfig(TEAMS, MAX_TEAM_CONCURRENT_WORKFLOW).getValue()));
    return quotas;
  }

  /*
   * Sets the custom quotes only for whats provided.
   * 
   * - Only store the set quotas on a team, so as not to override the defaults (which are retrieved
   * dynamically)
   */
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
            Optional.of(currentMonthEnd.getTimeInMillis()), Optional.empty(), Optional.empty(),
            Optional.of(List.of(teamId)));
    currentQuotas.setCurrentConcurrentWorkflows(insight.getConcurrentRuns().intValue());
    currentQuotas.setCurrentRunTotalDuration(insight.getTotalDuration().intValue());
    currentQuotas.setCurrentRunMedianDuration(insight.getMedianDuration().intValue());
    currentQuotas.setCurrentRuns(insight.getTotalRuns().intValue());

    // TODO update to only be active workflows
    List<String> workflowRefs = relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.WORKFLOW),
        Optional.empty(), Optional.of(RelationshipType.BELONGSTO),
        Optional.of(RelationshipRef.TEAM), Optional.of(List.of(teamId)));
    currentQuotas.setCurrentWorkflowCount(workflowRefs.size());

    // TODO look into this one
    currentQuotas.setCurrentPersistentStorage(null);
    return currentQuotas;
  }

  /*
   * Helper method to convert from ApproverGroup Entity to Model
   */
  private ApproverGroup convertEntityToApproverGroup(ApproverGroupEntity age) {
    ApproverGroup ag = new ApproverGroup(age);
    if (!age.getApproverRefs().isEmpty()) {
      age.getApproverRefs().forEach(ref -> {
        Optional<User> ue = identityService.getUserByID(ref);
        if (ue.isPresent()) {
          UserSummary u = new UserSummary(ue.get());
          ag.getApprovers().add(u);
        }
      });
    }
    return ag;
  }

  /*
   * Returns the List of UserSummary for a team
   */
  private List<UserSummary> getUsersForTeam(String teamId) {
    List<RelationshipEntity> userRels = relationshipService.getFilteredRels(Optional.of(RelationshipRef.USER),
        Optional.empty(), Optional.of(RelationshipType.MEMBEROF),
        Optional.of(RelationshipRef.TEAM), Optional.of(List.of(teamId)), true);
    List<UserSummary> teamUsers = new LinkedList<>();
    if (!userRels.isEmpty()) {
      userRels.forEach(rel -> {
        Optional<User> ue = identityService.getUserByID(rel.getFromRef());
        if (ue.isPresent()) {
          String role = TeamRoleEnum.READER.getLabel();
          if (rel.getData() != null && rel.getData().get("role") != null) {
            role = rel.getData().get("role").toString();
          }
          UserSummary u = new UserSummary(ue.get(), role);
          teamUsers.add(u);
        }
      });
    }
    return teamUsers;
  }

  /*
   * Creates a Relationship between User(s) and a Team
   * If relationship already exists, don't create a new one.
   * If user does not exist, a user record will be created with a relationship to the team
   * TODO - invite the user rather than create a relationship
   */
  private void createUserRelationships(String teamId, List<UserSummary> users) {
    List<String> userRefs = relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.USER),
        Optional.empty(), Optional.of(RelationshipType.MEMBEROF),
        Optional.of(RelationshipRef.TEAM), Optional.of(List.of(teamId)));
    if (users != null && !users.isEmpty()) {
      for (UserSummary userSummary : users) {
        LOGGER.debug("Requested User: " + userSummary.getId() + " - " + userSummary.getEmail());
        Optional<User> userEntity = null;
        //Find user by ID or Email - UI allows adding from existing or new (email)
        if (userSummary.getId() != null && !userSummary.getId().isEmpty()) {
          userEntity = identityService.getUserByID(userSummary.getId());
        } else if (userSummary.getEmail() != null && !userSummary.getEmail().isEmpty()) {
          userEntity = identityService.getUserByEmail(userSummary.getEmail());
        }
        //TODO - check the provided roles are roles that are in our system
        if (userEntity.isPresent() && !userRefs.contains(userEntity.get().getId())) {
          //Create team relationship for existing user
          relationshipService.addRelationshipRef(RelationshipRef.USER, userEntity.get().getId(), RelationshipType.MEMBEROF,
              RelationshipRef.TEAM, Optional.of(teamId), Optional.of(Map.of("role",userSummary.getRole())));
        } else {
          //Create new user record & relationship
          Optional<UserEntity> newUser = identityService.getAndRegisterUser(userSummary.getEmail(), null, null, Optional.of(UserType.user));
          if (newUser.isPresent()) {
            relationshipService.addRelationshipRef(RelationshipRef.USER, newUser.get().getId(), RelationshipType.MEMBEROF,
                RelationshipRef.TEAM, Optional.of(teamId),Optional.of(Map.of("role",userSummary.getRole())));
          }
          //TODO - throw error for user that can't be created?
        } 
      }
    }
  }
}
