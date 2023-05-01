package io.boomerang.service;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import io.boomerang.client.ExternalUserService;
import io.boomerang.client.UserProfile;
import io.boomerang.model.FlowUserProfile;
import io.boomerang.model.OneTimeCode;
import io.boomerang.model.UserQueryResult;
import io.boomerang.mongo.model.UserStatus;
import io.boomerang.mongo.model.UserType;
import io.boomerang.security.model.Token;
import io.boomerang.security.model.TokenType;
import io.boomerang.security.service.NoLogging;
import io.boomerang.v4.data.entity.UserEntity;
import io.boomerang.v4.model.User;
import io.boomerang.v4.service.TeamService;
import io.boomerang.v4.service.UserService;
import io.boomerang.v4.service.WorkflowService;

@Service
public class UserIdentityServiceImpl implements UserIdentityService {

  @Value("${flow.externalUrl.user}")
  private String flowExternalUrlUser;

  @Autowired
  private ExternalUserService extUserService;

  @Autowired
  private UserService userService;

  @Value("${boomerang.otc}")
  private String corePlatformOTC;

//  @Autowired
//  private WorkflowService workflowService;
//
//  @Autowired
//  private TeamService flowTeamService;

  @Override
  public UserEntity getCurrentUser() {
    if (flowExternalUrlUser.isBlank()) {
//      UserEntity user = this.getUserDetails();
//      String email = user.getEmail();
//      UserEntity entity = userService.getUserWithEmail(email);
      UserEntity entity = this.getUserDetails();
      entity.setHasConsented(true);
      return entity;
    } else {
      UserProfile userProfile = extUserService.getInternalUserProfile();
      UserEntity flowUser = new UserEntity();
      if (userProfile == null) {
        return null;
      }
      BeanUtils.copyProperties(userProfile, flowUser);

      String email = userProfile.getEmail();
      UserEntity dbUser = userService.getUserWithEmail(email);
      if (dbUser == null) {
        flowUser.setId(null);
        userService.registerUser(flowUser);
      } else {
        flowUser.setQuotas(dbUser.getQuotas());
      }
      return flowUser;
    }
  }

//  private UserEntity getOrRegisterUser(UserType userType) {
//    UserEntity userDetails = this.getUserDetails();
//    String email = userDetails.getEmail();
//    String firstName = userDetails.getFirstName();
//    String lastName = userDetails.getLastName();
//    String name = String.format("%s %s", Optional.ofNullable(firstName).orElse(""),
//        Optional.ofNullable(lastName).orElse("")).trim();
//    if (firstName == null && lastName == null && email != null) {
//      name = email;
//    }
//    return userService.getOrRegisterUser(email, name, userType);
//  }

  @Override
  public UserEntity getUserByID(String userId) {
    if (flowExternalUrlUser.isBlank()) {
      Optional<UserEntity> flowUser = userService.getUserById(userId);
      if (flowUser.isPresent()) {
        UserEntity profile = new UserEntity();
        BeanUtils.copyProperties(flowUser.get(), profile);

        return profile;
      }
    } else {
      UserProfile userProfile = extUserService.getUserProfileById(userId);
      FlowUserProfile flowUser = new FlowUserProfile();
      if (userProfile != null) {
        BeanUtils.copyProperties(userProfile, flowUser);
        flowUser.setType(userProfile.getType());

        return flowUser;
      }
    }
    return null;
  }
  
  @Override
  public UserEntity getUserByEmail(String userEmail) {
    UserEntity flowUser = userService.getUserWithEmail(userEmail);
    if (flowUser != null) {
      UserEntity profile = new UserEntity();
      BeanUtils.copyProperties(flowUser, profile);
      return profile;
    }
    return null;
  }

  @Override
  public UserQueryResult getUserViaSearchTerm(String searchTerm, Pageable pageable) {
    final UserQueryResult result = new UserQueryResult();
    final Page<UserEntity> users = userService.findBySearchTerm(searchTerm, pageable);
    final List<UserEntity> userList = new LinkedList<>();
    for (final UserEntity userEntity : users.getContent()) {
      userList.add(userEntity);
    }

    result.setRecords(userList);
    result.setPageable(users);
    return result;
  }

  @Override
  public UserQueryResult getUsers(Pageable pageable) {
    final UserQueryResult result = new UserQueryResult();

    final Page<UserEntity> users = userService.findAll(pageable);
    final List<UserEntity> userList = new LinkedList<>();

    for (final UserEntity userEntity : users.getContent()) {

      userList.add(userEntity);
    }

    result.setRecords(userList);
    result.setPageable(users);
    return result;
  }

  @Override
  public List<UserEntity> getUsersForTeams(List<String> teamIds) {
    return this.userService.getUsersforTeams(teamIds);
  }

  @Override
  public void updateFlowUser(String userId, User updatedFlowUser) {
    Optional<UserEntity> userOptional = this.userService.getUserById(userId);
    if (userOptional.isPresent()) {
      UserEntity user = userOptional.get();
      if (updatedFlowUser.getType() != null) {
        user.setType(updatedFlowUser.getType());
      }
      if (updatedFlowUser.getLabels() != null) {
        user.setLabels(updatedFlowUser.getLabels());
      }
      this.userService.save(user);
    }
  }

  @Override
  public ResponseEntity<Boolean> activateSetup(OneTimeCode otc) {
    if (flowExternalUrlUser.isBlank()) {
      if (corePlatformOTC.equals(otc.getOtc())) {
//        getOrRegisterUser(UserType.admin);
        return new ResponseEntity<>(HttpStatus.OK);
      }
    }
    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
  }

  @Override
  public UserProfile getOrRegisterCurrentUser() {
    if (flowExternalUrlUser.isBlank()) {
      if (userService.getUserCount() == 0) {
        throw new HttpClientErrorException(HttpStatus.LOCKED);
      }
//      UserEntity userEntity = getOrRegisterUser(UserType.user);
      UserEntity userEntity = getUserDetails();
      UserProfile profile = new UserProfile();
      BeanUtils.copyProperties(userEntity, profile);
      return profile;
    } else {
      UserEntity userEntity = getCurrentUser();
      UserProfile profile = new UserProfile();
      BeanUtils.copyProperties(userEntity, profile);
      return profile;
    }
  }

  @Override
  public void deleteFlowUser(String userId) {
    if (userService.getUserById(userId).isPresent()) {
      UserEntity user = userService.getUserById(userId).get();
      user.setStatus(UserStatus.deleted);
      userService.save(user);
    }
  }


  @Override
  public User addFlowUser(User flowUser) {
    if (userService.getUserWithEmail(flowUser.getEmail()) == null) {

      String email = flowUser.getEmail();
      String name = flowUser.getName();
      UserType type = flowUser.getType();
      UserEntity flowUserEntity = userService.getOrRegisterUser(email, name, type);
      flowUserEntity.setQuotas(flowUser.getQuotas());
      flowUserEntity.setHasConsented(true);
      flowUserEntity = userService.save(flowUserEntity);

      User newUser = new User();
      BeanUtils.copyProperties(flowUserEntity, newUser);
      return newUser;
    }
    return new User();
  }

  @Override
  @NoLogging
  public UserEntity getUserDetails() {
    if (SecurityContextHolder.getContext() != null
        && SecurityContextHolder.getContext().getAuthentication() != null
        && SecurityContextHolder.getContext().getAuthentication().getDetails() != null
        && SecurityContextHolder.getContext().getAuthentication()
            .getDetails() instanceof Token) {
      Token token = (Token) SecurityContextHolder.getContext().getAuthentication().getDetails();
      return token.getAuthor();
//      return (UserToken) SecurityContextHolder.getContext().getAuthentication().getDetails();
//    } else {
//      return new UserToken("boomerang@us.ibm.com", "boomerang", "joe");
    } else {
      return null;
    }
  }


  @Override
  public TokenType getCurrentScope() {
    if (SecurityContextHolder.getContext() != null
        && SecurityContextHolder.getContext().getAuthentication() != null
        && SecurityContextHolder.getContext().getAuthentication().getDetails() != null) {
//      Object details = SecurityContextHolder.getContext().getAuthentication().getDetails();
      Token details = (Token) SecurityContextHolder.getContext().getAuthentication().getDetails();
//      if (details instanceof UserToken) {
//        return TokenPermission.user;
//      } else if (details instanceof TeamToken) {
//        return TokenPermission.team;
//      } else if (details instanceof GlobalToken) {
//        return TokenPermission.global;
//      }
      return details.getType();
    }
    return null;
  }

  @Override
  public Token getRequestIdentity() {
    if (SecurityContextHolder.getContext() != null
        && SecurityContextHolder.getContext().getAuthentication() != null
        && SecurityContextHolder.getContext().getAuthentication().getDetails() != null) {
      Object details = SecurityContextHolder.getContext().getAuthentication().getDetails();
      return (Token) details;
    } else {
      return null;
    }
  }

  @Override
  public FlowUserProfile getFullUserProfile(String userId) {
    if (flowExternalUrlUser.isBlank()) {
      Optional<UserEntity> flowUser = userService.getUserById(userId);
      if (flowUser.isPresent()) {
        FlowUserProfile profile = new FlowUserProfile();
        BeanUtils.copyProperties(flowUser.get(), profile);
        setUserTeams(profile);
        return profile;
      }
    } else {
      UserProfile userProfile = extUserService.getUserProfileById(userId);
      FlowUserProfile flowUser = new FlowUserProfile();
      if (userProfile != null) {
        BeanUtils.copyProperties(userProfile, flowUser);
        flowUser.setType(userProfile.getType());
        setUserTeams(flowUser);

        return flowUser;
      }
    }
    return null;
  }


  private void setUserTeams(FlowUserProfile flowUser) {
    if (flowUser.getType() == UserType.admin) {
//      flowUser.setUserTeams(flowTeamService.getAllTeamsListing());
      flowUser.setUserTeams(null);
    } else {
//      flowUser.setUserTeams(flowTeamService.getUsersTeamListing(flowUser));
      flowUser.setUserTeams(null);
    }
  }
}
