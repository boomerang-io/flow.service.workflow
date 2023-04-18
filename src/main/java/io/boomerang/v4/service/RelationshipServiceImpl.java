package io.boomerang.v4.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import io.boomerang.mongo.model.TokenScope;
import io.boomerang.mongo.model.UserType;
import io.boomerang.security.model.TeamToken;
import io.boomerang.service.UserIdentityService;
import io.boomerang.v4.data.entity.RelationshipEntity;
import io.boomerang.v4.data.entity.UserEntity;
import io.boomerang.v4.data.repository.RelationshipRepository;
import io.boomerang.v4.model.enums.RelationshipRef;
import io.boomerang.v4.model.enums.RelationshipType;

@Service
public class RelationshipServiceImpl implements RelationshipService {

  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  private UserIdentityService userIdentityService;

  @Autowired
  private RelationshipRepository relationshipRepository;
  
  @Autowired
  private MongoTemplate mongoTemplate;
  
  /*
   * Creates a new RelationshipEntity for the provided inputs coupled with the current scope
   * 
   * TODO: figure out future workflow token scope 
   * 
   * @return RelationshipEntity
   */
  @Override
  public RelationshipEntity addRelationshipRefForCurrentScope(RelationshipRef fromType, String fromRef) {
    UserEntity user = null;
    RelationshipRef toType = null;
    String toRef = null;

    LOGGER.info("Current User Scope: " + userIdentityService.getCurrentScope());
    switch (userIdentityService.getCurrentScope()) {
      case user:
        user = userIdentityService.getCurrentUser();
        toType = RelationshipRef.USER;
        toRef = user.getId();
        break;
//      case workflow:
//        toType = RelationshipRefType.WORKFLOW;
//        WorkflowToken token = (WorkflowToken) userIdentityService.getRequestIdentity();
//        toRef = token.getWorkflowId();
//        break;        
      case team:
        toType = RelationshipRef.TEAM;
        TeamToken token = (TeamToken) userIdentityService.getRequestIdentity();
        toRef = token.getTeamId();
        break;
      case global:
        toType = RelationshipRef.GLOBAL;
        break;
    }
    
    return this.addRelationshipRef(fromType, fromRef, toType, Optional.of(toRef));
  }
  
  /*
   * Creates a new RelationshipEntity for the provided inputs
   * 
   * This method will map the RelationshipType based on the from and to
   * 
   * @return RelationshipEntity
   */
  @Override
  public RelationshipEntity addRelationshipRef(RelationshipRef fromType, String fromRef, RelationshipRef toType, Optional<String> toRef) {
    RelationshipType relationship = RelationshipType.BELONGSTO;
    if (RelationshipRef.USER.equals(fromType) && RelationshipRef.TEAM.equals(toType)) {
      relationship = RelationshipType.MEMBEROF;
      //TODO: future mapping
//    } else if (RelationshipRef.WORKFLOWRUN.equals(fromType) && RelationshipRef.WORKFLOW.equals(toType)) {
//      relationship = RelationshipType.EXECUTIONOF;
      //TODO: future with logging how it was initiated.
//    } else if (RelationshipRefType.WORKFLOWRUN.equals(fromType) && (RelationshipRefType.USER.equals(toType) || RelationshipRefType.TEAM.equals(toType) || RelationshipRefType.GLOBAL.equals(toType))) {
//      relationship = RelationshipType.initiatedBy;
    }
    
    RelationshipEntity relEntity = this.addRelationshipRef(fromType, fromRef, relationship, toType, toRef);
    return relationshipRepository.save(relEntity);
  }
  
  /*
   * Creates a new RelationshipEntity for the provided inputs. Requires RelationshipType
   * 
   * @return RelationshipEntity
   */
  @Override
  public RelationshipEntity addRelationshipRef(RelationshipRef fromType, String fromRef, RelationshipType relationship, RelationshipRef toType, Optional<String> toRef) {   
    RelationshipEntity relEntity = new RelationshipEntity();
    relEntity.setFrom(fromType);
    relEntity.setFromRef(fromRef);
    relEntity.setType(relationship);
    relEntity.setTo(toType);
    if (toRef.isPresent()) {
      relEntity.setToRef(toRef.get());
    }
    LOGGER.info("Relationship: " + relEntity.toString());
    return relationshipRepository.save(relEntity);
  }
  
//  /*
//   * Retrieve RelationshipEntity for the provided inputs
//   *
//   * 
//   * @return RelationshipEntity
//   */
//  @Override
//  public Optional<RelationshipEntity> getRelationship(RelationshipRefType fromType, String fromRef) {
//    return relationshipRepository.findByFromTypeAndFromRef(fromType, fromRef);
//  }
//  
  /*
   * Retrieve RelationshipEntity for the provided inputs
   *
   * 
   * @return RelationshipEntity
   */
  @Override
  public Optional<RelationshipEntity> getRelationship(RelationshipRef fromType, String fromRef, RelationshipType relationship) {
    return relationshipRepository.findByFromAndFromRefAndType(fromType, fromRef, relationship);
  }
  
//  /*
//   * Returns the RelationshipEntities for the matching criteria
//   * 
//   * @return RelationshipEntity List
//   */
//  @Override
//  public List<RelationshipEntity> getRelationships(RelationshipRefType fromType, String fromRef, RelationshipRefType toType, String toRef) {
//    return relationshipRepository.findAll(Example.of(this.createRelationshipRef(fromType, fromRef, toType, Optional.of(toRef))));
//  }
//  
//  /*
//   * Returns the RelationshipEntities for the matching criteria
//   * 
//   * @return RelationshipEntity List
//   */
//  @Override
//  public List<RelationshipEntity> getRelationships(RelationshipRefType fromType, String fromRef, RelationshipType relationship, RelationshipRefType toType, String toRef) {
//    return relationshipRepository.findAll(Example.of(this.createRelationshipRef(fromType, fromRef, relationship, toType, Optional.of(toRef))));
//  }
  
  /*
   * Removes all relationships
   */
  @Override
  public void removeRelationships(RelationshipRef fromType, List<String> fromRefs,
      RelationshipRef toType, List<String> toRefs) {
    List<RelationshipEntity> relEntities = relationshipRepository.findByFromAndFromRefInAndToAndToRefIn(fromType, fromRefs, toType, toRefs);
    if (!relEntities.isEmpty()) {
      relationshipRepository.deleteAll(relEntities);
    }
  }

//  /*
//   * Generates the Refs that the current security scope has access to, based on a specific type and optional lists of typeRefs, scopes, and teamIds 
//   * 
//   * @param RelationshipRef fromRef
//   * 
//   * @param RelatnshipType type
//   * 
//   * @param list of Refs: WorkflowRef, WorkflowRunRef, TaskTemplateRef, TaskRunRef
//   * 
//   * @param RelationshipRef toRef
//   * 
//   * @param list of Scopes
//   * 
//   * @param list of TeamIds
//   * 
//   * @return list of filtered Refs
//   */
//  @Override
//  public List<String> getFilteredRefs(RelationshipRef fromRef, Optional<List<String>> fromRefs, Optional<RelationshipType> type, Optional<RelationshipRef> toRef, 
//      Optional<List<String>> toRefs) {
//    UserEntity user = null;
//    List<String> refsList = new LinkedList<>();
//    if (type.isEmpty()) {
//      type = Optional.of(RelationshipType.BELONGSTO);
//    }
//
//    // TODO rename userIdentifyService to accessService or identityService
//    TokenScope scope = userIdentityService.getCurrentScope();
//    LOGGER.info("Current Access Scope: " + userIdentityService.getCurrentScope());
//    
//    // If User is Admin provide global access
//    if (TokenScope.user.equals(scope) && UserType.admin.equals(userIdentityService.getCurrentUser().getType())) {
//      scope = TokenScope.global;
//    }
//
//    switch (scope) {
//      case user:
//        // User is a special case. They could have access to User or Team based Refs
//        user = userIdentityService.getCurrentUser();
//        if (toRef.isPresent() && RelationshipRef.USER.equals(toRef.get())) {
//          List<String> userRefs = getRefsForUsers(fromRef, fromRefs, List.of(user.getId()));
//          refsList.addAll(userRefs.stream().filter(r -> toRefs.get().contains(r)).collect(Collectors.toList()));
//        } else if (!toRef.isPresent()) {
//          refsList.addAll(getRefsForUsers(fromRef, fromRefs, List.of(user.getId())));
//        }
//        //Ignore trying to get a relationship between a Team and a Team
//        if (!RelationshipRef.TEAM.equals(fromRef)) {
//          // Add refs based on teams the user is a 'Member Of'
//          // toRef either needs to be TEAM or not present (i.e. not filtered)
//          List<String> filteredTeams = new LinkedList<>();
//          if (toRef.isPresent() && RelationshipRef.TEAM.equals(toRef.get())) {
//            filteredTeams = getTeamsRefsByUsers(List.of(user.getId()));
//            filteredTeams = filteredTeams.stream().filter(r -> toRefs.get().contains(r)).collect(Collectors.toList());
//          } else if (!toRef.isPresent()) {
//            filteredTeams = getTeamsRefsByUsers(List.of(user.getId()));
//          }
//          refsList.addAll(getRefsForTeams(fromRef, fromRefs, filteredTeams));
//        }
//        break;
////      case workflow:
////        break;
//      case team:
//        // Add refs based on TeamTokens teamId
//        TeamToken token = (TeamToken) userIdentityService.getRequestIdentity();
//        //Ignore trying to get a relationship between a Team and a Team
//        if (!RelationshipRef.TEAM.equals(fromRef)) {
//          if (toRef.isPresent() && RelationshipRef.TEAM.equals(toRef.get())) {
//            if (toRefs.isPresent() && toRefs.get().contains(token.getTeamId()));
//            refsList.addAll(getRefsForTeams(fromRef, fromRefs, List.of(token.getTeamId())));
//          } else if (!toRef.isPresent()) {
//            refsList.addAll(getRefsForTeams(fromRef, fromRefs, List.of(token.getTeamId())));
//          } 
//        } else if (fromRefs.get().contains(token.getTeamId())) {
//          refsList.add(token.getTeamId());
//        }
//        break;
//      case global:
//        //Get All Refs unless filtered
//        if (toRef.isPresent() && RelationshipRef.USER.equals(toRef.get())) {
//          refsList.addAll(getRefsForUsers(fromRef, fromRefs, toRefs.get()));
//        } else if (toRef.isPresent() && RelationshipRef.TEAM.equals(toRef.get())) {
//          refsList.addAll(getRefsForTeams(fromRef, fromRefs, toRefs.get()));
//        } else if (toRef.isPresent() && RelationshipRef.SYSTEM.equals(toRef.get())) {
//          refsList.addAll(getRefsForSystem(fromRef));
//        } else if (toRef.isPresent() && RelationshipRef.TEMPLATE.equals(toRef.get())) {
//          refsList.addAll(getRefsForTemplate(fromRef));
//        } else if (!toRef.isPresent()) {
//          refsList.addAll(getRefsForAllUsers(fromRef));
//          refsList.addAll(getRefsForAllTeams(fromRef));
//          refsList.addAll(getRefsForTemplate(fromRef));
//          refsList.addAll(getRefsForSystem(fromRef));
//        }
//        break;
//    }        
//    
//    return refsList;
//  }
  
  /*
   * Generates the Refs that the current security scope has access to, based on a specific type and optional lists of typeRefs, scopes, and teamIds 
   * 
   * @param RelationshipRef fromRef
   * 
   * @param RelatnshipType type
   * 
   * @param list of Refs: WorkflowRef, WorkflowRunRef, TaskTemplateRef, TaskRunRef
   * 
   * @param RelationshipRef toRef
   * 
   * @param list of Scopes
   * 
   * @param list of TeamIds
   * 
   * @return list of filtered Refs
   */
  @Override
  public List<String> getFilteredRefs(Optional<RelationshipRef> from, Optional<List<String>> fromRefs, Optional<RelationshipType> type, Optional<RelationshipRef> to, 
      Optional<List<String>> toRefs) {
    
    //TODO: validation that we are not trying to get a relationship between two of the same objects or provide IDs with no context.
//    if (from.isEmpty() && fromRefs.isPresent()) {
//      throw new BoomerangException();
//    } else if (from.isPresent() && to.isPresent() && from.get().equals(to.get())) {
//      throw new BoomerangException();
//    }
    
    // Defaults if not provided
    if (type.isEmpty()) {
      type = Optional.of(RelationshipType.BELONGSTO);
    } else if (RelationshipType.MEMBEROF.equals(type.get())) {
      from = Optional.of(RelationshipRef.USER);
    }

    // TODO rename userIdentifyService to accessService or identityService
    TokenScope accessScope = userIdentityService.getCurrentScope();
    LOGGER.info("Current Access Scope: " + userIdentityService.getCurrentScope());
    
    // If User is Admin provide global access
    if (TokenScope.user.equals(accessScope) && UserType.admin.equals(userIdentityService.getCurrentUser().getType())) {
      accessScope = TokenScope.global;
    }

    switch (accessScope) {
      case user:
        // User is a special case. They could have access to User or Team based Refs
        UserEntity user = null;
        user = userIdentityService.getCurrentUser();
        if (from.isPresent() && RelationshipRef.USER.equals(from.get())) {
          fromRefs = Optional.of(List.of(user.getId()));
        } else if (RelationshipType.BELONGSTO.equals(type.get()) && to.isPresent() && RelationshipRef.USER.equals(to.get())) {
          toRefs = Optional.of(List.of(user.getId()));
        } else if (RelationshipType.BELONGSTO.equals(type.get()) && to.isPresent() && RelationshipRef.TEAM.equals(to.get())) {
         if (toRefs.isPresent()) {
           // If toRefs are provided (i.e. TeamIds) then filter to ones provided that the user has access to
           List<String> filteredTeams = getTeamsRefsByUsers(List.of(user.getId()));
           List<String> tempRefs = toRefs.get();
           toRefs = Optional.of(filteredTeams.stream().filter(r -> tempRefs.contains(r)).collect(Collectors.toList()));
         } else {
           toRefs = Optional.of(getTeamsRefsByUsers(List.of(user.getId())));
         }
        } else if (RelationshipType.BELONGSTO.equals(type.get()) && !to.isPresent()) {
          // If no TO is provided then set to both User and TeamRefs user has access to
          toRefs = Optional.of(List.of(user.getId())); 
          toRefs.get().addAll(getTeamsRefsByUsers(List.of(user.getId())));
        }
        break;
      case team:
        // Add refs based on TeamTokens teamId
        TeamToken token = (TeamToken) userIdentityService.getRequestIdentity();
        if (to.isPresent() && RelationshipRef.TEAM.equals(to.get())) {
          if (!toRefs.isPresent() || (toRefs.isPresent() && toRefs.get().contains(token.getTeamId()))) {
            toRefs = Optional.of(getRefsForTeams(from.get(), fromRefs, List.of(token.getTeamId())));
          }
        } else if (!to.isPresent()) {
          toRefs = Optional.of(List.of(token.getTeamId())); 
        }
        break;
      case global:
        // Allow anything with no filtering
        break;
    }
    
    // Create manual query for MongoDB
    List<Criteria> criteriaList = new ArrayList<>();

    if (from.isPresent()) {
      Criteria criteria = Criteria.where("from").is(from.get());
      criteriaList.add(criteria);
    }

    if (fromRefs.isPresent()) {
      Criteria criteria = Criteria.where("fromRef").in(fromRefs.get());
      criteriaList.add(criteria);
    }

    if (fromRefs.isPresent()) {
      Criteria criteria = Criteria.where("type").is(type.get());
      criteriaList.add(criteria);
    } else {
      //Default to 'Belongs To' primary relationship type
      Criteria criteria = Criteria.where("type").is(RelationshipType.BELONGSTO);
      criteriaList.add(criteria);
    }

    if (to.isPresent()) {
      Criteria criteria = Criteria.where("to").is(to.get());
      criteriaList.add(criteria);
    }

    if (toRefs.isPresent()) {
      Criteria criteria = Criteria.where("toRef").in(toRefs.get());
      criteriaList.add(criteria);
    }

    Criteria[] criteriaArray = criteriaList.toArray(new Criteria[criteriaList.size()]);
    Criteria allCriteria = new Criteria();
    if (criteriaArray.length > 0) {
      allCriteria.andOperator(criteriaArray);
    }
    Query query = new Query(allCriteria);
    
    List<RelationshipEntity> relEntities = mongoTemplate.find(query, RelationshipEntity.class);
    
    LOGGER.debug("Relationships Found: " + relEntities.toString());
    
    return relEntities.stream().map(RelationshipEntity::getFromRef).collect(Collectors.toList());
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
//  @Override
//  public List<String> getFilteredRefsForUserEmail(RelationshipRef fromRef,
//      Optional<List<String>> fromRefs, Optional<RelationshipType> type, Optional<RelationshipRef> toRef, 
//      Optional<List<String>> toRefs, String userEmail) {
//    UserEntity user = userIdentityService.getUserByEmail(userEmail);
//    Boolean isAdmin = false;
//    if (user != null && user.getType() == UserType.admin) {
//      isAdmin = true;
//    }
//    return getFilteredRefs(fromRef, fromRefs, type, toRef, toRefs);
//  }
  
//  /*
//   * Check if a Relationship exists with an object of that ID
//   * 
//   * This method can be used if noRefs are available but you need to check if the ID has already been used.
//   * 
//   *  @return boolean
//   */
//  @Override
//  public boolean doesRelationshipExist(RelationshipRefType type,
//      String fromRef) {
//    return relationshipRepository.findByFromTypeAndFromRef(type, fromRef).isPresent();
//  }
//  
//  /*
//   * Check if a Relationship exists with an object of that ID
//   * 
//   * This method can be used if noRefs are available but you need to check if the ID has already been used.
//   * 
//   *  @return boolean
//   */
//  private boolean doesRelationshipExist(RelationshipRefType type,
//      String fromRef, RelationshipType relationship) {
//    return relationshipRepository.findByFromTypeAndFromRefAndRelationship(type, fromRef, relationship).isPresent();
//  }

  private List<String> getRefsForTeams(RelationshipRef fromRef, Optional<List<String>> fromRefs, List<String> filteredTeams) {
    List<RelationshipEntity> relationships = null;
    if (fromRefs.isPresent()) {
      relationships = this.relationshipRepository.findByFromAndFromRefInAndTypeAndToAndToRefIn(fromRef, fromRefs.get(), RelationshipType.BELONGSTO,
          RelationshipRef.TEAM, filteredTeams);
    } else {
      relationships = this.relationshipRepository.findByFromAndTypeAndToAndToRefIn(fromRef, RelationshipType.BELONGSTO,
          RelationshipRef.TEAM, filteredTeams);
    }
    return relationships.stream().map(RelationshipEntity::getFromRef).collect(Collectors.toList());
  }

//  private List<String> getRefsForAllTeams(RelationshipRef fromRef) {
//    List<RelationshipEntity> relationships =
//        this.relationshipRepository.findByFromAndTypeAndTo(fromRef, RelationshipType.BELONGSTO, RelationshipRef.TEAM);
//    return relationships.stream().map(RelationshipEntity::getFromRef).collect(Collectors.toList());
//  }
//
//  private List<String> getRefsForSystem(RelationshipRef fromRef) {
//    List<RelationshipEntity> relationships =
//        this.relationshipRepository.findByFromAndTypeAndTo(fromRef, RelationshipType.BELONGSTO, RelationshipRef.SYSTEM);
//    return relationships.stream().map(RelationshipEntity::getFromRef).collect(Collectors.toList());
//  }
//
//  private List<String> getRefsForTemplate(RelationshipRef fromRef) {
//    List<RelationshipEntity> relationships =
//        this.relationshipRepository.findByFromAndTypeAndTo(fromRef, RelationshipType.BELONGSTO, RelationshipRef.TEMPLATE);
//    return relationships.stream().map(RelationshipEntity::getFromRef).collect(Collectors.toList());
//  }

//  private List<String> getRefsForUsers(RelationshipRef fromRef, Optional<List<String>> fromRefs, final List<String> users) {
//    List<RelationshipEntity> relationships = null;
//    if (fromRefs.isPresent()) {
//      relationships = this.relationshipRepository.findByFromAndFromRefInAndTypeAndToAndToRefIn(fromRef, fromRefs.get(), RelationshipType.BELONGSTO,
//          RelationshipRef.USER, users);
//    } else {
//      relationships = this.relationshipRepository.findByFromAndTypeAndToAndToRefIn(fromRef, RelationshipType.BELONGSTO,
//          RelationshipRef.USER, users);
//    }
//    return relationships.stream().map(RelationshipEntity::getFromRef).collect(Collectors.toList());
//  }

//  private List<String> getRefsForAllUsers(RelationshipRef fromRef) {
//    List<RelationshipEntity> relationships =
//        this.relationshipRepository.findByFromAndTypeAndTo(fromRef, RelationshipType.BELONGSTO, RelationshipRef.USER);
//    return relationships.stream().map(RelationshipEntity::getFromRef).collect(Collectors.toList());
//  }
//
  private List<String> getTeamsRefsByUsers(final List<String> userId) {
    List<RelationshipEntity> relationships = 
        this.relationshipRepository.findByFromAndFromRefInAndTypeAndTo(RelationshipRef.USER, userId, RelationshipType.MEMBEROF, RelationshipRef.TEAM);
    return relationships.stream().map(RelationshipEntity::getFromRef).collect(Collectors.toList());
  }
}
