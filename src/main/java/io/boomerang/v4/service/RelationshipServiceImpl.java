package io.boomerang.v4.service;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import io.boomerang.mongo.model.UserType;
import io.boomerang.security.model.TeamToken;
import io.boomerang.service.UserIdentityService;
import io.boomerang.v4.data.entity.RelationshipEntity;
import io.boomerang.v4.data.entity.TeamEntity;
import io.boomerang.v4.data.entity.UserEntity;
import io.boomerang.v4.data.repository.RelationshipRepository;
import io.boomerang.v4.model.enums.RelationshipRefType;
import io.boomerang.v4.model.enums.RelationshipType;

@Service
public class RelationshipServiceImpl implements RelationshipService {

  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  private UserIdentityService userIdentityService;

  @Autowired
  private RelationshipRepository relationshipRepository;

  @Autowired
  private TeamService teamService;

  // @Autowired
  // private FlowWorkflowService flowWorkflowService;
  //
  // /*
  // * Converts from ActivityEntity DB model to consumable FlowActivity
  // *
  // * @param list of ActivityEntity's
  // * @return list of FlowActivity
  // */
  // @Override
  // public List<FlowActivity> convertActivityEntityToFlowActivity(List<ActivityEntity> records) {
  //
  // final List<FlowActivity> flowActivities = new LinkedList<>();
  //
  // for (final ActivityEntity record : records) {
  // final FlowActivity flow = new FlowActivity(record);
  // final WorkflowEntity workflow = flowWorkflowService.getWorkflow(record.getWorkflowId());
  //
  // if (workflow != null) {
  // flow.setWorkflowName(workflow.getName());
  // flow.setDescription(workflow.getDescription());
  // flow.setIcon(workflow.getIcon());
  // flow.setShortDescription(workflow.getShortDescription());
  // }
  //
  // flowActivities.add(flow);
  // }
  // return flowActivities;
  // }
  
  /*
   * Creates a new RelationshipEntity for the provided inputs
   * 
   * @return RelationshipEntity
   */
  @Override
  public RelationshipEntity createRelationshipRef(RelationshipRefType fromType, String fromRef) {
    UserEntity user = null;
    RelationshipRefType toType = null;
    String toRef = null;
    RelationshipType relationship = RelationshipType.belongsTo;

    LOGGER.info("Current User Scope: " + userIdentityService.getCurrentScope());
    switch (userIdentityService.getCurrentScope()) {
      case user:
        user = userIdentityService.getCurrentUser();
        toType = RelationshipRefType.USER;
        toRef = user.getId();
        break;
      case team:
        toType = RelationshipRefType.TEAM;
        TeamToken token = (TeamToken) userIdentityService.getRequestIdentity();
        toRef = token.getTeamId();
        break;
      case global:
        toType = RelationshipRefType.GLOBAL;
        break;
    }
    
    if (RelationshipRefType.USER.equals(fromType) && RelationshipRefType.TEAM.equals(toType)) {
      relationship = RelationshipType.memberOf;
    }
    
    RelationshipEntity relEntity = new RelationshipEntity();
    relEntity.setFromType(fromType);
    relEntity.setFromRef(fromRef);
    relEntity.setRelationship(relationship);
    relEntity.setToType(toType);
    relEntity.setToRef(toRef);
    LOGGER.info("Relationship: " + relEntity.toString());
    return relationshipRepository.save(relEntity);
  }
  
  /*
   * Creates a new RelationshipEntity for the provided inputs
   * 
   * @return RelationshipEntity
   */
  @Override
  public RelationshipEntity createRelationshipRef(RelationshipRefType fromType, String fromRef, RelationshipRefType toType, String toRef) {
    RelationshipType relationship = RelationshipType.belongsTo;
    if (RelationshipRefType.USER.equals(fromType) && RelationshipRefType.TEAM.equals(toType)) {
      relationship = RelationshipType.memberOf;
    }
    
    RelationshipEntity relEntity = new RelationshipEntity();
    relEntity.setFromType(fromType);
    relEntity.setFromRef(fromRef);
    relEntity.setRelationship(relationship);
    relEntity.setToType(toType);
    relEntity.setToRef(toRef);
    LOGGER.info("Relationship: " + relEntity.toString());
    return relationshipRepository.save(relEntity);
  }
  
  /*
   * Retrieve RelationshipEntity for the provided inputs
   *
   * 
   * @return RelationshipEntity
   */
  @Override
  public Optional<RelationshipEntity> getRelationship(RelationshipRefType fromType, String fromRef) {
    return relationshipRepository.findByFromTypeAndFromRef(fromType, fromRef);
  }
  
  /*
   * Returns the RelationshipEntities for the matching criteria
   * 
   * @return RelationshipEntity List
   */
  @Override
  public List<RelationshipEntity> getRelationship(RelationshipRefType fromType, String fromRef, RelationshipRefType toType, String toRef) {
    return relationshipRepository.findAll(Example.of(this.createRelationshipRef(fromType, fromRef, toType, toRef)));
  }
  
  /*
   * Removes all relationships
   */
  @Override
  public void removeRelationships(RelationshipRefType fromType, List<String> fromRefs,
      RelationshipRefType toType, List<String> toRefs) {
    List<RelationshipEntity> relEntities = relationshipRepository.findByFromTypeAndFromRefInAndToTypeAndToRefIn(fromType, fromRefs, toType, toRefs);
    if (!relEntities.isEmpty()) {
      relationshipRepository.deleteAll(relEntities);
    }
  }

  /*
   * Generates the Refs based on a specific type and optional lists of typeRefs, scopes, and teamIds
   * 
   * @param RelationshipRefType type
   * 
   * @param list of TypeRefs: WorkflowRef, WorkflowRunRef, TaskTemplateRef, TaskRunRef
   * 
   * @param list of Scopes
   * 
   * @param list of TeamIds
   * 
   * @return list of filtered Refs
   */
  @Override
  public List<String> getFilteredRefs(RelationshipRefType type, Optional<List<String>> typeRefs,
      Optional<List<String>> teamIds, Optional<List<String>> scopes) {
    UserEntity user = null;
    Boolean isAdmin = false;

    LOGGER.info("Current User Scope: " + userIdentityService.getCurrentScope());
    switch (userIdentityService.getCurrentScope()) {
      case user:
        user = userIdentityService.getCurrentUser();
        if (user.getType() == UserType.admin) {
          isAdmin = true;
        }
        break;
      case team:

        break;
      case global:
        isAdmin = true;
        break;
    }

    return getFilteredRefsList(type, typeRefs, teamIds, scopes, user, isAdmin);
  }

  /*
   * Generates the Refs based on a specific type and optional lists of typeRefs, scopes, and teamIds
   * 
   * @param RelationshipRefType type
   * 
   * @param list of TypeRefs: WorkflowRef, WorkflowRunRef, TaskTemplateRef, TaskRunRef
   * 
   * @param list of Scopes
   * 
   * @param list of TeamIds
   * 
   * @param FlowUserEntity user
   * 
   * @return list of filtered Refs
   */
  @Override
  public List<String> getFilteredRefsForUserEmail(RelationshipRefType type,
      Optional<List<String>> typeRefs, Optional<List<String>> teamIds,
      Optional<List<String>> scopes, String userEmail) {
    UserEntity user = userIdentityService.getUserByEmail(userEmail);
    Boolean isAdmin = false;
    if (user != null && user.getType() == UserType.admin) {
      isAdmin = true;
    }
    return getFilteredRefsList(type, typeRefs, teamIds, scopes, user, isAdmin);
  }
  
  /*
   * Check if a Relationship exists with an object of that ID
   * 
   * This method can be used if noRefs are available but you need to check if the ID has already been used.
   * 
   *  @return boolean
   */
  @Override
  public boolean doesRelationshipExist(RelationshipRefType type,
      String fromRef) {
    Optional<RelationshipEntity> relationship = relationshipRepository.findByFromTypeAndFromRef(type, fromRef);
    return relationship.isPresent();
  }

  private List<String> getFilteredRefsList(RelationshipRefType type,
      Optional<List<String>> typeRefs, Optional<List<String>> teamIds,
      Optional<List<String>> scopes, UserEntity user, Boolean isAdmin) {
    List<String> refsList = new LinkedList<>();
    if (!typeRefs.isPresent()) {
      if (scopes.isPresent() && !scopes.get().isEmpty()) {
        List<String> scopeList = scopes.get();
        if (scopeList.contains("user") && user != null) {
          addUserWorkflows(type, user, refsList);
        }
        if (scopeList.contains("system") && isAdmin) {
          addSystemWorkflows(type, refsList);
        }
        if (scopeList.contains("team")) {
          addTeamRefs(type, isAdmin, user, refsList, teamIds);
        }
      } else if (teamIds.isPresent() && !teamIds.get().isEmpty()) {
        addTeamRefs(type, isAdmin, user, refsList, teamIds);
      } else {
        if (user != null) {
          addUserWorkflows(type, user, refsList);
        }
        addTeamRefs(type, isAdmin, user, refsList, teamIds);
        if (isAdmin) {
          addSystemWorkflows(type, refsList);
        }
      }
    } else {
      // TODO: why is there no validation here?
      // I think this should be returning a filtered list of validated IDs based on what was
      // provided i.e. validatedList.contains(providedList)
      List<String> requestWorkflowList = typeRefs.get();
      refsList.addAll(requestWorkflowList);
    }
    return refsList;
  }

  private void addTeamRefs(RelationshipRefType type, Boolean isAdmin, final UserEntity user,
      List<String> refsList, Optional<List<String>> teamIds) {
    List<RelationshipEntity> relationships = null;
    if (teamIds.isPresent() && !teamIds.get().isEmpty()) {
      relationships = this.relationshipRepository.findByFromTypeAndToTypeAndToRefIn(type,
          RelationshipRefType.TEAM, teamIds.get());
    } else {
      if (isAdmin) {
        relationships = this.relationshipRepository.findByFromTypeAndToType(type, RelationshipRefType.TEAM);
      } else if (user != null) {
        List<RelationshipEntity> userRefs = this.relationshipRepository.findByFromTypeAndFromRefAndToType(RelationshipRefType.USER, user.getId(), RelationshipRefType.TEAM);
        List<String> teamRefs =
            userRefs.stream().map(RelationshipEntity::getToRef).collect(Collectors.toList());
        relationships = this.relationshipRepository.findByFromTypeAndToTypeAndToRefIn(type,
            RelationshipRefType.TEAM, teamRefs);
      }
    }
    if (relationships != null) {
      List<String> teamWorkflowsIds =
          relationships.stream().map(RelationshipEntity::getFromRef).collect(Collectors.toList());
      refsList.addAll(teamWorkflowsIds);
    }
  }

  private void addSystemWorkflows(RelationshipRefType type, List<String> refsList) {
    List<RelationshipEntity> relationships =
        this.relationshipRepository.findByFromTypeAndToType(type, RelationshipRefType.SYSTEM);
    List<String> systemWorkflowsIds =
        relationships.stream().map(RelationshipEntity::getFromRef).collect(Collectors.toList());
    refsList.addAll(systemWorkflowsIds);
  }

  private void addUserWorkflows(RelationshipRefType type, final UserEntity user, List<String> refsList) {
    String userId = user.getId();
    List<RelationshipEntity> relationships =
        this.relationshipRepository.findByFromTypeAndToTypeAndToRef(type,  RelationshipRefType.USER, userId);
    List<String> userWorkflowIds =
        relationships.stream().map(RelationshipEntity::getFromRef).collect(Collectors.toList());
    refsList.addAll(userWorkflowIds);
  }
}
