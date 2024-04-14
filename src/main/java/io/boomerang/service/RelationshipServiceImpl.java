package io.boomerang.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import io.boomerang.config.MongoConfiguration;
import io.boomerang.data.entity.RelationshipConnectionEntity;
import io.boomerang.data.entity.RelationshipEntity;
import io.boomerang.data.entity.RelationshipEntityGraph;
import io.boomerang.data.repository.RelationshipRepository;
import io.boomerang.model.enums.RelationshipLabel;
import io.boomerang.model.enums.RelationshipType;
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
  
  @Autowired
  private MongoConfiguration mongoConfiguration;
  
  private Map<String, String> teamSlugToRelationshipId = new HashMap<>();
  
//  private Map<String, String> teamSlugToRef = new HashMap<>();
  
//  /*
//   * Creates a new RelationshipEntity for the provided inputs coupled with the current scope
//   * 
//   * @return RelationshipEntity
//   */
//  @Override
//  public RelationshipEntity addRelationshipRefForCurrentScope(RelationshipNodeType fromType, String fromRef) {
//    RelationshipLabel relationship = RelationshipLabel.BELONGSTO;
//    RelationshipNodeType toType = null;
//    String toRef = null;
//
//    LOGGER.info("Current Access Scope: " + identityService.getCurrentScope());
//    switch (identityService.getCurrentScope()) {
//      case session:
//      case user:
//        toType = RelationshipNodeType.USER;
//        toRef = identityService.getCurrentPrincipal();
//        break;
//      case workflow:
//        toType = RelationshipNodeType.WORKFLOW;
//        toRef = identityService.getCurrentPrincipal();
//        break;        
//      case team:
//        toType = RelationshipNodeType.TEAM;
//        toRef = identityService.getCurrentPrincipal();
//        if (RelationshipNodeType.USER.equals(fromType)) {
//          relationship = RelationshipLabel.MEMBEROF;
//        }
//        break;
//      case global:
//        toType = RelationshipNodeType.GLOBAL;
//        break;
//      default:
//        break;
//    }
//    
//    return this.addRelationshipRef(fromType, fromRef, relationship, toType, Optional.of(toRef), Optional.empty());
//  }
  
  /*
   * Creates a new RelationshipEntity for the provided inputs. Requires RelationshipType
   * 
   * @return RelationshipEntity
   */
//  @Override
//  public RelationshipEntity addRelationshipRef(RelationshipType fromType, String fromRef, RelationshipLabel relationship, RelationshipType toType, Optional<String> toRef, Optional<Map<String, Object>> data) {   
//    RelationshipEntity relEntity = new RelationshipEntity();
//    relEntity.setFrom(fromType);
//    relEntity.setFromRef(fromRef);
//    relEntity.setLabel(relationship);
//    relEntity.setTo(toType);
//    if (toRef.isPresent()) {
//      relEntity.setToRef(toRef.get());
//    }
//    if (data.isPresent()) {
//      relEntity.setData(data.get());
//    }
//    LOGGER.info("Relationship: " + relEntity.toString());
//    return relationshipRepository.save(relEntity);
//  }
//  
//  /*
//   * Patch a RelationshipEntity
//   * 
//   * @return RelationshipEntity
//   */
//  @Override
//  public RelationshipEntity patchRelationshipData(RelationshipType fromType, String fromRef, RelationshipLabel relationship, Map<String, Object> data) {
//    Optional<RelationshipEntity> entity = getRelationship(fromType, fromRef, relationship);
//    if (entity.isPresent()) {
//        entity.get().getData().putAll(data);
//      LOGGER.info("Relationship: " + entity.get().toString());
//      return relationshipRepository.save(entity.get());
//    } 
//    throw new BoomerangException(BoomerangError.UNABLE_PATCH_REL);
//  }
////  
//  /*
//   * Retrieve RelationshipEntity for the provided inputs
//   *
//   * 
//   * @return RelationshipEntity
//   */
//  @Override
//  public Optional<RelationshipEntity> getRelationship(RelationshipType fromType, String fromRef, RelationshipLabel relationship) {
//    return relationshipRepository.findByFromAndFromRefAndType(fromType, fromRef, relationship);
//  }
//  
//  /*
//   * Retrieve RelationshipEntityRef for the provided inputs
//   *
//   * 
//   * @return RelationshipEntity
//   */
//  @Override
//  public Optional<String> getRelationshipRef(RelationshipType fromType, String fromRef, RelationshipLabel relationship) {
//    Optional<RelationshipEntity> rel = getRelationship(fromType, fromRef, relationship);
//    if (rel.isPresent()) {
//      return Optional.of(rel.get().getToRef());
//    }
//    return Optional.empty();
//  }
//  
//  /*
//   * Removes a relationship by ID
//   */
//  @Override
//  public void removeRelationshipById(String id) {
//      relationshipRepository.deleteById(id);
//  }
//  
//  /*
//   * Removes all relationships
//   */
//  @Override
//  public void removeRelationships(RelationshipType fromType, List<String> fromRefs,
//      RelationshipType toType, List<String> toRefs) {
//    List<RelationshipEntity> relEntities = relationshipRepository.findByFromAndFromRefInAndToAndToRefIn(fromType, fromRefs, toType, toRefs);
//    if (!relEntities.isEmpty()) {
//      relationshipRepository.deleteAll(relEntities);
//    }
//  }
//  
//  /*
//   * Removes all relationships
//   */
//  @Override
//  public void removeRelationships(RelationshipType fromType, List<String> fromRefs,
//      RelationshipType toType) {
//    List<RelationshipEntity> relEntities = relationshipRepository.findByFromAndFromRefInAndTo(fromType, fromRefs, toType);
//    if (!relEntities.isEmpty()) {
//      relationshipRepository.deleteAll(relEntities);
//    }
//  }
//  
//  /*
//   * Removes all Team Relationships
//   */
//  @Override
//  public void removeRelationships(RelationshipType toType, String toRef) {
//    relationshipRepository.deleteByToAndToRef(toType, toRef);
//    relationshipRepository.deleteByFromAndFromRef(toType, toRef);
//  }
//  
//  /*
//   * Removes User Team relationship
//   */
//  @Override
//  public void removeUserTeamRelationship(String toRef) {
//    String userId = identityService.getCurrentPrincipal();
//    List<RelationshipEntity> relEntities = relationshipRepository.findByFromAndFromRefInAndToAndToRefIn(RelationshipType.USER, List.of(userId), RelationshipType.TEAM, List.of(toRef));
//    if (!relEntities.isEmpty()) {
//      relationshipRepository.deleteAll(relEntities);
//    }
//  }
//  
////  /*
////   * Generates the TeamRefs for the current security scope with no elevated permissions.
////   * 
////   * This is used to return the /mine used by the web to load the Teams selection.
////   */
////  @Override
////  public Map<String, String> getMyTeamRefsAndRoles(String userId) {
////    List<RelationshipEntity> relationships = 
////        this.relationshipRepository.findByFromAndFromRefInAndTypeAndTo(RelationshipNodeType.USER, List.of(userId), RelationshipLabel.MEMBEROF, RelationshipNodeType.TEAM);
////    
////    return relationships.stream()
////            .collect(Collectors.toMap(r -> r.getToRef(), r -> r.getData().get("role") != null ? r.getData().get("role").toString() : RoleEnum.READER.getLabel()));
////  }
//  
//  /*
//   * Generates the FromRefs that the current security scope has access to, based on a specific type and optional lists of typeRefs, scopes, and teamIds 
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
//   * @return list of filtered FromRefs
//   */
//  @Override
//  public List<String> getFilteredFromRefs(Optional<RelationshipType> from, Optional<List<String>> fromRefs, Optional<RelationshipLabel> type, Optional<RelationshipType> to, 
//      Optional<List<String>> toRefs) {
//    return getFilteredRels(from, fromRefs, type, to, toRefs, true).stream().map(RelationshipEntity::getFromRef).collect(Collectors.toList());
//  }
//  
//  /*
//   * Generates the ToRefs that the current security scope has access to, based on a specific type and optional lists of typeRefs, scopes, and teamIds 
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
//   * @return list of filtered FromRefs
//   */
//  @Override
//  public List<String> getFilteredToRefs(Optional<RelationshipType> from, Optional<List<String>> fromRefs, Optional<RelationshipLabel> type, Optional<RelationshipType> to, 
//      Optional<List<String>> toRefs) {
//    return getFilteredRels(from, fromRefs, type, to, toRefs, true).stream().map(RelationshipEntity::getToRef).collect(Collectors.toList());
//  }
  
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
//  @Override
//  public List<RelationshipEntity> getFilteredRels(Optional<RelationshipType> from, Optional<List<String>> fromRefs, Optional<RelationshipLabel> label, Optional<RelationshipType> to, 
//      Optional<List<String>> toRefs, boolean elevate) {
//    
//    // Defaults if not provided
//    if (label.isEmpty()) {
//      label = Optional.of(RelationshipLabel.BELONGSTO);
//    } else if (RelationshipLabel.MEMBEROF.equals(label.get())) {
//      from = Optional.of(RelationshipType.USER);
//    }
//
//    AuthType accessScope = identityService.getCurrentScope();
//    LOGGER.debug("RelationshipFilter() - Access Scope: " + identityService.getCurrentScope());
//    
//    // If User is Admin provide global access
//    // MEMBEROF requests are ignored as we only want to return that users Teams and as such don't elevate the scope
//    if (elevate && (AuthType.session.equals(accessScope) || AuthType.user.equals(accessScope)) && identityService.isCurrentUserAdmin()) {
//      LOGGER.info("RelationshipFilter() - Identity is Admin - Elevating permissions.");
//      accessScope = AuthType.global;
//    }
//
//    switch (accessScope) {
//      case session:
//      case user:
//        String userId = identityService.getCurrentPrincipal();
//        if (from.isPresent() && RelationshipType.USER.equals(from.get())) {
//          fromRefs = Optional.of(List.of(userId));
//        } else if (to.isPresent() && RelationshipType.TEAM.equals(to.get())) {
//          List<String> filteredTeams = getTeamsRefsByUsers(List.of(userId));
//         if (toRefs.isPresent()) {
//           // If toRefs are provided (i.e. TeamIds) then filter to ones provided that the user has access to
//           List<String> tempRefs = toRefs.get();
//           toRefs = Optional.of(filteredTeams.stream().filter(r -> tempRefs.contains(r)).collect(Collectors.toList()));
//         } else {
//           toRefs = Optional.of(filteredTeams);
//         }
//        }
//        break;
//      case workflow:
//        // Add refs based on Workflow
//        // Will either set toRef to the WorkflowID or make sure its in the list of provided Refs
//        String workflowId = identityService.getCurrentPrincipal();
//        if (to.isPresent() && RelationshipType.WORKFLOW.equals(to.get())) {
//          if (!toRefs.isPresent() || (toRefs.isPresent() && toRefs.get().contains(workflowId))) {
//            toRefs = Optional.of(List.of(workflowId));
//          }
//        } else if (from.isPresent() && RelationshipType.WORKFLOW.equals(from.get())) {
//          if (!fromRefs.isPresent() || (fromRefs.isPresent() && fromRefs.get().contains(workflowId))) {
//            fromRefs = Optional.of(List.of(workflowId));
//          }
//        } 
//        break;
//      case team:
//        // Add refs based on Tokens Team
//        String teamId = identityService.getCurrentPrincipal();
//        if (to.isPresent() && RelationshipType.TEAM.equals(to.get())) {
//          if (!toRefs.isPresent() || (toRefs.isPresent() && toRefs.get().contains(teamId))) {
//            toRefs = Optional.of(List.of(teamId)); 
//          }
//        } else if (!to.isPresent()) {
//          toRefs = Optional.of(List.of(teamId)); 
//        }
//        break;
//      case global:
//        // Allow anything with no filtering
//        break;
//    }
//    
//    // Create manual query for MongoDB
//    List<Criteria> criteriaList = new ArrayList<>();
//
//    if (from.isPresent()) {
//      Criteria criteria = Criteria.where("from").is(from.get());
//      criteriaList.add(criteria);
//    }
//
//    if (fromRefs.isPresent()) {
//      Criteria criteria = Criteria.where("fromRef").in(fromRefs.get());
//      criteriaList.add(criteria);
//    }
//
//    if (from.isPresent()) {
//      Criteria criteria = Criteria.where("type").is(label.get());
//      criteriaList.add(criteria);
//    } else {
//      //Default to 'Belongs To' primary relationship type
//      Criteria criteria = Criteria.where("type").is(RelationshipLabel.BELONGSTO);
//      criteriaList.add(criteria);
//    }
//
//    if (to.isPresent()) {
//      Criteria criteria = Criteria.where("to").is(to.get());
//      criteriaList.add(criteria);
//    }
//
//    if (toRefs.isPresent()) {
//      Criteria criteria = Criteria.where("toRef").in(toRefs.get());
//      criteriaList.add(criteria);
//    }
//
//    Criteria[] criteriaArray = criteriaList.toArray(new Criteria[criteriaList.size()]);
//    Criteria allCriteria = new Criteria();
//    if (criteriaArray.length > 0) {
//      allCriteria.andOperator(criteriaArray);
//    }
//    Query query = new Query(allCriteria);
//    
//    List<RelationshipEntity> relEntities = mongoTemplate.find(query, RelationshipEntity.class);
//    
//    LOGGER.debug("Relationships Found: " + relEntities.toString());
//    
//    return relEntities;
//  }

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

//  private List<String> getTeamsRefsByUsers(final List<String> userId) {
//    List<RelationshipEntity> relationships = 
//        this.relationshipRepository.findByFromAndFromRefInAndTypeAndTo(RelationshipType.USER, userId, RelationshipLabel.MEMBEROF, RelationshipType.TEAM);
//    return relationships.stream().map(RelationshipEntity::getToRef).collect(Collectors.toList());
//  }
  
  /////////////////////////////////////
  // Relationship V2 methods         //
  // Using EntityV2 or Nodes & Edges //
  /////////////////////////////////////
  /*
   * Checks the in memory cache map or retrieves from DB
   */
  private String getTeamRelationshipId(String slug) {
    if (!teamSlugToRelationshipId.containsKey(slug)) {
      Optional<RelationshipEntity> teamNodeV2 =
          this.relationshipRepository.findFirstByTypeAndRefOrSlug(RelationshipType.TEAM, slug);
      if (teamNodeV2.isPresent()) {
        teamSlugToRelationshipId.put(slug, teamNodeV2.get().getId());
      }
    }
    return teamSlugToRelationshipId.get(slug);
  }
  
  /*
   * Helper method. Checks for relationship to Team by Slug or Ref
   */
  public boolean hasTeamRelationship(Optional<RelationshipType> fromType, Optional<String> fromRef, RelationshipLabel label, String toRef, boolean elevate) {    
      return hasRelationships(fromType, fromRef.isPresent() ? Optional.of(List.of(fromRef.get())) : Optional.empty(), label, RelationshipType.TEAM, toRef, elevate);
  }
  
  public boolean hasTeamRelationships(Optional<RelationshipType> fromType, Optional<List<String>> fromRefs, RelationshipLabel label, String toRef, boolean elevate) {    
    return hasRelationships(fromType, fromRefs, label, RelationshipType.TEAM, toRef, elevate);
  }
  
  public boolean hasRootRelationship(RelationshipType type, String ref) {
    List<String> rootRefs = getRootRefs(type, Optional.of(List.of(ref)));
    if (rootRefs.size() > 0) {
      return true;
    }
    return false;
  }
  
  public boolean hasRootRelationships(RelationshipType type, List<String> refs) {   
    List<String> rootRefs = getRootRefs(type, Optional.of(refs));
    if (rootRefs.size() > 0) {
      return true;
    }
    return false;
  }
  
  private boolean hasRelationships(Optional<RelationshipType> fromType, Optional<List<String>> fromRefs, RelationshipLabel label, RelationshipType toType, String toRef, boolean elevate) {
    List<String> refs = filter(fromType, fromRefs, label, toType, toRef, elevate);
    if (refs.size() > 0) {
      return true;
    }
    return false;
  }
  
  public List<String> getFilteredRefs(Optional<RelationshipType> fromType, Optional<List<String>> fromRefs, RelationshipLabel label, RelationshipType toType, String toRef, boolean elevate) {
    return filter(fromType, fromRefs, label, toType, toRef, elevate);
  }
  
  public List<String> getRootRefs(RelationshipType type, Optional<List<String>> slugs) {
    // Root nodes have no parent - thus no need to use a Graph. Query for nodes.
    List<Criteria> criteriaList = new ArrayList<>();
    Criteria criteria = Criteria.where("type").is(type);
    criteriaList.add(criteria);
    if (slugs.isPresent()) {
      criteria = Criteria.where("slug").in(slugs.get());
      criteriaList.add(criteria);
    }
    Criteria[] criteriaArray = criteriaList.toArray(new Criteria[criteriaList.size()]);
    Criteria allCriteria = new Criteria();
    if (criteriaArray.length > 0) {
      allCriteria.andOperator(criteriaArray);
    }
    Query query = new Query(allCriteria);
    
    List<RelationshipEntity> relEntities = mongoTemplate.find(query, RelationshipEntity.class);
    return relEntities.stream().map(e -> e.getRef()).toList();
  }
  
  /*
   * This method understands the connection direction. Check the relationship arch diagram to understand how this works.
   */
  private List<String> filter(Optional<RelationshipType> fromType, Optional<List<String>> fromRefs, RelationshipLabel label, RelationshipType toType, String toRef, boolean elevate) {
    List<String> filteredRefs = new ArrayList<>();
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
        // Set up default type for not specified
        if (fromType.isEmpty()) {
          fromType = Optional.of(RelationshipType.USER);
        }
        // Enforce single UserId of self
        if (fromType.isPresent() && RelationshipType.USER.equals(fromType.get())) {
          if ((fromRefs.isPresent() && fromRefs.get().contains(userId)) || fromRefs.isEmpty()) {
            LOGGER.debug("FromType and FromRef is correct");
            fromRefs = Optional.of(List.of(userId));
          } else {
            fromRefs = Optional.empty();
          }
        } else if (RelationshipType.USER.equals(toType)) {
          if (!toRef.equals(userId)) {
            return filteredRefs;
          }
        }
        // If relationship of Object (not user, such as Workflow) to Team, then check user has access to Team
//        if (fromType.isPresent() && !RelationshipNodeType.USER.equals(fromType.get())) {
//          RelationshipEntityV2Aggregate aggregate = this.relationshipRepositoryV2.findUserTeamRelationships(userId,  mongoConfiguration.fullCollectionName("relationships"));
//          if (!aggregate.getRoles().isEmpty()) {
//            aggregate.getRoles().forEach(r -> {
//              memberRoleMap.put(r.getSlug(), r.getData().containsKey("role") ? r.getData().get("role") : RoleEnum.READER.getLabel());
//            });
//          }
//        }
        // TODO - validate users relationship to Team
        break;
      case workflow:
        // Validate refs for the Workflow principal
        String workflowId = identityService.getCurrentPrincipal();
        // Set up default type for not specified
        if (fromType.isEmpty()) {
          fromType = Optional.of(RelationshipType.WORKFLOW);
        }
        if (fromType.isPresent() && RelationshipType.WORKFLOW.equals(fromType.get())) {
          if ((fromRefs.isPresent() && fromRefs.get().contains(workflowId)) || fromRefs.isEmpty()) {
            fromRefs = Optional.of(List.of(workflowId));
          } else {
            return filteredRefs;
          }
        } else if (RelationshipType.WORKFLOW.equals(toType)) {
          if (!toRef.equals(workflowId)) {
            return filteredRefs;
          }
        }
        break;
      case team:
        // Validate refs based on Team principals
        // Note: team is currently never the from, everything goes to a team
        String teamId = identityService.getCurrentPrincipal();
        if (RelationshipType.TEAM.equals(toType)) {
          if (!toRef.equals(teamId)) {
            return filteredRefs;
          }
        }
        break;
      case global:
        // Allow anything with no filtering
        break;
    }
    
    //Query the database as a Graph for relationship
    try {
      RelationshipEntityGraph aggregate;
      if (fromRefs.isPresent()) {
        aggregate = this.relationshipRepository.graphRelationshipsByTypeToAndIn(toType, toRef, mongoConfiguration.fullCollectionName("relationships"), fromType.get(), fromRefs.get());
      } else {
        aggregate = this.relationshipRepository.graphRelationshipsByTypeTo(toType, toRef, mongoConfiguration.fullCollectionName("relationships"), fromType.get());
      }
      filteredRefs = aggregate.getChildren().stream().map(c -> c.getRef()).toList();
    } catch (Exception ex) {
      LOGGER.error(ex);
    }
    LOGGER.debug("Filtered Refs: {}", filteredRefs);
    return filteredRefs;
  }
  
  /*
   * This method understands the connection direction. Check the relationship arch diagram to understand how this works.
   */
  public List<String> getFilteredRootRefs(RelationshipType rootType, List<String> rootRefs, boolean elevate) {
    List<String> filteredRefs = new ArrayList<>();
    RelationshipType fromType = null;
    String fromRef = null;
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
        fromType = RelationshipType.USER;
        fromRef = identityService.getCurrentPrincipal();
        break;
      case workflow:
        fromType = RelationshipType.WORKFLOW;
        fromRef = identityService.getCurrentPrincipal();
        break;
      case team:
        fromType = RelationshipType.TEAM;
        fromRef = identityService.getCurrentPrincipal();
        if (rootType.equals(RelationshipType.TEAM) && (rootRefs.contains(fromRef) || rootRefs.isEmpty())) {
          filteredRefs = List.of(fromRef);
        }
        break;
      case global:
        filteredRefs = getRootRefs(rootType, Optional.of(rootRefs));
        return filteredRefs;
    }
    
    //Query the database as a Graph for relationship
    try {
      RelationshipEntityGraph aggregate = this.relationshipRepository.graphRelationshipsByTypeFrom(fromType, fromRef, mongoConfiguration.fullCollectionName("relationships"), rootType);
      if (!rootRefs.isEmpty()) {
        filteredRefs = aggregate.getChildren().stream().filter(c -> rootRefs.contains(c.getSlug())).map(c -> c.getRef()).toList();
      } else {
        filteredRefs = aggregate.getChildren().stream().map(c -> c.getRef()).toList();
      }
    } catch (Exception ex) {
      LOGGER.error(ex);
    }
    LOGGER.debug("Filtered Refs: {}", filteredRefs);
    return filteredRefs;
  }
  
  /*
   * Helper method. Should only be used after checking if relationship exists. 
   */
  public String getRefFromSlug(RelationshipType type, String refOrSlug) {
    Optional<RelationshipEntity> node = this.relationshipRepository.findFirstByTypeAndRefOrSlug(type, refOrSlug);
    if (node.isPresent()) {
      return node.get().getRef();
    }
    return "";
  }
  
  /*
   * Helper method. Should only be used after checking if relationship exists. 
   */
  public String getSlugFromRef(RelationshipType type, String refOrSlug) {
    Optional<RelationshipEntity> node = this.relationshipRepository.findFirstByTypeAndRefOrSlug(type, refOrSlug);
    if (node.isPresent()) {
      return node.get().getSlug();
    }
    return "";
  }
  
  public void createNodeWithTeamConnection(RelationshipType type, String ref, String slug, String to, Optional<Map<String, String>> data) {
    Optional<RelationshipEntity> toNode = this.relationshipRepository.findFirstByTypeAndRefOrSlug(RelationshipType.TEAM, to);
    this.relationshipRepository.insert(new RelationshipEntity(type, ref, slug, Optional.empty(), new RelationshipConnectionEntity(RelationshipLabel.BELONGSTO, toNode.get().getId(), data)));
  }
  
  public void createNode(RelationshipType type, String ref, String slug, Optional<Map<String, String>> data) {
      this.relationshipRepository.insert(new RelationshipEntity(type, ref, slug, data));
  }
  
  public void upsertTeamConnection(RelationshipType fromType, String fromRef, RelationshipLabel label, String slug, Optional<Map<String, String>> data) {
//      this.relationshipRepositoryV2.findAndUpdateConnectionByTypeAndRefOrSlugAndConnectionsLabelAndConnectionsTo(fromType, fromRef, label, getTeamRelationshipId(slug), "reader");
//      this.relationshipRepositoryV2.existsByTypeAndRefOrSlug(fromType, fromRef, slug)
    Optional<RelationshipEntity> teamNode = this.relationshipRepository.findFirstByTypeAndRefOrSlug(RelationshipType.TEAM, slug);
    if (teamNode.isPresent()) {
      long update = this.relationshipRepository.updateConnectionByTypeAndRefOrSlug(fromType, fromRef, new ObjectId(teamNode.get().getId()), data.isPresent() ? data.get() : new HashMap<String, String>());
      LOGGER.debug("Updates made: {}", update);
      if (update == 0) {
        long push = this.relationshipRepository.pushConnectionByTypeAndRefOrSlug(fromType, fromRef, new RelationshipConnectionEntity(label, teamNode.get().getId(), data));
        LOGGER.debug("Push made: {}", push);
      }
    }
  }
  
  public void removeTeamConnection(RelationshipType type, List<String> refs, String team) {
    Optional<RelationshipEntity> teamNode = this.relationshipRepository.findFirstByTypeAndRefOrSlug(RelationshipType.TEAM, team);
    if (teamNode.isPresent()) {
      if (RelationshipType.USER.equals(type) & refs.isEmpty()) {
        String userId = identityService.getCurrentPrincipal();
        refs = List.of(userId);
      }
      this.relationshipRepository.removeConnectionByTo(type, refs, new ObjectId(teamNode.get().getId()));
    }
  }
  
  public void removeAllTeamConnections(String slug) {
    Optional<RelationshipEntity> teamNode = this.relationshipRepository.findFirstByTypeAndRefOrSlug(RelationshipType.TEAM, slug);
    if (teamNode.isPresent()) {
      this.relationshipRepository.removeAllConnectionsByTo(new ObjectId(teamNode.get().getId()));
    }
  }
  
  public Map<String, String> getMembersAndRoleForTeam(String slug) {
    Map<String, String> memberRoleMap = new HashMap<String, String>();
      RelationshipEntityGraph graph = this.relationshipRepository.graphRelationshipsByTypeTo(RelationshipType.TEAM, slug, mongoConfiguration.fullCollectionName("relationships"), RelationshipType.USER);
      LOGGER.debug(graph.toString());
  //    String teamNodeId = getTeamRelationshipIdFromSlug(slug);
  //    List<RelationshipEntityV2> memberNodes = this.relationshipRepositoryV2.findAllByConnectionsLabelAndConnectionsTo(RelationshipLabel.MEMBEROF, teamNodeId);      
    if (!graph.getChildren().isEmpty()) {
      graph.getChildren().forEach(n -> { 
          Optional<RelationshipConnectionEntity> connection = n.getConnections().stream().filter(c -> c.getTo().equals(graph.getId())).findFirst();
          String role = RoleEnum.READER.getLabel();
          if (connection.isPresent() && connection.get().getData().containsKey("role")) {
            role = connection.get().getData().get("role");
          }
          memberRoleMap.put(n.getRef(), role);
        });
    }
    return memberRoleMap;
  }
  
  // Team Node
  public void updateNodeSlug(RelationshipType type, String currentSlug, String newSlug) {
    RelationshipEntity entity = this.relationshipRepository.findAndSetSlugByTypeAndSlug(type, currentSlug, newSlug);
    teamSlugToRelationshipId.remove(currentSlug);
    teamSlugToRelationshipId.put(newSlug, entity.getId());
//      teamSlugToRef.remove(currentSlug);
//      teamSlugToRef.put(newSlug, entity.getRef());
  }
  
  public void removeNodeByRefOrSlug(RelationshipType type, String slug) {
    this.relationshipRepository.deleteByTypeAndRefOrSlug(type, slug);
  }
  
  /*
   * This is used for top level nodes such as Team, GlobalTasks, etc
   */
  public boolean doesSlugOrRefExistForType(RelationshipType type, String slug) {
    return this.relationshipRepository.existsByTypeAndRefOrSlug(type, slug);
  }
  
  /*
   * This is used to find a slug for all Nodes of a Type within a particular team
   */
  public boolean doesSlugOrRefExistForTypeInTeam(RelationshipType type, String slug, String team) {
    RelationshipEntityGraph graph = this.relationshipRepository.graphRelationshipsByTypeTo(RelationshipType.TEAM, team, mongoConfiguration.fullCollectionName("relationships"), type);
    if (!graph.getChildren().isEmpty()) {
      return graph.getChildren().stream().anyMatch(c -> c.getSlug().equals(slug) || c.getRef().equals(slug));
    }
    return false;
  }
  
  /* 
   * Helper method used for Schedules to get their Team
   */
  public String getTeamSlugFromChild(RelationshipType type, String ref) {
    RelationshipEntityGraph graph = this.relationshipRepository.graphRelationshipsByTypeFrom(type, ref, mongoConfiguration.fullCollectionName("relationships"), RelationshipType.TEAM);
    return !graph.getChildren().isEmpty() && graph.getChildren().get(0).getSlug() != null ? graph.getChildren().get(0).getSlug() : "";
  }
  
  public Map<String, String> getMyTeamRefsAndRoles(String userId) {
    Map<String, String> memberRoleMap = new HashMap<String, String>();
    RelationshipEntityGraph graph = this.relationshipRepository.findUserTeamRelationships(userId,  mongoConfiguration.fullCollectionName("relationships"));
    LOGGER.debug(graph.toString());
    if (!graph.getTeams().isEmpty()) {
      graph.getTeams().forEach(r -> {
        memberRoleMap.put(r.getRef(), r.getData().containsKey("role") ? r.getData().get("role") : RoleEnum.READER.getLabel());
      });
    }
    LOGGER.debug("Roles: {}", memberRoleMap.toString());
    return memberRoleMap;
  }
  
  public Map<String, String> getMyTeamSlugsAndRoles(String userId) {
    Map<String, String> memberRoleMap = new HashMap<String, String>();
    RelationshipEntityGraph aggregate = this.relationshipRepository.findUserTeamRelationships(userId,  mongoConfiguration.fullCollectionName("relationships"));
    LOGGER.debug(aggregate.toString());
    if (!aggregate.getTeams().isEmpty()) {
      aggregate.getTeams().forEach(r -> {
        memberRoleMap.put(r.getSlug(), r.getData().containsKey("role") ? r.getData().get("role") : RoleEnum.READER.getLabel());
      });
    }
    LOGGER.debug("Roles: {}", memberRoleMap.toString());
    return memberRoleMap;
  }
}
