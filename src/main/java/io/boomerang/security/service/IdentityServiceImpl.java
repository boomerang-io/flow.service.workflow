package io.boomerang.security.service;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.EnumUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import io.boomerang.client.ExternalUserProfile;
import io.boomerang.client.ExternalUserService;
import io.boomerang.data.entity.TeamEntity;
import io.boomerang.data.entity.UserEntity;
import io.boomerang.data.repository.TeamRepository;
import io.boomerang.data.repository.UserRepository;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.model.OneTimeCode;
import io.boomerang.model.TeamSummary;
import io.boomerang.model.TeamSummaryInsights;
import io.boomerang.model.User;
import io.boomerang.model.UserProfile;
import io.boomerang.model.UserRequest;
import io.boomerang.model.UserStatus;
import io.boomerang.model.enums.RelationshipLabel;
import io.boomerang.model.enums.RelationshipType;
import io.boomerang.model.enums.TeamStatus;
import io.boomerang.model.enums.UserType;
import io.boomerang.security.model.AuthType;
import io.boomerang.security.model.Token;
import io.boomerang.security.repository.RoleRepository;
import io.boomerang.service.RelationshipService;
import io.boomerang.service.RelationshipServiceImpl;
import io.boomerang.service.TeamService;

@Service
public class IdentityServiceImpl implements IdentityService {

  private static final Logger LOGGER = LogManager.getLogger();

  @Value("${flow.externalUrl.user}")
  private String externalUserUrl;

  @Value("${flow.otc}")
  private String corePlatformOTC;

  @Autowired
  private ExternalUserService extUserService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private RelationshipServiceImpl relationshipServiceImpl;

  @Autowired
  private TeamService teamService;

  @Autowired
  private TeamRepository teamRepository;

  @Autowired
  private RoleRepository roleRepository;

  @Autowired
  private MongoTemplate mongoTemplate;

  @Override
  public ResponseEntity<Boolean> activateSetup(OneTimeCode otc) {
    if (externalUserUrl.isBlank()) {
      if (corePlatformOTC.equals(otc.getOtc())) {
        return new ResponseEntity<>(HttpStatus.OK);
      }
    }
    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
  }

  /*
   * Used by the CreateUserSession to check if instance is activated
   */
  @Override
  public boolean isActivated() {
    if (externalUserUrl.isBlank() && userRepository.count() == 0) {
      return false;
    }
    return true;
  }

  /*
   * Used by the AuthenticationFilter to Retrieve the user and create the Users Team
   */
  @Override
  public Optional<UserEntity> getAndRegisterUser(String email, String firstName, String lastName,
      Optional<UserType> usertype, boolean allowUserCreation) {
    if (email == null || email.isBlank()) {
      return Optional.empty();
    }

    Optional<UserEntity> userEntity = getUserEntityByEmail(email);
    boolean createRelationshipNode = false;
    if (externalUserUrl.isBlank()) {
      if (userEntity.isEmpty() && allowUserCreation) {          
        // Create new User (UserEntity is defaulted on new)
        UserEntity newUserEntity = new UserEntity();
        newUserEntity.setEmail(email);
        if (usertype.isPresent()) {
          newUserEntity.setType(usertype.get());
        }
        userEntity = Optional.of(newUserEntity);
      } else if (userEntity.isEmpty()) {
        return Optional.empty();
      }
      // Refresh name from provided details
      String name;
      if (firstName == null && lastName == null && email != null) {
        name = email;
      } else {
        name = String.format("%s %s", Optional.ofNullable(firstName).orElse(""),
            Optional.ofNullable(lastName).orElse("")).trim();
      }
      userEntity.get().setName(name);
      userEntity.get().setLastLoginDate(new Date());
      if (userEntity.get().getSettings().getIsFirstVisit()) {
        userEntity.get().getSettings().setIsFirstVisit(false);
      }
      userEntity = Optional.of(userRepository.save(userEntity.get()));
      //Create User relationship node if entity was created
      //At end due to the save only happening at the end.
      if (createRelationshipNode) {        
        relationshipServiceImpl.createNode(RelationshipType.USER, userEntity.get().getId(), email, Optional.empty());
      }
    }

    return userEntity;
  }

  private void convertExternalUserType(ExternalUserProfile extUser, UserEntity userEntity) {
    if (!UserType.user.equals(extUser.getType()) && !UserType.admin.equals(extUser.getType())
        && !UserType.operator.equals(extUser.getType())) {
      userEntity.setType(UserType.user);
    }
  }

  @Override
  public Optional<User> getUserByID(String userId) {
    if (externalUserUrl.isBlank()) {
      Optional<UserEntity> userEntity = userRepository.findById(userId);
      if (userEntity.isPresent()) {
        return Optional.of(new User(userEntity.get()));
      }
    } else {
      ExternalUserProfile extUser = extUserService.getUserProfileById(userId);
      if (extUser != null) {
        User user = new User();
        BeanUtils.copyProperties(extUser, user);
        convertExternalUserType(extUser, user);
        return Optional.of(user);
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<User> getUserByEmail(String userEmail) {
    Optional<UserEntity> userEntity = getUserEntityByEmail(userEmail);
    if (userEntity.isPresent()) {
      return Optional.of(new User(userEntity.get()));
    }
    return Optional.empty();
  }

  private Optional<UserEntity> getUserEntityByEmail(String userEmail) {
    if (externalUserUrl.isBlank()) {
      UserEntity extUser =
          userRepository.findByEmailIgnoreCaseAndStatus(userEmail, UserStatus.active);
      if (extUser != null) {
        UserEntity userEntity = new UserEntity();
        BeanUtils.copyProperties(extUser, userEntity);
        return Optional.of(userEntity);
      }
    } else {
      ExternalUserProfile extUser = extUserService.getUserProfileByEmail(userEmail);
      if (extUser != null && UserStatus.active.toString().equals(extUser.getStatus())) {
        UserEntity userEntity = new UserEntity();
        BeanUtils.copyProperties(extUser, userEntity);
        convertExternalUserType(extUser, userEntity);
        return Optional.of(userEntity);
      }
    }
    return Optional.empty();
  }

  /*
   * Retrieves the profile for current user session
   */
  @Override
  public UserProfile getCurrentProfile() {
    UserProfile profile = new UserProfile();
    if (externalUserUrl.isBlank()) {
      UserEntity user = getCurrentUser();
      profile = new UserProfile(user);
    } else {
      String userId = getCurrentPrincipal();
      ExternalUserProfile extUserProfile = extUserService.getUserProfileById(userId);
      if (extUserProfile != null) {
        BeanUtils.copyProperties(extUserProfile, profile);
        convertExternalUserType(extUserProfile, profile);
      }
    }
    // Add TeamSummaries
//    Map<String, String> teamRefs = relationshipService.getMyTeamRefsAndRoles(profile.getId());
    Map<String, String> teamRefs = relationshipServiceImpl.getMyTeamRefsAndRoles(profile.getId());
    //TODO - change to return an Object with teamId, teamSlug, 
    List<TeamSummary> teamSummaries = new LinkedList<>();
    List<String> permissions = new LinkedList<>();
    teamRefs.forEach((k, v) -> {
      Optional<TeamEntity> teamEntity = teamRepository.findById(k);
      if (teamEntity.isPresent()) {
        // Generate TeamSummary + Insight
        TeamSummary ts = new TeamSummary(teamEntity.get());
        TeamSummaryInsights tsi = new TeamSummaryInsights();
        Map<String, String> membersAndRoles = relationshipServiceImpl.getMembersAndRoleForTeam(k);
        tsi.setMembers(Long.valueOf(membersAndRoles.size()));
        List<String> workflowRefs =
            relationshipServiceImpl.getFilteredRefs(Optional.of(RelationshipType.WORKFLOW),
                Optional.empty(), RelationshipLabel.BELONGSTO,
                RelationshipType.TEAM, k, true);
        tsi.setWorkflows(Long.valueOf(workflowRefs.size()));
        ts.setInsights(tsi);
        teamSummaries.add(ts);

        // Generate Permissions
        roleRepository.findByTypeAndName("team", v).getPermissions().stream()
            .forEach(p -> permissions.add(p.replace("{principal}", k)));
      }
    });
    profile.setTeams(teamSummaries);
    profile.setPermissions(permissions);
    return profile;
  }
  
  @Override
  public void updateCurrentProfile(UserRequest request) {
    String userId = getCurrentPrincipal();
    request.setId(userId);
    this.apply(request);
  }

  // /*
  // * Retrieves the profile for a specified user. Does not use current session.
  // */
  // @Override
  // public UserProfile getProfile(String userId) {
  // if (externalUserUrl.isBlank()) {
  // Optional<UserEntity> flowUser = userRepository.findById(userId);
  // if (flowUser.isPresent()) {
  // UserProfile profile = new UserProfile(flowUser.get());
  // List<String> teamRefs =
  // relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.USER),
  // Optional.of(List.of(userId)), Optional.of(RelationshipType.MEMBEROF),
  // Optional.of(RelationshipRef.TEAM), Optional.empty());
  // List<Team> usersTeams = teamService.query(Optional.empty(), Optional.empty(), Optional.empty(),
  // Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(teamRefs)).getContent();
  // profile.setTeams(usersTeams);
  // return profile;
  // }
  // } else {
  // ExternalUserProfile extUserProfile = extUserService.getUserProfileById(userId);
  // if (extUserProfile != null) {
  // UserProfile profile = new UserProfile();
  // BeanUtils.copyProperties(extUserProfile, profile);
  // convertExternalUserType(extUserProfile, profile);
  // List<String> teamRefs =
  // relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.USER),
  // Optional.of(List.of(userId)), Optional.of(RelationshipType.MEMBEROF),
  // Optional.of(RelationshipRef.TEAM), Optional.empty());
  // List<Team> usersTeams = teamService.query(Optional.empty(), Optional.empty(), Optional.empty(),
  // Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(teamRefs)).getContent();
  // profile.setTeams(usersTeams);
  // return profile;
  // }
  // }
  // return null;
  // }

  /*
   * Query for Users
   */
  @Override
  public Page<User> query(Optional<Integer> queryPage, Optional<Integer> queryLimit,
      Optional<Direction> queryOrder, Optional<String> querySort,
      Optional<List<String>> queryLabels, Optional<List<String>> queryStatus,
      Optional<List<String>> queryIds) {
    Pageable pageable = Pageable.unpaged();
    final Sort sort =
        Sort.by(new Order(queryOrder.orElse(Direction.ASC), querySort.orElse("name")));
    if (queryLimit.isPresent()) {
      pageable = PageRequest.of(queryPage.get(), queryLimit.get(), sort);
    }

    // TODO figure out a Ref search for querying users - lock to admin for now.
    // List<String> userRefs = relationshipService.getFilteredRefs(Optional.empty(),
    // Optional.empty(),
    // Optional.of(RelationshipType.MEMBEROF), Optional.of(RelationshipRef.TEAM), queryIds);

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

    if (queryIds.isPresent()) {
      Criteria criteria = Criteria.where("id").in(queryIds);
      criteriaList.add(criteria);
    }

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

    List<UserEntity> entities = mongoTemplate.find(query, UserEntity.class);
    LOGGER.debug("Found " + entities.size() + " users.");

    List<User> users = new LinkedList<>();
    if (!entities.isEmpty()) {
      entities.forEach(e -> users.add(new User(e)));
    }
    Page<User> pages = PageableExecutionUtils.getPage(users, pageable,
        () -> mongoTemplate.count(query, UserEntity.class));

    return pages;
  }

  @Override
  public User create(UserRequest request) {
    if (externalUserUrl.isBlank() && request != null && request.getEmail() != null
        && this.userRepository.countByEmailIgnoreCaseAndStatus(request.getEmail(),
            UserStatus.active) == 0) {
      // Create User (UserEntity is defaulted on new)
      UserEntity userEntity = new UserEntity();
      userEntity.setEmail(request.getEmail());
      if (request.getName() != null && !request.getName().isBlank()) {
        userEntity.setName(request.getName());
      }
      if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
        userEntity.setDisplayName(request.getDisplayName());
      }
      if (request.getType() != null) {
        userEntity.setType(request.getType());
      }
      if (request.getLabels() != null) {
        userEntity.setLabels(request.getLabels());
      }
      userEntity.getSettings().setHasConsented(true);
      userEntity = this.userRepository.save(userEntity);

      return new User(userEntity);
    } else {
      // TODO throw exception
      return null;
    }
  }

  @Override
  // TODO throw exception if externalUserURL is provided
  public void apply(UserRequest request) {
    Optional<UserEntity> userOptional = Optional.empty();
    if (request != null && request.getId() != null && !request.getId().isBlank()) {
      userOptional = this.userRepository.findByIdAndStatus(request.getId(), UserStatus.active);
    } else if (request != null && request.getEmail() != null && !request.getEmail().isBlank()) {
      userOptional = Optional.of(this.userRepository
          .findByEmailIgnoreCaseAndStatus(request.getEmail(), UserStatus.active));
    }
    if (userOptional.isPresent()) {
      UserEntity user = userOptional.get();
      if (request.getName() != null && !request.getName().isBlank()) {
        user.setName(request.getName());
      }
      if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
        user.setDisplayName(request.getDisplayName());
      }
      if (request.getType() != null) {
        user.setType(request.getType());
      }
      if (request.getLabels() != null) {
        user.setLabels(request.getLabels());
      }
      this.userRepository.save(user);
    }
  }

  @Override
  /*
   * Delete User
   * 
   * User must leave or delete teams prior to deleting account
   */
  //TODO - determine if the user needs to be removed from ApproverGroups, and anything else
  public void delete(String userId) {
    Optional<UserEntity> user = userRepository.findById(userId);
    Map<String, String> teamRefsAndRoles = relationshipServiceImpl.getMyTeamRefsAndRoles(userId);
    if (!teamRefsAndRoles.isEmpty()) {
      throw new BoomerangException(BoomerangError.USER_UNABLE_TO_DELETE);
    }
    if (user.isPresent()) {
      userRepository.deleteById(userId);
      relationshipServiceImpl.removeNodeByRefOrSlug(RelationshipType.USER, userId);
    }
  }

  @Override
  @NoLogging
  public User getCurrentUser() {
    Token token = this.getCurrentIdentity();
    return getUserByID(token.getPrincipal()).get();
  }

  @Override
  @NoLogging
  public boolean isCurrentUserAdmin() {
    boolean isUserAdmin = false;
    final UserEntity userEntity = getCurrentUser();
    if (userEntity != null && (userEntity.getType() == UserType.admin
        || userEntity.getType() == UserType.operator || userEntity.getType() == UserType.auditor
        || userEntity.getType() == UserType.author)) {
      isUserAdmin = true;
    }
    return isUserAdmin;
  }

  @Override
  public String getCurrentPrincipal() {
    Token token = this.getCurrentIdentity();
    return token.getPrincipal();
  }

  @Override
  public AuthType getCurrentScope() {
    if (SecurityContextHolder.getContext() != null
        && SecurityContextHolder.getContext().getAuthentication() != null
        && SecurityContextHolder.getContext().getAuthentication().getDetails() != null
        && SecurityContextHolder.getContext().getAuthentication().getDetails() instanceof Token) {
      Token details = (Token) SecurityContextHolder.getContext().getAuthentication().getDetails();
      return details.getType();
    }
    return null;
  }

  @Override
  public Token getCurrentIdentity() {
    if (SecurityContextHolder.getContext() != null
        && SecurityContextHolder.getContext().getAuthentication() != null
        && SecurityContextHolder.getContext().getAuthentication().getDetails() != null) {
      Object details = SecurityContextHolder.getContext().getAuthentication().getDetails();
      return (Token) details;
    } else {
      return null;
    }
  }
}
