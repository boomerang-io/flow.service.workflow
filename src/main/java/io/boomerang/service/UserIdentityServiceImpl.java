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
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import io.boomerang.client.ExernalUserService;
import io.boomerang.client.model.UserProfile;
import io.boomerang.model.FlowUser;
import io.boomerang.model.OneTimeCode;
import io.boomerang.model.UserQueryResult;
import io.boomerang.mongo.entity.FlowUserEntity;
import io.boomerang.mongo.model.UserStatus;
import io.boomerang.mongo.model.UserType;
import io.boomerang.mongo.service.FlowUserService;
import io.boomerang.security.model.UserToken;
import io.boomerang.security.service.UserDetailsService;

@Service
public class UserIdentityServiceImpl implements UserIdentityService {

  @Value("${flow.externalUrl.user}")
  private String flowExternalUrlUser;

  @Autowired
  private UserDetailsService usertDetailsService;

  @Autowired
  private ExernalUserService coreUserService;

  @Autowired
  private FlowUserService flowUserService;

  @Value("${boomerang.otc}")
  private String corePlatformOTC;

  @Override
  public FlowUserEntity getCurrentUser() {
    if (flowExternalUrlUser.isBlank()) {
      UserToken user = usertDetailsService.getUserDetails();
      String email = user.getEmail();
      FlowUserEntity entity = flowUserService.getUserWithEmail(email);
      entity.setHasConsented(true);
      return entity;
    } else {
      UserProfile userProfile = coreUserService.getInternalUserProfile();
      FlowUserEntity flowUser = new FlowUserEntity();
      if (userProfile == null) {
        return null;
      }
      BeanUtils.copyProperties(userProfile, flowUser);
      flowUser.setTeams(null);
      String email = userProfile.getEmail();
      FlowUserEntity dbUser = flowUserService.getUserWithEmail(email);
      if (dbUser == null) {
        flowUser.setId(null);
        flowUserService.registerUser(flowUser);
      } else {
        flowUser.setQuotas(dbUser.getQuotas());
      }
      return flowUser;
    }
  }


  private FlowUserEntity getOrRegisterUser(UserType userType) {
    UserToken userDetails = usertDetailsService.getUserDetails();
    String email = userDetails.getEmail();
    String firstName = userDetails.getFirstName();
    String lastName = userDetails.getLastName();
    return flowUserService.getOrRegisterUser(email, firstName, lastName, userType);
  }

  @Override
  public FlowUserEntity getUserByID(String userId) {
    if (flowExternalUrlUser.isBlank()) {
      Optional<FlowUserEntity> flowUser = flowUserService.getUserById(userId);
      if (flowUser.isPresent()) {
        return flowUser.get();
      }
    } else {
      UserProfile userProfile = coreUserService.getUserProfileById(userId);
      FlowUserEntity flowUser = new FlowUserEntity();
      if (userProfile != null) {
        BeanUtils.copyProperties(userProfile, flowUser);
        flowUser.setType(userProfile.getType());
        return flowUser;
      }
    }
    return null;
  }

  @Override
  public UserQueryResult getUserViaSearchTerm(String searchTerm, Pageable pageable) {
    final UserQueryResult result = new UserQueryResult();
    final Page<FlowUserEntity> users = flowUserService.findBySearchTerm(searchTerm, pageable);
    final List<FlowUserEntity> userList = new LinkedList<>();
    for (final FlowUserEntity userEntity : users.getContent()) {
      userList.add(userEntity);
    }

    result.setRecords(userList);
    result.setPageable(users);
    return result;
  }

  @Override
  public UserQueryResult getUsers(Pageable pageable) {
    final UserQueryResult result = new UserQueryResult();

    final Page<FlowUserEntity> users = flowUserService.findAll(pageable);
    final List<FlowUserEntity> userList = new LinkedList<>();

    for (final FlowUserEntity userEntity : users.getContent()) {

      userList.add(userEntity);
    }

    result.setRecords(userList);
    result.setPageable(users);
    return result;
  }

  @Override
  public List<FlowUserEntity> getUsersForTeams(List<String> teamIds) {
    return this.flowUserService.getUsersforTeams(teamIds);
  }

  @Override
  public void updateFlowUser(String userId, FlowUser updatedFlowUser) {
    Optional<FlowUserEntity> userOptional = this.flowUserService.getUserById(userId);
    if (userOptional.isPresent()) {
      FlowUserEntity user = userOptional.get();
      user.setType(updatedFlowUser.getType());
      this.flowUserService.save(user);
    }
  }

  @Override
  public ResponseEntity<Boolean> activateSetup(OneTimeCode otc) {
    if (flowExternalUrlUser.isBlank()) {
      if (corePlatformOTC.equals(otc.getOtc())) {
        getOrRegisterUser(UserType.admin);
        return new ResponseEntity<>(HttpStatus.OK);
      }
    }
    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
  }

  @Override
  public UserProfile getOrRegisterCurrentUser() {
    if (flowExternalUrlUser.isBlank()) {
      if (flowUserService.getUserCount() == 0) {
        throw new HttpClientErrorException(HttpStatus.LOCKED);
      }
      FlowUserEntity userEntity = getOrRegisterUser(UserType.user);
      UserProfile profile = new UserProfile();
      BeanUtils.copyProperties(userEntity, profile);
      return profile;
    } else {
      FlowUserEntity userEntity = getCurrentUser();
      UserProfile profile = new UserProfile();
      BeanUtils.copyProperties(userEntity, profile);
      return profile;
    }
  }

  @Override
  public void deleteFlowUser(String userId) {
    if (flowUserService.getUserById(userId).isPresent()) {
      FlowUserEntity user = flowUserService.getUserById(userId).get();
      user.setStatus(UserStatus.deleted);
      flowUserService.save(user);
    }
  }


  @Override
  public FlowUser addFlowUser(FlowUser flowUser) {
    FlowUserEntity flowUserEntity = new FlowUserEntity();
    BeanUtils.copyProperties(flowUser, flowUserEntity);
    flowUser.setStatus(UserStatus.active);
    flowUser.setId(null);
    flowUserEntity = flowUserService.save(flowUser);
    FlowUser newUser = new FlowUser();
    BeanUtils.copyProperties(flowUserEntity, newUser);
    return newUser;
  }

}
