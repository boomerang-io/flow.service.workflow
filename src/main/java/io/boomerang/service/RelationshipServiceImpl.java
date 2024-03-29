package io.boomerang.service;

import java.util.ArrayList;
import java.util.LinkedList;
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
import io.boomerang.data.entity.RelationshipEntity;
import io.boomerang.data.repository.RelationshipRepository;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.model.enums.RelationshipType;
import io.boomerang.model.enums.RelationshipLabel;
import io.boomerang.security.model.AuthType;
import io.boomerang.security.model.RoleEnum;
import io.boomerang.security.service.IdentityService;

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
   * @return RelationshipEntity
   */
  @Override
  public RelationshipEntity addRelationshipRefForCurrentScope(RelationshipType fromType, String fromRef) {
    RelationshipLabel relationship = RelationshipLabel.BELONGSTO;
    RelationshipType toType = null;
    String toRef = null;

    LOGGER.info("Current Access Scope: " + identityService.getCurrentScope());
    switch (identityService.getCurrentScope()) {
      case session:
      case user:
        toType = RelationshipType.USER;
        toRef = identityService.getCurrentPrincipal();
        break;
      case workflow:
        toType = RelationshipType.WORKFLOW;
        toRef = identityService.getCurrentPrincipal();
        break;        
      case team:
        toType = RelationshipType.TEAM;
        toRef = identityService.getCurrentPrincipal();
        if (RelationshipType.USER.equals(fromType)) {
          relationship = RelationshipLabel.MEMBEROF;
        }
        break;
      case global:
        toType = RelationshipType.GLOBAL;
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
  public RelationshipEntity addRelationshipRef(RelationshipType fromType, String fromRef, RelationshipLabel relationship, RelationshipType toType, Optional<String> toRef, Optional<Map<String, Object>> data) {   
    RelationshipEntity relEntity = new RelationshipEntity();
    relEntity.setFrom(fromType);
    relEntity.setFromRef(fromRef);
    relEntity.setLabel(relationship);
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
  
  /*
   * Patch a RelationshipEntity
   * 
   * @return RelationshipEntity
   */
  @Override
  public RelationshipEntity patchRelationshipData(RelationshipType fromType, String fromRef, RelationshipLabel relationship, Map<String, Object> data) {
    Optional<RelationshipEntity> entity = getRelationship(fromType, fromRef, relationship);
    if (entity.isPresent()) {
        entity.get().getData().putAll(data);
      LOGGER.info("Relationship: " + entity.get().toString());
      return relationshipRepository.save(entity.get());
    } 
    throw new BoomerangException(BoomerangError.UNABLE_PATCH_REL);
  }
//  
  /*
   * Retrieve RelationshipEntity for the provided inputs
   *
   * 
   * @return RelationshipEntity
   */
  @Override
  public Optional<RelationshipEntity> getRelationship(RelationshipType fromType, String fromRef, RelationshipLabel relationship) {
    return relationshipRepository.findByFromAndFromRefAndType(fromType, fromRef, relationship);
  }
  
  /*
   * Retrieve RelationshipEntityRef for the provided inputs
   *
   * 
   * @return RelationshipEntity
   */
  @Override
  public Optional<String> getRelationshipRef(RelationshipType fromType, String fromRef, RelationshipLabel relationship) {
    Optional<RelationshipEntity> rel = getRelationship(fromType, fromRef, relationship);
    if (rel.isPresent()) {
      return Optional.of(rel.get().getToRef());
    }
    return Optional.empty();
  }
  
  /*
   * Removes a relationship by ID
   */
  @Override
  public void removeRelationshipById(String id) {
      relationshipRepository.deleteById(id);
  }
  
  /*
   * Removes all relationships
   */
  @Override
  public void removeRelationships(RelationshipType fromType, List<String> fromRefs,
      RelationshipType toType, List<String> toRefs) {
    List<RelationshipEntity> relEntities = relationshipRepository.findByFromAndFromRefInAndToAndToRefIn(fromType, fromRefs, toType, toRefs);
    if (!relEntities.isEmpty()) {
      relationshipRepository.deleteAll(relEntities);
    }
  }
  
  /*
   * Removes all relationships
   */
  @Override
  public void removeRelationships(RelationshipType fromType, List<String> fromRefs,
      RelationshipType toType) {
    List<RelationshipEntity> relEntities = relationshipRepository.findByFromAndFromRefInAndTo(fromType, fromRefs, toType);
    if (!relEntities.isEmpty()) {
      relationshipRepository.deleteAll(relEntities);
    }
  }
  
  /*
   * Removes all Team Relationships
   */
  @Override
  public void removeRelationships(RelationshipType toType, String toRef) {
    relationshipRepository.deleteByToAndToRef(toType, toRef);
    relationshipRepository.deleteByFromAndFromRef(toType, toRef);
  }
  
  /*
   * Removes User Team relationship
   */
  @Override
  public void removeUserTeamRelationship(String toRef) {
    String userId = identityService.getCurrentPrincipal();
    List<RelationshipEntity> relEntities = relationshipRepository.findByFromAndFromRefInAndToAndToRefIn(RelationshipType.USER, List.of(userId), RelationshipType.TEAM, List.of(toRef));
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
  public Map<String, String> getMyTeamRefsAndRoles(String userId) {
    List<RelationshipEntity> relationships = 
        this.relationshipRepository.findByFromAndFromRefInAndTypeAndTo(RelationshipType.USER, List.of(userId), RelationshipLabel.MEMBEROF, RelationshipType.TEAM);
    
    return relationships.stream()
            .collect(Collectors.toMap(r -> r.getToRef(), r -> r.getData().get("role") != null ? r.getData().get("role").toString() : RoleEnum.READER.getLabel()));
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
  public List<String> getFilteredFromRefs(Optional<RelationshipType> from, Optional<List<String>> fromRefs, Optional<RelationshipLabel> type, Optional<RelationshipType> to, 
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
  public List<String> getFilteredToRefs(Optional<RelationshipType> from, Optional<List<String>> fromRefs, Optional<RelationshipLabel> type, Optional<RelationshipType> to, 
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
  public List<RelationshipEntity> getFilteredRels(Optional<RelationshipType> from, Optional<List<String>> fromRefs, Optional<RelationshipLabel> label, Optional<RelationshipType> to, 
      Optional<List<String>> toRefs, boolean elevate) {
    
    // Defaults if not provided
    if (label.isEmpty()) {
      label = Optional.of(RelationshipLabel.BELONGSTO);
    } else if (RelationshipLabel.MEMBEROF.equals(label.get())) {
      from = Optional.of(RelationshipType.USER);
    }

    AuthType accessScope = identityService.getCurrentScope();
    LOGGER.debug("RelationshipFilter() - Access Scope: " + identityService.getCurrentScope());
    
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
        if (from.isPresent() && RelationshipType.USER.equals(from.get())) {
          fromRefs = Optional.of(List.of(userId));
        } else if (to.isPresent() && RelationshipType.TEAM.equals(to.get())) {
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
        if (to.isPresent() && RelationshipType.WORKFLOW.equals(to.get())) {
          if (!toRefs.isPresent() || (toRefs.isPresent() && toRefs.get().contains(workflowId))) {
            toRefs = Optional.of(List.of(workflowId));
          }
        } else if (from.isPresent() && RelationshipType.WORKFLOW.equals(from.get())) {
          if (!fromRefs.isPresent() || (fromRefs.isPresent() && fromRefs.get().contains(workflowId))) {
            fromRefs = Optional.of(List.of(workflowId));
          }
        } 
        break;
      case team:
        // Add refs based on Tokens Team
        String teamId = identityService.getCurrentPrincipal();
        if (to.isPresent() && RelationshipType.TEAM.equals(to.get())) {
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
      Criteria criteria = Criteria.where("type").is(label.get());
      criteriaList.add(criteria);
    } else {
      //Default to 'Belongs To' primary relationship type
      Criteria criteria = Criteria.where("type").is(RelationshipLabel.BELONGSTO);
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
  //TODO replace a number of the getFilteredRefs with this
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

  private List<String> getTeamsRefsByUsers(final List<String> userId) {
    List<RelationshipEntity> relationships = 
        this.relationshipRepository.findByFromAndFromRefInAndTypeAndTo(RelationshipType.USER, userId, RelationshipLabel.MEMBEROF, RelationshipType.TEAM);
    return relationships.stream().map(RelationshipEntity::getToRef).collect(Collectors.toList());
  }
  
  ///////////////////////////////////////2nd Gen
  /*
   * Has Relationship
   * 
   * Checks that the relationship exists. Can be simple true / false to determine the user has access
   */
  public boolean hasRelationship(RelationshipType type, String ref) {
    return true;
  }
  
  public List<String> getWorkflows() {
    return new LinkedList<>();
  }
  
  @Override
  public void updateTeamRef(String oldRef, String newRef) {
    this.relationshipRepository.findAndSetToRefByToAndToRef(RelationshipType.TEAM, oldRef, newRef);
  }
}
