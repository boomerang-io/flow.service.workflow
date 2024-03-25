package io.boomerang.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
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
import io.boomerang.data.entity.RelationshipEdgeEntity;
import io.boomerang.data.entity.RelationshipEntity;
import io.boomerang.data.entity.RelationshipEntityV2;
import io.boomerang.data.entity.RelationshipEntityV2Graph;
import io.boomerang.data.entity.RelationshipNodeEntity;
import io.boomerang.data.entity.RelationshipNodeEntityAggregate;
import io.boomerang.data.repository.RelationshipEdgeRepository;
import io.boomerang.data.repository.RelationshipNodeRepository;
import io.boomerang.data.repository.RelationshipRepository;
import io.boomerang.data.repository.RelationshipRepositoryV2;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
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
  private RelationshipNodeRepository relationshipNodeRepository;

  @Autowired
  private RelationshipEdgeRepository relationshipEdgeRepository;
  
  @Autowired
  private RelationshipRepositoryV2 relationshipRepositoryV2;
  
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
  
//  /*
//   * Generates the TeamRefs for the current security scope with no elevated permissions.
//   * 
//   * This is used to return the /mine used by the web to load the Teams selection.
//   */
//  @Override
//  public Map<String, String> getMyTeamRefsAndRoles(String userId) {
//    List<RelationshipEntity> relationships = 
//        this.relationshipRepository.findByFromAndFromRefInAndTypeAndTo(RelationshipNodeType.USER, List.of(userId), RelationshipLabel.MEMBEROF, RelationshipNodeType.TEAM);
//    
//    return relationships.stream()
//            .collect(Collectors.toMap(r -> r.getToRef(), r -> r.getData().get("role") != null ? r.getData().get("role").toString() : RoleEnum.READER.getLabel()));
//  }
  
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
  
//  ////////////////////////////
//  // 2nd generation methods //
//  ////////////////////////////
//  /*
//   * Has Team Relationship By Slug 
//   * 
//   * Checks that the relationship exists using a Team Slug.
//   */
//  public boolean hasTeamRelationshipBySlug(Optional<RelationshipNodeType> fromType, Optional<String> fromRef, RelationshipLabel label, String slug, boolean elevate) {    
//      return hasRelationships(fromType, fromRef.isPresent() ? Optional.of(List.of(fromRef.get())) : Optional.empty(), label, RelationshipNodeType.TEAM, getTeamRefFromSlug(slug), elevate);
//  }
//
//  /*
//   * Checks the in memory cache map or retrieves from DB
//   */
//  private String getTeamRelationshipIdFromSlug(String slug) {
//    if (!teamSlugToRelationshipId.containsKey(slug)) {
//      Optional<RelationshipNodeEntity> teamNode = this.relationshipNodeRepository.findFirstByTypeAndSlug(RelationshipNodeType.TEAM, slug);
//      if (teamNode.isPresent()) {
//        teamSlugToRelationshipId.put(slug, teamNode.get().getId());
//      }
//    }
//    return teamSlugToRelationshipId.get(slug);
//  }
//
//  /*
//   * Checks the in memory cache map or retrieves from DB
//   */
//  private String getTeamRefFromSlug(String slug) {
//    if (!teamSlugToRef.containsKey(slug)) {
//      Optional<RelationshipNodeEntity> teamNode = this.relationshipNodeRepository.findFirstByTypeAndSlug(RelationshipNodeType.TEAM, slug);
//      if (teamNode.isPresent()) {
//        teamSlugToRef.put(slug, teamNode.get().getRef());
//      }
//    }
//    return teamSlugToRef.get(slug);
//  }
//  
//  /*
//   * This method understands the connection direction. Check the relationship arch diagram to understand how this works.
//   */
//  private boolean hasRelationships(Optional<RelationshipNodeType> fromType, Optional<List<String>> fromRefs, RelationshipLabel label, RelationshipNodeType toType, String toRef, boolean elevate) {
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
//        // Set up default type for not specified
//        if (fromType.isEmpty()) {
//          fromType = Optional.of(RelationshipNodeType.USER);
//        }
//        if (fromType.isPresent() && RelationshipNodeType.USER.equals(fromType.get())) {
//          if ((fromRefs.isPresent() && fromRefs.get().contains(userId)) || fromRefs.isEmpty()) {
//            LOGGER.debug("FromType and FromRef is correct");
//            fromRefs = Optional.of(List.of(userId));
//          } else {
//            return false;
//          }
//        }
//        if (RelationshipNodeType.TEAM.equals(toType)) {
//          // Do a check of if current user has a relationship to the team.
//          Optional<RelationshipNodeEntity> userNode = this.relationshipNodeRepository.findFirstByTypeAndRef(RelationshipNodeType.USER, userId);
//          Optional<RelationshipNodeEntity> teamNode = this.relationshipNodeRepository.findFirstByTypeAndRef(RelationshipNodeType.TEAM, toRef);
//          if (userNode.isPresent() && teamNode.isPresent()) {
//           Integer relationshipCount = this.relationshipEdgeRepository.countByFromAndLabelAndTo(userNode.get().getId(), RelationshipLabel.MEMBEROF, teamNode.get().getId());
//           LOGGER.debug("Found user node: {}", relationshipCount);
//           LOGGER.debug("Team toRef: {}", toRef);
//           if (relationshipCount <= 0) {
//             return false;
//           }
//          } else {
//            return false;
//          }
//        }
//        // If from is USER and to is TEAM then return true as we have already done all the checks
//        if (fromType.isPresent() && RelationshipNodeType.USER.equals(fromType.get()) && RelationshipNodeType.TEAM.equals(toType)) {
//          return true;
//        }
//        break;
//      case workflow:
//        // Validate refs for the Workflow principal
//        String workflowId = identityService.getCurrentPrincipal();
//        // Set up default type for not specified
//        if (fromType.isEmpty()) {
//          fromType = Optional.of(RelationshipNodeType.WORKFLOW);
//        }
//        if (fromType.isPresent() && RelationshipNodeType.WORKFLOW.equals(fromType.get())) {
//          if ((fromRefs.isPresent() && fromRefs.get().contains(workflowId)) || fromRefs.isEmpty()) {
//            fromRefs = Optional.of(List.of(workflowId));
//          } else {
//            return false;
//          }
//        } else if (RelationshipNodeType.WORKFLOW.equals(toType)) {
//          if (!toRef.equals(workflowId)) {
//            return false;
//          }
//        }
//        break;
//      case team:
//        // Validate refs based on Team principals
//        // Note: team is currently never the from, everything goes to a team
//        String teamId = identityService.getCurrentPrincipal();
//        if (RelationshipNodeType.TEAM.equals(toType)) {
//          if (!toRef.equals(teamId)) {
//            return false;
//          }
//        }
//        break;
//      case global:
//        // Allow anything with no filtering
//        break;
//    }
//    
//    //Query the database for relationship
//    // Find the two Nodes
//    try {
//      List<RelationshipNodeEntity> fromNodes = this.relationshipNodeRepository.findAllByTypeAndRefIn(fromType.get(), fromRefs.get());
//      List<String> fromNodeIds = fromNodes.stream().map(RelationshipNodeEntity::getId).collect(Collectors.toList());
//      //TODO determine if we using Slugs or Ids in the toRef
//      Optional<RelationshipNodeEntity> toNode = this.relationshipNodeRepository.findFirstByTypeAndRef(toType, toRef);
//      //Check for any relationships between the two Nodes
//      if (!fromNodeIds.isEmpty() && toNode.isPresent() && this.relationshipEdgeRepository.countByFromInAndTo(fromNodeIds, toNode.get().getId()) > 0) {
//        return true;
//      }
//    } catch (Exception ex) {
//      LOGGER.error(ex);
//      return false;
//    }
//    return false;
//  }
//  
//  public RelationshipNodeEntity createTeamNode(String ref, String slug) {
//    RelationshipNodeEntity teamNode = createNode(RelationshipNodeType.TEAM, ref, slug);
//    teamSlugToRelationshipId.put(slug, teamNode.getId());
//    teamSlugToRef.put(slug, ref);
//    return teamNode;
//  }
//  
//  public RelationshipNodeEntity createUserNode(String ref, String slug) {
//    return createNode(RelationshipNodeType.USER, ref, slug);
//  }
//  
//  private RelationshipNodeEntity createNode(RelationshipNodeType type, String ref, String slug) {
//    return this.relationshipNodeRepository.insert(new RelationshipNodeEntity(type, ref, slug, Optional.empty()));
//  }
//  
//  public void upsertTeamConnectionBySlug(RelationshipNodeType fromType, String fromRef, RelationshipLabel label, String slug, Optional<Map<String, String>> data) {
//    Optional<RelationshipNodeEntity> fromNode = this.relationshipNodeRepository.findFirstByTypeAndRef(fromType, fromRef);
//    if (fromNode.isPresent()) {
//      Optional<RelationshipEdgeEntity> connection = this.relationshipEdgeRepository.findFirstByFromAndLabelAndTo(fromNode.get().getId(), label, getTeamRelationshipIdFromSlug(slug));
//      if (connection.isPresent()) {
//        connection.get().getData().putAll(data.get());
//        this.relationshipEdgeRepository.save(connection.get());
//      } else {
//        createEdge(fromNode.get().getId(), label, getTeamRelationshipIdFromSlug(slug), data);
//      }
//    }
//  }
//  
//  public Map<String, String> getMembersAndRoleForTeamBySlug(String slug) {
//    List<RelationshipEdgeEntity> connections = this.relationshipEdgeRepository.findAllByLabelAndTo(RelationshipLabel.MEMBEROF, getTeamRelationshipIdFromSlug(slug));
//    if (!connections.isEmpty()) {
//      Map<String, String> memberRoleMap = new HashMap<String, String>();
//      connections.forEach(c -> {
//        Optional<RelationshipNodeEntity> member = this.relationshipNodeRepository.findFirstByTypeAndRef(RelationshipNodeType.USER, c.getFrom());
//        if (member.isPresent()) {          
//          memberRoleMap.put(member.get().getRef(), c.getData().containsKey("role") ? c.getData().get("role") : "");
//        }
//      });
//      return memberRoleMap;
//    }
//    return new HashMap<>();
//  }
//  
//  private RelationshipEdgeEntity createEdge(String from, RelationshipLabel label, String to, Optional<Map<String, String>> data) {
//    //TODO check that the relationship doesn't already exist
//    return this.relationshipEdgeRepository.insert(new RelationshipEdgeEntity(from, label, to, data));
//  }
//  
//  // Team Node
//  public void updateTeamSlug(String currentSlug, String newSlug) {
//    RelationshipNodeEntity entity = this.relationshipNodeRepository.findAndSetSlugByTypeAndSlug(RelationshipNodeType.TEAM, currentSlug, newSlug);
//    teamSlugToRelationshipId.remove(currentSlug);
//    teamSlugToRelationshipId.put(newSlug, entity.getId());
//    teamSlugToRef.remove(currentSlug);
//    teamSlugToRef.put(newSlug, entity.getRef());
//  }
//  
//  public void removeRelationshipsBySlug(RelationshipNodeType type, String slug) {
//    this.relationshipNodeRepository
//  }
//  
//  public void removeRelationshipsByRef(RelationshipNodeType type, String slug) {
//    
//  }
  
  /////////////////////////////////////
  // Relationship V2 methods         //
  // Using EntityV2 or Nodes & Edges //
  /////////////////////////////////////
  private boolean usev2 = true;
  
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
    
    List<RelationshipEntityV2> relEntities = mongoTemplate.find(query, RelationshipEntityV2.class);
    return relEntities.stream().map(e -> e.getRef()).toList();
  }

  /*
   * Checks the in memory cache map or retrieves from DB
   */
  private String getTeamRelationshipId(String slug) {
      if (!teamSlugToRelationshipId.containsKey(slug)) {
        if (usev2) {
          Optional<RelationshipEntityV2> teamNodeV2 = this.relationshipRepositoryV2.findFirstByTypeAndRefOrSlug(RelationshipType.TEAM, slug);
          if (teamNodeV2.isPresent()) {
            teamSlugToRelationshipId.put(slug, teamNodeV2.get().getId());
          }
        } else {
          Optional<RelationshipNodeEntity> teamNode = this.relationshipNodeRepository.findFirstByTypeAndRefOrSlug(RelationshipType.TEAM, slug);
          if (teamNode.isPresent()) {
            teamSlugToRelationshipId.put(slug, teamNode.get().getId());
          }
        }
      }
      return teamSlugToRelationshipId.get(slug);
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
            return filteredRefs;
          }
        } else if (RelationshipType.USER.equals(toType)) {
          if (!toRef.equals(userId)) {
            return filteredRefs;
          }
        }
        // If relationship of Object (not user, such as Workflow) to Team, then check user has access to Team
//        if (fromType.isPresent() && !RelationshipNodeType.USER.equals(fromType.get())) {
//          RelationshipEntityV2Aggregate aggregate = this.relationshipRepositoryV2.findUserTeamRelationships(userId,  mongoConfiguration.fullCollectionName("relationships_v2"));
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
    if (usev2) {
      try {
        RelationshipEntityV2Graph aggregate = this.relationshipRepositoryV2.graphRelationshipsByTypeTo(toType, toRef, mongoConfiguration.fullCollectionName("relationships_v2"), fromType.get());
        LOGGER.debug(aggregate.toString());
        if (!aggregate.getChildren().isEmpty()) {
          if (fromRefs.isPresent()) {
            final List<String> finalFromRefs = fromRefs.get(); 
            filteredRefs = aggregate.getChildren().stream().filter(c -> finalFromRefs.contains(c.getRef())).map(c -> c.getRef()).toList();
          } else {
            filteredRefs = aggregate.getChildren().stream().map(c -> c.getRef()).toList();
          }
        }
      } catch (Exception ex) {
        LOGGER.error(ex);
      }
    } else {
      try {
        RelationshipNodeEntityAggregate aggregate = this.relationshipNodeRepository.findRelationshipsByGraphTo(toType, toRef, mongoConfiguration.fullCollectionName("relationship_edges"), label, mongoConfiguration.fullCollectionName("relationship_nodes"));
        LOGGER.debug(aggregate.toString());
        if (!aggregate.getChildren().isEmpty()) {
          final List<String> finalFromRefs = fromRefs.get(); 
          filteredRefs = aggregate.getChildren().stream().filter(c -> finalFromRefs.contains(c.getRef())).map(c -> c.getRef()).toList();
        }
      } catch (Exception ex) {
        LOGGER.error(ex);
      }
    }
    LOGGER.debug("Filtered Refs: {}", filteredRefs);
    return filteredRefs;
  }
  
  /*
   * Helper method. Should only be used after checking if relationship exists. 
   */
  public String getRefFromSlug(RelationshipType type, String refOrSlug) {
    Optional<RelationshipEntityV2> node = this.relationshipRepositoryV2.findFirstByTypeAndRefOrSlug(type, refOrSlug);
    if (node.isPresent()) {
      return node.get().getRef();
    }
    return "";
  }
  
  /*
   * Helper method. Should only be used after checking if relationship exists. 
   */
  public String getSlugFromRef(RelationshipType type, String refOrSlug) {
    Optional<RelationshipEntityV2> node = this.relationshipRepositoryV2.findFirstByTypeAndRefOrSlug(type, refOrSlug);
    if (node.isPresent()) {
      return node.get().getSlug();
    }
    return "";
  }
  
  public void createNodeWithTeamConnection(RelationshipType type, String ref, String slug, String to, Optional<Map<String, String>> data) {
    if (usev2) {
      Optional<RelationshipEntityV2> toNode = this.relationshipRepositoryV2.findFirstByTypeAndRefOrSlug(RelationshipType.TEAM, to);
      this.relationshipRepositoryV2.insert(new RelationshipEntityV2(type, ref, slug, Optional.empty(), new RelationshipConnectionEntity(RelationshipLabel.BELONGSTO, toNode.get().getId(), data)));
    } else {
      //TODO
      LOGGER.warn("This method is not implemented yet");
    }
  }
  
  public void createNode(RelationshipType type, String ref, String slug, Optional<Map<String, String>> data) {
    if (usev2) {
      this.relationshipRepositoryV2.insert(new RelationshipEntityV2(type, ref, slug, data));
    } else {
      this.relationshipNodeRepository.insert(new RelationshipNodeEntity(type, ref, slug, data));
    }
  }
  
  public void upsertTeamConnectionBySlug(RelationshipType fromType, String fromRef, RelationshipLabel label, String slug, Optional<Map<String, String>> data) {
    if (usev2) {
//      this.relationshipRepositoryV2.findAndUpdateConnectionByTypeAndRefOrSlugAndConnectionsLabelAndConnectionsTo(fromType, fromRef, label, getTeamRelationshipId(slug), "reader");
//      this.relationshipRepositoryV2.existsByTypeAndRefOrSlug(fromType, fromRef, slug)
      Optional<RelationshipEntityV2> teamNode = this.relationshipRepositoryV2.findFirstByTypeAndRefOrSlug(RelationshipType.TEAM, slug);
      if (teamNode.isPresent()) {
        long update = this.relationshipRepositoryV2.updateConnectionByTypeAndRefOrSlug(fromType, fromRef, new ObjectId(teamNode.get().getId()), data.get());
        LOGGER.debug("Updates made: {}", update);
        if (update == 0) {
          long push = this.relationshipRepositoryV2.pushConnectionByTypeAndRefOrSlug(fromType, fromRef, new RelationshipConnectionEntity(label, teamNode.get().getId(), data));
          LOGGER.debug("Push made: {}", push);
        }
      }
    } else {
      LOGGER.debug(fromType + " - " + fromRef);
      Optional<RelationshipNodeEntity> fromNode = this.relationshipNodeRepository.findFirstByTypeAndRefOrSlug(fromType, fromRef);
      if (fromNode.isPresent()) {
        Optional<RelationshipEdgeEntity> connection = this.relationshipEdgeRepository.findFirstByFromAndLabelAndTo(fromNode.get().getId(), label, getTeamRelationshipId(slug));
        if (connection.isPresent()) {
          connection.get().getData().putAll(data.get());
          this.relationshipEdgeRepository.save(connection.get());
        } else {
          this.relationshipEdgeRepository.insert(new RelationshipEdgeEntity(fromNode.get().getId(), label, getTeamRelationshipId(slug), data));
        }
      }
    }
  }
  
  public Map<String, String> getMembersAndRoleForTeamBySlug(String slug) {
    Map<String, String> memberRoleMap = new HashMap<String, String>();
    if (usev2) {
      RelationshipEntityV2Graph graph = this.relationshipRepositoryV2.graphRelationshipsByTypeTo(RelationshipType.TEAM, slug, mongoConfiguration.fullCollectionName("relationships_v2"), RelationshipType.USER);
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
    } else {
      RelationshipNodeEntityAggregate aggregate = this.relationshipNodeRepository.findRelationshipsByGraphTo(RelationshipType.TEAM, slug, mongoConfiguration.fullCollectionName("relationship_edges"), RelationshipLabel.MEMBEROF, mongoConfiguration.fullCollectionName("relationship_nodes"));
      LOGGER.debug(aggregate.toString());
      if (!aggregate.getChildren().isEmpty() && !aggregate.getPaths().isEmpty()) {
        Map<String, Map<String, String>> pathRoles = aggregate.getPaths().stream().collect(Collectors.toMap(RelationshipEdgeEntity::getFrom, RelationshipEdgeEntity::getData));
        aggregate.getChildren().forEach(n -> {
          memberRoleMap.put(n.getRef(), pathRoles.containsKey(n.getId()) && pathRoles.get(n.getId()).containsKey("role") ? pathRoles.get(n.getId()).get("role") : RoleEnum.READER.getLabel());
        });
      }
    }
    return memberRoleMap;
  }
  
  // Team Node
  public void updateNodeSlug(RelationshipType type, String currentSlug, String newSlug) {
    if (usev2) {
      RelationshipEntityV2 entity = this.relationshipRepositoryV2.findAndSetSlugByTypeAndSlug(type, currentSlug, newSlug);
      teamSlugToRelationshipId.remove(currentSlug);
      teamSlugToRelationshipId.put(newSlug, entity.getId());
//      teamSlugToRef.remove(currentSlug);
//      teamSlugToRef.put(newSlug, entity.getRef());
    } else {

      RelationshipNodeEntity entity = this.relationshipNodeRepository.findAndSetSlugByTypeAndSlug(type, currentSlug, newSlug);
      teamSlugToRelationshipId.remove(currentSlug);
      teamSlugToRelationshipId.put(newSlug, entity.getId());
//      teamSlugToRef.remove(currentSlug);
//      teamSlugToRef.put(newSlug, entity.getRef());
    }
  }
  
  public void removeNodeByRefOrSlug(RelationshipType type, String slug) {
    if (usev2) {
      this.relationshipRepositoryV2.deleteByTypeAndRefOrSlug(type, slug);
    } else {
      this.relationshipNodeRepository.deleteByTypeAndRefOrSlug(type, slug);
    }
  }
  
  /*
   * This is used for top level nodes such as Team, GlobalTasks, etc
   */
  public boolean doesSlugOrRefExistForType(RelationshipType type, String slug) {
    if (usev2) {
      return this.relationshipRepositoryV2.existsByTypeAndRefOrSlug(type, slug);
    } else {
      return this.relationshipNodeRepository.existsByTypeAndSlug(type, slug);
    }
  }
  
  /*
   * This is used to find a slug for all Nodes of a Type within a particular team
   */
  public boolean doesSlugOrRefExistForTypeInTeam(RelationshipType type, String slug, String team) {
    if (usev2) {
      RelationshipEntityV2Graph graph = this.relationshipRepositoryV2.graphRelationshipsByTypeTo(RelationshipType.TEAM, team, mongoConfiguration.fullCollectionName("relationships_v2"), type);
      if (!graph.getChildren().isEmpty()) {
        return graph.getChildren().stream().anyMatch(c -> c.getSlug().equals(slug) || c.getRef().equals(slug));
      }
    } else {
      //TODO
      LOGGER.warn("This method is not implemented yet");
    }
    return false;
  }
  
  /* 
   * Helper method used for Schedules to get their Team
   */
  public String getWorkflowsTeamSlug(String ref) {
    RelationshipEntityV2Graph graph = this.relationshipRepositoryV2.findWorkflowTeamRelationship(ref, mongoConfiguration.fullCollectionName("relationships_v2"));
    return !graph.getChildren().isEmpty() && graph.getChildren().get(0).getSlug() != null ? graph.getChildren().get(0).getSlug() : "";
  }
  
  public Map<String, String> getMyTeamRefsAndRoles(String userId) {
    Map<String, String> memberRoleMap = new HashMap<String, String>();
    if (usev2) {
      RelationshipEntityV2Graph graph = this.relationshipRepositoryV2.findUserTeamRelationships(userId,  mongoConfiguration.fullCollectionName("relationships_v2"));
      LOGGER.debug(graph.toString());
      if (!graph.getTeams().isEmpty()) {
        graph.getTeams().forEach(r -> {
          memberRoleMap.put(r.getRef(), r.getData().containsKey("role") ? r.getData().get("role") : RoleEnum.READER.getLabel());
        });
      }
    } else {
      RelationshipNodeEntityAggregate aggregate = getMyTeamAggregate(userId);
      LOGGER.debug(aggregate.toString());
      if (!aggregate.getChildren().isEmpty() && !aggregate.getPaths().isEmpty()) {
        Map<String, Map<String, String>> pathRoles = aggregate.getPaths().stream().collect(Collectors.toMap(RelationshipEdgeEntity::getTo, RelationshipEdgeEntity::getData));
        LOGGER.debug("Roles: {}", pathRoles.toString());
        aggregate.getChildren().forEach(n -> {
          memberRoleMap.put(n.getRef(), pathRoles.containsKey(n.getId()) && pathRoles.get(n.getId()).containsKey("role") ? pathRoles.get(n.getId()).get("role") : RoleEnum.READER.getLabel());
        });
      }
    }
    LOGGER.debug("Roles: {}", memberRoleMap.toString());
    return memberRoleMap;
  }
  
  public Map<String, String> getMyTeamSlugsAndRoles(String userId) {
    Map<String, String> memberRoleMap = new HashMap<String, String>();
    if (usev2) {
      RelationshipEntityV2Graph aggregate = this.relationshipRepositoryV2.findUserTeamRelationships(userId,  mongoConfiguration.fullCollectionName("relationships_v2"));
      LOGGER.debug(aggregate.toString());
      if (!aggregate.getTeams().isEmpty()) {
        aggregate.getTeams().forEach(r -> {
          memberRoleMap.put(r.getSlug(), r.getData().containsKey("role") ? r.getData().get("role") : RoleEnum.READER.getLabel());
        });
      }
    } else {
      RelationshipNodeEntityAggregate aggregate = getMyTeamAggregate(userId);
      if (!aggregate.getChildren().isEmpty() && !aggregate.getPaths().isEmpty()) {
        Map<String, Map<String, String>> pathRoles = aggregate.getPaths().stream().collect(Collectors.toMap(RelationshipEdgeEntity::getTo, RelationshipEdgeEntity::getData));
        LOGGER.debug("Roles: {}", pathRoles.toString());
        aggregate.getChildren().forEach(n -> {
          memberRoleMap.put(n.getSlug(), pathRoles.containsKey(n.getId()) && pathRoles.get(n.getId()).containsKey("role") ? pathRoles.get(n.getId()).get("role") : RoleEnum.READER.getLabel());
        });
      }
    }
    LOGGER.debug("Roles: {}", memberRoleMap.toString());
    return memberRoleMap;
  }
  
  private RelationshipNodeEntityAggregate getMyTeamAggregate(String userId) {
    return this.relationshipNodeRepository.findRelationshipsByGraphFrom(RelationshipType.USER, userId, mongoConfiguration.fullCollectionName("relationship_edges"), RelationshipLabel.MEMBEROF, mongoConfiguration.fullCollectionName("relationship_nodes"));
  }
}
