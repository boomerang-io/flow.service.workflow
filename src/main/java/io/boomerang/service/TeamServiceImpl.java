package io.boomerang.service;

import static io.boomerang.util.DataAdapterUtil.filterValueByFieldType;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Calendar;
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
import io.boomerang.security.model.RoleEnum;
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
import io.boomerang.v4.model.TeamMember;
import io.boomerang.v4.model.TeamNameCheckRequest;
import io.boomerang.v4.model.TeamRequest;
import io.boomerang.v4.model.User;
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
   * Retrieve a single team by ID
   */
  @Override
  public Team get(String teamId) {
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

    return convertTeamEntityToTeam(entity.get());
  }

  /*
   * Creates a new Team
   * 
   * - Name must not be blank
   */
  @Override
  public Team create(TeamRequest request, TeamType type) {
    if (!request.getName().isBlank()) {
      //Validate name - future when Name is Slug
//        String teamName = firstName + "'s Team";
//        TeamNameCheckRequest checkRequest = new TeamNameCheckRequest(teamName);
//        boolean validName = false;
//        Integer teamNumber = 1;
//        while (!validName) {
//          try {
//            teamService.validateName(checkRequest);
//            validName = true;
//          } catch (Exception e) {
//            teamName = teamName + " " + teamNumber;
//          }
//        }
      
      TeamEntity teamEntity = new TeamEntity();
      teamEntity.setType(type); // Only create TeamType.Team in most cases - User and System are internally created.
      /*
       * Copy majority of fields. 
       * - Status is ignored - can only be active
       * - Members, quotas, parameters, and approverGroups need further logic
       */
      
      BeanUtils.copyProperties(request, teamEntity, "id", "status", "members", "quotas", "parameters", "approverGroups");

      // Set custom quotas
      // Don't set default quotas as they can change over time and should be dynamic
      Quotas quotas = new Quotas();
      // Override quotas based on creation request
      setCustomQuotas(quotas, request.getQuotas());
      teamEntity.setQuotas(quotas);
      
      // Create / Update Parameters
      if (request.getParameters() != null && !request.getParameters().isEmpty()) {
        createOrUpdateParameters(teamEntity, request.getParameters());
      }
      
      // Create / Update ApproverGroups
      if (request.getApproverGroups() != null && !request.getApproverGroups().isEmpty()) {
        createOrUpdateApproverGroups(teamEntity, request.getApproverGroups());
      }

      teamEntity = teamRepository.save(teamEntity);

      // Create Relationships for Users
      createOrUpdateUserRelationships(teamEntity.getId(), request.getMembers());
      
      return convertTeamEntityToTeam(teamEntity);
    } else {
      // TODO: make this a better error for unable to create team i.e. name is mandatory
      throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
    }
  }

  /*
   * Patch team
   */
  @Override
  public Team patch(String teamId, TeamRequest request) {
    if (request != null) {
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
      if (request.getName() != null && !request.getName().isBlank()) {
        teamEntity.setName(request.getName());
      }
      if (request.getStatus() != null) {
        teamEntity.setStatus(request.getStatus());
      }
      if (request.getExternalRef() != null && !request.getExternalRef().isBlank()) {
        teamEntity.setExternalRef(request.getExternalRef());
      }
      if (request.getLabels() != null && !request.getLabels().isEmpty()) {
        teamEntity.getLabels().putAll(request.getLabels());
      }

      // Set custom quotas
      // Don't set default quotas as they can change over time and should be dynamic
      Quotas quotas = new Quotas();
      // Override quotas based on creation request
      setCustomQuotas(quotas, request.getQuotas());
      teamEntity.setQuotas(quotas);
      
      // Create / Update Parameters
      if (request.getParameters() != null && !request.getParameters().isEmpty()) {
        createOrUpdateParameters(teamEntity, request.getParameters());
      }
      
      // Create / Update ApproverGroups
      if (request.getApproverGroups() != null && !request.getApproverGroups().isEmpty()) {
        createOrUpdateApproverGroups(teamEntity, request.getApproverGroups());
      }

      teamRepository.save(teamEntity);

      // Create / Update Relationships for Users
      createOrUpdateUserRelationships(teamEntity.getId(), request.getMembers());
      return convertTeamEntityToTeam(teamEntity);
    }
    throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
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
  public void removeMembers(String teamId, List<TeamMember> request) {
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
      for (TeamMember userSummary : request) {
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

  @Override
  /*
   *  Allows only the requesting user to leave the team
   *  
   *  TODO: ensure the remaining owner cannot leave the team
   */
  public void leave(String teamId) {
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
      relationshipService.removeUserTeamRelationship(teamId);
  }

  /*
   * Creates or Updates Team Parameters
   */
  private List<AbstractParam> createOrUpdateParameters(TeamEntity teamEntity, List<AbstractParam> request) {
    List<AbstractParam> parameters = teamEntity.getParameters();
    for (AbstractParam r : request) {
      AbstractParam parameter = parameters.stream()
          .filter(p -> p.getKey().equals(r.getKey())).findAny().orElse(null);
      //TODO make this better
      if (parameter != null) {
        parameters.remove(parameter);
        BeanUtils.copyProperties(r, parameter);
      }
      parameters.add(parameter);
    }

    return parameters;
  }

  /*
   * Delete parameters by key
   */
  @Override
  public void deleteParameters(String teamId, List<String> request) {
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
      for (String r : request) {
        AbstractParam parameter =
            parameters.stream().filter(p -> p.getKey().equals(r)).findAny().orElse(null);
  
        if (parameter != null) {
          parameters.remove(parameter);
        }
      }
      teamEntity.setParameters(parameters);
      teamRepository.save(teamEntity);
    }

    // TODO better exception for unable find key
    throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
  }
  /*
   * Create & Update Approver Group
   * 
   * - Creates a relationship against a team
   * - ApproverGroup name must be unique per team
   */
  private void createOrUpdateApproverGroups(TeamEntity teamEntity, List<ApproverGroupRequest> request) {
    //Retrieve ApproverGroups by relationship as they are stored separately to the TeamEntity
    List<String> approverGroupRefs =
        relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.APPROVERGROUP),
            Optional.empty(), Optional.of(RelationshipType.BELONGSTO),
            Optional.of(RelationshipRef.TEAM), Optional.of(List.of(teamEntity.getId())));
    
    List<ApproverGroupEntity> approverGroupEntities =
        approverGroupRepository.findByIdIn(approverGroupRefs);
    
    for (ApproverGroupRequest r : request) {
      //Ensure ApproverGroupName is not blank or null
      if (r.getName() == null || r.getName().isBlank()) {
        // TODO better exception for invalid Team Parameter Name
        throw new BoomerangException(BoomerangError.TEAM_INVALID_REF);
      }

      ApproverGroupEntity age = approverGroupEntities.stream()
          .filter(e -> e.getName().equalsIgnoreCase(r.getName())).findFirst().orElse(null);
      
      if (age != null) {
        LOGGER.debug("Existing ApproverGroup: " + age.toString());
        // ApproverGroup already exists - update
        approverGroupEntities.remove(age);
        age.setName(r.getName());

        //Ensure each approver is a valid team member
        if (r.getApprovers() != null) {
          List<String> userRefs = relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.USER),
              Optional.empty(), Optional.of(RelationshipType.MEMBEROF),
              Optional.of(RelationshipRef.TEAM), Optional.of(List.of(teamEntity.getId())));
          LOGGER.debug("User Refs: " + userRefs.toString());
          List<String> validApproverRefs = r.getApprovers().stream()
              .filter(a -> userRefs.contains(a)).collect(Collectors.toList());
          LOGGER.debug("Valid Approver Refs: " + validApproverRefs.toString());
          age.setApprovers(validApproverRefs);

          age = approverGroupRepository.save(age);
        }
      } else {
        // ApproverGroup + Relationship needs creating
        ApproverGroupEntity approverGroupEntity = new ApproverGroupEntity();
        approverGroupEntity.setName(r.getName());
        if (r.getApprovers() != null) {
          List<String> userRefs = relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.USER),
              Optional.empty(), Optional.of(RelationshipType.MEMBEROF),
              Optional.of(RelationshipRef.TEAM), Optional.of(List.of(teamEntity.getId())));
          LOGGER.debug("User Refs: " + userRefs.toString());
          List<String> validApproverRefs = r.getApprovers().stream()
              .filter(a -> userRefs.contains(a)).collect(Collectors.toList());
          LOGGER.debug("Valid Approver Refs: " + validApproverRefs.toString());
          approverGroupEntity.setApprovers(validApproverRefs);
        }
        approverGroupEntity = approverGroupRepository.save(approverGroupEntity);
        relationshipService.addRelationshipRef(RelationshipRef.APPROVERGROUP,
            approverGroupEntity.getId(), RelationshipType.BELONGSTO, RelationshipRef.TEAM, Optional.of(teamEntity.getId()), Optional.empty());
      }
    }
  }

  /*
   * Delete an Approver Group
   * 
   * - Removes relationship as well
   */
  @Override
  public void deleteApproverGroups(String teamId, List<String> request) {
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
    
    for (String r : request) {
      Optional<ApproverGroupEntity> ag = approverGroupRepository.findById(r);
      if (ag.isPresent()) {
        approverGroupRepository.deleteById(r);
        relationshipService.removeRelationships(RelationshipRef.APPROVERGROUP,
          List.of(r), RelationshipRef.TEAM, List.of(teamId));
      }
    }
  }

  /*
   * Delete custom quotas on the team and reset back to default
   */
  @Override
  public void deleteCustomQuotas(String teamId) {
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
  }

  /*
   * Reset quotas to default (i.e. delete custom quotas on the team)
   */
  @Override
  public ResponseEntity<Quotas> getDefaultQuotas() {
    return ResponseEntity.ok(setDefaultQuotas());
  }
  
  /*
   * Used by WorkflowRun Service to ensure Workflow can run
   */
  public CurrentQuotas getQuotas(String teamId) {
    Optional<TeamEntity> optTeamEntity = teamRepository.findById(teamId);
    if (optTeamEntity.isPresent()) {
      Quotas quotas = setDefaultQuotas();
      setCustomQuotas(quotas, optTeamEntity.get().getQuotas());
      CurrentQuotas currentQuotas = new CurrentQuotas(quotas);
      setCurrentQuotas(currentQuotas, optTeamEntity.get().getId());
      return currentQuotas;
    }
    return null;
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
    
    // Get Members
    team.setMembers(getUsersForTeam(teamEntity.getId()));

    // Get default & custom stored Quotas
    Quotas quotas = setDefaultQuotas();
    setCustomQuotas(quotas, teamEntity.getQuotas());
    CurrentQuotas currentQuotas = new CurrentQuotas(quotas);
    setCurrentQuotas(currentQuotas, teamEntity.getId());
    team.setQuotas(currentQuotas);

    // Get Approver Groups
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
    if (!age.getApprovers().isEmpty()) {
      age.getApprovers().forEach(ref -> {
        Optional<User> ue = identityService.getUserByID(ref);
        if (ue.isPresent()) {
          TeamMember u = new TeamMember(ue.get());
          ag.getApprovers().add(u);
        }
      });
    }
    return ag;
  }

  /*
   * Returns the List of UserSummary for a team
   */
  private List<TeamMember> getUsersForTeam(String teamId) {
    List<RelationshipEntity> userRels = relationshipService.getFilteredRels(Optional.of(RelationshipRef.USER),
        Optional.empty(), Optional.of(RelationshipType.MEMBEROF),
        Optional.of(RelationshipRef.TEAM), Optional.of(List.of(teamId)), true);
    List<TeamMember> teamUsers = new LinkedList<>();
    if (!userRels.isEmpty()) {
      userRels.forEach(rel -> {
        Optional<User> ue = identityService.getUserByID(rel.getFromRef());
        if (ue.isPresent()) {
          String role = RoleEnum.READER.getLabel();
          if (rel.getData() != null && rel.getData().get("role") != null) {
            role = rel.getData().get("role").toString();
          }
          TeamMember u = new TeamMember(ue.get(), role);
          teamUsers.add(u);
        }
      });
    }
    return teamUsers;
  }

  /*
   * Creates a Relationship between User(s) and a Team
   * If relationship already exists, patch the role.
   * If user does not exist, a user record will be created with a relationship to the team
   * TODO - invite the user rather than create a relationship
   */
  private void createOrUpdateUserRelationships(String teamId, List<TeamMember> users) {
    List<String> userRefs = relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.USER),
        Optional.empty(), Optional.of(RelationshipType.MEMBEROF),
        Optional.of(RelationshipRef.TEAM), Optional.of(List.of(teamId)));
    if (users != null && !users.isEmpty()) {
      for (TeamMember userSummary : users) {
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
        } else if (userEntity.isPresent() && userRefs.contains(userEntity.get().getId())) {
          //Patch team relationship for existing team member
          relationshipService.patchRelationshipData(RelationshipRef.USER, userEntity.get().getId(), RelationshipType.MEMBEROF, Map.of("role",userSummary.getRole()));
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
