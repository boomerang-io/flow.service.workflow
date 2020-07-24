package net.boomerangplatform.service;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import net.boomerangplatform.client.BoomerangUserService;
import net.boomerangplatform.client.model.UserProfile;
import net.boomerangplatform.model.UserQueryResult;
import net.boomerangplatform.mongo.entity.FlowUserEntity;
import net.boomerangplatform.mongo.model.UserType;
import net.boomerangplatform.mongo.service.FlowUserService;
import net.boomerangplatform.security.model.UserDetails;
import net.boomerangplatform.security.service.UserDetailsService;

@Service
public class UserIdentityServiceImpl implements UserIdentityService {

  @Value("${boomerang.standalone}")
  private boolean standAloneMode;

  @Autowired
  private UserDetailsService usertDetailsService;

  @Autowired
  private BoomerangUserService coreUserService;

  @Autowired
  private FlowUserService flowUserService;

  @Override
  public FlowUserEntity getCurrentUser() {
    if (standAloneMode) {
      UserDetails userDetails = usertDetailsService.getUserDetails();
      String email = userDetails.getEmail();

      String firstName = userDetails.getFirstName();
      String lastName = userDetails.getLastName();
      return flowUserService.getOrRegisterUser(email, firstName, lastName);
    } else {
      UserProfile userProfile = coreUserService.getInternalUserProfile();
      FlowUserEntity flowUser = new FlowUserEntity();
      BeanUtils.copyProperties(userProfile, flowUser);
      flowUser.setType(UserType.valueOf(userProfile.getType()));
      return flowUser;
    }
  }

  @Override
  public FlowUserEntity getUserByID(String userId) {
    if (standAloneMode) {
      Optional<FlowUserEntity> flowUser = flowUserService.getUserById(userId);
      if (flowUser.isPresent()) {
        return flowUser.get();
      }
    } else {
      UserProfile userProfile = coreUserService.getUserProfileById(userId);
      FlowUserEntity flowUser = new FlowUserEntity();
      BeanUtils.copyProperties(userProfile, flowUser);
      flowUser.setType(UserType.valueOf(userProfile.getType()));
      return flowUser;
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
}
