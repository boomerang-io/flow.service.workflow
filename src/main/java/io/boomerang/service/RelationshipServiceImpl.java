package io.boomerang.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import io.boomerang.security.model.AuthType;
import io.boomerang.security.service.IdentityService;
import io.boomerang.v4.data.entity.RelationshipEntity;
import io.boomerang.v4.data.repository.RelationshipRepository;
import io.boomerang.v4.model.enums.RelationshipRef;
import io.boomerang.v4.model.enums.RelationshipType;

@Service
public class RelationshipServiceImpl implements RelationshipService {

  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  private IdentityService identityService;

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
    RelationshipType relationship = RelationshipType.BELONGSTO;
    RelationshipRef toType = null;
    String toRef = null;

    LOGGER.info("Current Access Scope: " + identityService.getCurrentScope());
    switch (identityService.getCurrentScope()) {
      case session:
      case user:
        toType = RelationshipRef.USER;
        toRef = identityService.getCurrentPrincipal();
        break;
      case workflow:
        toType = RelationshipRef.WORKFLOW;
        toRef = identityService.getCurrentPrincipal();
        break;        
      case team:
        toType = RelationshipRef.TEAM;
        toRef = identityService.getCurrentPrincipal();
        if (RelationshipRef.USER.equals(fromType)) {
          relationship = RelationshipType.MEMBEROF;
        }
        break;
      case global:
        toType = RelationshipRef.GLOBAL;
        break;
      default:
        break;
    }
    
    return this.addRelationshipRef(fromType, fromRef, relationship, toType, Optional.of(toRef), Optional.empty());
  }
  
  /*
   * Creates a new RelationshipEntity for the provided inputs. Requires RelationshipType
   * 
   * @return RelationshipEntity
   */
  @Override
  public RelationshipEntity addRelationshipRef(RelationshipRef fromType, String fromRef, RelationshipType relationship, RelationshipRef toType, Optional<String> toRef, Optional<Map<String, Object>> data) {   
    RelationshipEntity relEntity = new RelationshipEntity();
    relEntity.setFrom(fromType);
    relEntity.setFromRef(fromRef);
    relEntity.setType(relationship);
    relEntity.setTo(toType);
    if (toRef.isPresent()) {
      relEntity.setToRef(toRef.get());
    }
    if (data.isPresent()) {
      relEntity.setData(data.get());
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
  
  /*
   * Retrieve RelationshipEntityRef for the provided inputs
   *
   * 
   * @return RelationshipEntity
   */
  @Override
  public Optional<String> getRelationshipRef(RelationshipRef fromType, String fromRef, RelationshipType relationship) {
    Optional<RelationshipEntity> rel = getRelationship(fromType, fromRef, relationship);
    if (rel.isPresent()) {
      return Optional.of(rel.get().getToRef());
    }
    return Optional.empty();
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
  
  /*
   * Generates the TeamRefs for the current security scope with no elevated permissions.
   * 
   * This is used to return the /mine used by the web to load the Teams selection.
   */
  @Override
  public List<String> getMyTeamRefs() {
    return getFilteredRels(Optional.empty(), Optional.empty(), Optional.of(RelationshipType.MEMBEROF), Optional.of(RelationshipRef.TEAM), Optional.empty(), false).stream().map(RelationshipEntity::getToRef).collect(Collectors.toList());
  }
  
  /*
   * Generates the FromRefs that the current security scope has access to, based on a specific type and optional lists of typeRefs, scopes, and teamIds 
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
   * @return list of filtered FromRefs
   */
  @Override
  public List<String> getFilteredFromRefs(Optional<RelationshipRef> from, Optional<List<String>> fromRefs, Optional<RelationshipType> type, Optional<RelationshipRef> to, 
      Optional<List<String>> toRefs) {
    return getFilteredRels(from, fromRefs, type, to, toRefs, true).stream().map(RelationshipEntity::getFromRef).collect(Collectors.toList());
  }
  
  /*
   * Generates the ToRefs that the current security scope has access to, based on a specific type and optional lists of typeRefs, scopes, and teamIds 
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
   * @return list of filtered FromRefs
   */
  @Override
  public List<String> getFilteredToRefs(Optional<RelationshipRef> from, Optional<List<String>> fromRefs, Optional<RelationshipType> type, Optional<RelationshipRef> to, 
      Optional<List<String>> toRefs) {
    return getFilteredRels(from, fromRefs, type, to, toRefs, true).stream().map(RelationshipEntity::getToRef).collect(Collectors.toList());
  }
  
  /*
   * Retrieves RelationshipEntities that the current security scope has access to, based on a specific type and optional lists of typeRefs, scopes, and teamIds 
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
   * @return filtered RelationshipEntities
   */
  @Override
  public List<RelationshipEntity> getFilteredRels(Optional<RelationshipRef> from, Optional<List<String>> fromRefs, Optional<RelationshipType> type, Optional<RelationshipRef> to, 
      Optional<List<String>> toRefs, boolean elevate) {
    
    //TODO Validation that we are not trying to get a relationship between two of the same objects or provide IDs with no context.
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

    AuthType accessScope = identityService.getCurrentScope();
    LOGGER.info("RelationshipFilter() - Access Scope: " + identityService.getCurrentScope());
    
    // If User is Admin provide global access
    // MEMBEROF requests are ignored as we only want to return that users Teams and as such don't elevate the scope
    if (elevate && (AuthType.session.equals(accessScope) || AuthType.user.equals(accessScope)) && identityService.isCurrentUserAdmin()) {
      LOGGER.info("RelationshipFilter() - Identity is Admin - Elevating permissions.");
      accessScope = AuthType.global;
    }

    switch (accessScope) {
      case session:
      case user:
        String userId = identityService.getCurrentPrincipal();
        if (from.isPresent() && RelationshipRef.USER.equals(from.get())) {
          fromRefs = Optional.of(List.of(userId));
        } else if (to.isPresent() && RelationshipRef.TEAM.equals(to.get())) {
          List<String> filteredTeams = getTeamsRefsByUsers(List.of(userId));
         if (toRefs.isPresent()) {
           // If toRefs are provided (i.e. TeamIds) then filter to ones provided that the user has access to
           List<String> tempRefs = toRefs.get();
           toRefs = Optional.of(filteredTeams.stream().filter(r -> tempRefs.contains(r)).collect(Collectors.toList()));
         } else {
           toRefs = Optional.of(filteredTeams);
         }
        } 
        break;
      case workflow:
        // Add refs based on Workflow
        // Will either set toRef to the WorkflowID or make sure its in the list of provided Refs
        String workflowId = identityService.getCurrentPrincipal();
        if (to.isPresent() && RelationshipRef.WORKFLOW.equals(to.get())) {
          if (!toRefs.isPresent() || (toRefs.isPresent() && toRefs.get().contains(workflowId))) {
            toRefs = Optional.of(List.of(workflowId));
          }
        } else if (!to.isPresent()) {
          toRefs = Optional.of(List.of(workflowId)); 
        }
        break;
      case team:
        // Add refs based on Tokens Team
        String teamId = identityService.getCurrentPrincipal();
        if (to.isPresent() && RelationshipRef.TEAM.equals(to.get())) {
          if (!toRefs.isPresent() || (toRefs.isPresent() && toRefs.get().contains(teamId))) {
            toRefs = Optional.of(List.of(teamId)); 
          }
        } else if (!to.isPresent()) {
          toRefs = Optional.of(List.of(teamId)); 
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

    if (from.isPresent()) {
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
    
    return relEntities;
  }

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
