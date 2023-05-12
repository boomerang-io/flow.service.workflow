package io.boomerang.security.service;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
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
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.security.model.Token;
import io.boomerang.security.model.TokenScope;
import io.boomerang.v4.data.entity.UserEntity;
import io.boomerang.v4.data.repository.UserRepository;
import io.boomerang.v4.model.OneTimeCode;
import io.boomerang.v4.model.Team;
import io.boomerang.v4.model.User;
import io.boomerang.v4.model.UserProfile;
import io.boomerang.v4.model.UserRequest;
import io.boomerang.v4.model.UserResponsePage;
import io.boomerang.v4.model.UserStatus;
import io.boomerang.v4.model.UserType;
import io.boomerang.v4.model.enums.RelationshipRef;
import io.boomerang.v4.model.enums.RelationshipType;
import io.boomerang.v4.model.enums.TeamStatus;
import io.boomerang.v4.model.ref.Workflow;
import io.boomerang.v4.service.RelationshipService;
import io.boomerang.v4.service.TeamService;
import io.boomerang.v4.service.WorkflowService;

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
  private RelationshipService relationshipService;
  
  @Autowired
  private TeamService teamService;
  
  @Autowired
  private WorkflowService workflowService;

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
      Optional<UserType> usertype) {
    if (email == null || email.isBlank()) {
      return Optional.empty();
    }

    Optional<UserEntity> userEntity = getUserEntityByEmail(email);
    if (externalUserUrl.isBlank()) {
      if (userEntity.isEmpty()) {
        // Create new User (UserEntity is defaulted on new)
        UserEntity newUserEntity = new UserEntity();
        newUserEntity.setEmail(email);
        userEntity = Optional.of(newUserEntity);
      }
      // Refresh name from provided details
      String name = String.format("%s %s", Optional.ofNullable(firstName).orElse(""),
          Optional.ofNullable(lastName).orElse("")).trim();
      if (firstName == null && lastName == null && email != null) {
        name = email;
      }
      userEntity.get().setName(name);
      userEntity.get().setLastLoginDate(new Date());
      userEntity.get().getSettings().setIsFirstVisit(false);
      userEntity = Optional.of(userRepository.save(userEntity.get()));
    }

    // Create Users Personal Team.
//    TeamRequest request = new TeamRequest();
//    request.setName(lastName + "'s Team");
//    request.setUsers(List.of(new UserSummary(userEntity.get())));
//    teamService.create(request, TeamType.personal);

    return userEntity;
  }

  private void convertExternalUserType(ExternalUserProfile extUser, UserEntity userEntity) {
    if (!UserType.user.equals(extUser.getType()) && !UserType.admin.equals(extUser.getType()) && !UserType.operator.equals(extUser.getType())) {
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
    UserEntity extUser = userRepository.findByEmailIgnoreCaseAndStatus(userEmail, UserStatus.active);
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
    UserEntity user = getCurrentUser();
    UserProfile profile = new UserProfile(user);
    List<String> teamRefs = relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.USER), Optional.of(List.of(user.getId())), Optional.of(RelationshipType.MEMBEROF), Optional.of(RelationshipRef.TEAM), Optional.empty());
    List<Team> usersTeams = teamService.query(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(teamRefs)).getContent();
    profile.setTeams(usersTeams);
    return profile;
  }

  /*
   * Retrieves the profile for a specified user. Does not use current session.
   */
  @Override
  public UserProfile getProfile(String userId) {
    if (externalUserUrl.isBlank()) {
      Optional<UserEntity> flowUser = userRepository.findById(userId);
      if (flowUser.isPresent()) {
        UserProfile profile = new UserProfile(flowUser.get());
        List<String> teamRefs = relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.USER), Optional.of(List.of(userId)), Optional.of(RelationshipType.MEMBEROF), Optional.of(RelationshipRef.TEAM), Optional.empty());
        List<Team> usersTeams = teamService.query(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(teamRefs)).getContent();
        profile.setTeams(usersTeams);
        return profile;
      }
    } else {
      ExternalUserProfile extUserProfile = extUserService.getUserProfileById(userId);
      if (extUserProfile != null) {
        UserProfile profile = new UserProfile();
        BeanUtils.copyProperties(extUserProfile, profile);
        convertExternalUserType(extUserProfile, profile);
        List<String> teamRefs = relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.USER), Optional.of(List.of(userId)), Optional.of(RelationshipType.MEMBEROF), Optional.of(RelationshipRef.TEAM), Optional.empty());
        List<Team> usersTeams = teamService.query(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(teamRefs)).getContent();
        profile.setTeams(usersTeams);
        return profile;
      }
    }
    return null;
  }

  /*
   * Query for Users
   */
  @Override
  public UserResponsePage query(Optional<Integer> queryPage, Optional<Integer> queryLimit, Optional<Direction> querySort, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus, Optional<List<String>> queryIds) {
    Pageable pageable = Pageable.unpaged();
    final Sort sort = Sort.by(new Order(querySort.orElse(Direction.ASC), "creationDate"));
    if (queryLimit.isPresent()) {
      pageable = PageRequest.of(queryPage.get(), queryLimit.get(), sort);
    }
    
    //TODO figure out a Ref search for querying users - lock to admin for now.
//    List<String> userRefs = relationshipService.getFilteredRefs(Optional.empty(), Optional.empty(),
//        Optional.of(RelationshipType.MEMBEROF), Optional.of(RelationshipRef.TEAM), queryIds);

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

    Criteria criteria = Criteria.where("id").in(queryIds);
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

    Page<UserEntity> pages =
        PageableExecutionUtils.getPage(mongoTemplate.find(query, UserEntity.class),
            pageable, () -> mongoTemplate.count(query, UserEntity.class));

    List<UserEntity> entities = pages.getContent();
    List<User> users = new LinkedList<>();

    if (!entities.isEmpty()) {
      entities.forEach(e -> users.add(new User(e)));
    }
    return new UserResponsePage(users, pageable, pages.getNumberOfElements());
  }
  
  @Override
  public User create(UserRequest request) {
    if (externalUserUrl.isBlank() && request != null && request.getEmail() != null && this.userRepository.countByEmailIgnoreCaseAndStatus(request.getEmail(), UserStatus.active) == 0) {
      //Create User (UserEntity is defaulted on new)
      UserEntity userEntity = new UserEntity();
      userEntity.setEmail(request.getEmail());
      if (request.getName() != null && !request.getName().isBlank()) {
        userEntity.setName(request.getName());
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
      //TODO throw exception
      return null;
    }
  }

  @Override
  //TODO throw exception if externalUserURL is provided
  public void apply(UserRequest request) {
    Optional<UserEntity> userOptional = Optional.empty();
    if (request != null && request.getId() != null && !request.getId().isBlank()) {
      userOptional = this.userRepository.findByIdAndStatus(request.getId(), UserStatus.active);
    } else if (request != null && request.getEmail() != null && !request.getEmail().isBlank()) {
      userOptional = Optional.of(this.userRepository.findByEmailIgnoreCaseAndStatus(request.getEmail(), UserStatus.active));
    }
    if (userOptional.isPresent()) {
      UserEntity user = userOptional.get();
      if (request.getName() != null && !request.getName().isBlank()) {
        user.setName(request.getName());
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
  public void delete(String userId) {
    Optional<UserEntity> user = userRepository.findById(userId);
    if (user.isPresent()) {
      user.get().setStatus(UserStatus.deleted);
      userRepository.save(user.get());
    }
  }

  @Override
  @NoLogging
  public User getCurrentUser() {
    Token token = this.getCurrentIdentity();
    Optional<String> ref = relationshipService.getRelationshipRef(RelationshipRef.TOKEN, token.getId(), RelationshipType.AUTHORIZES);
    if (ref.isPresent()) {
        return getUserByID(ref.get()).get();
    }
    return null;
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
  public Workflow getCurrentWorkflow() {
    Token token = this.getCurrentIdentity();
    Optional<String> ref = relationshipService.getRelationshipRef(RelationshipRef.TOKEN, token.getId(), RelationshipType.AUTHORIZES);
    if (ref.isPresent()) {
      return workflowService.get(ref.get(), Optional.empty(), false).getBody();
  }
  return null;
  }
  
  @Override
  public Team getCurrentTeam() {
    Token token = this.getCurrentIdentity();
    Optional<String> ref = relationshipService.getRelationshipRef(RelationshipRef.TOKEN, token.getId(), RelationshipType.AUTHORIZES);
    if (ref.isPresent()) {
        return teamService.get(ref.get()).getBody();
    }
    return null;
  }

  @Override
  public String getCurrentPrincipal() {
    Token token = this.getCurrentIdentity();
    Optional<String> ref = relationshipService.getRelationshipRef(RelationshipRef.TOKEN, token.getId(), RelationshipType.AUTHORIZES);
    if (ref.isPresent()) {
        return ref.get();
    }
    return null;
  }

  @Override
  public TokenScope getCurrentScope() {
    if (SecurityContextHolder.getContext() != null
        && SecurityContextHolder.getContext().getAuthentication() != null
        && SecurityContextHolder.getContext().getAuthentication().getDetails() != null
        && SecurityContextHolder.getContext().getAuthentication()
        .getDetails() instanceof Token) {
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
