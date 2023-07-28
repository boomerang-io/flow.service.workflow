package io.boomerang.security.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.ResponseEntity;
import io.boomerang.security.model.Token;
import io.boomerang.security.model.AuthType;
import io.boomerang.v4.data.entity.UserEntity;
import io.boomerang.v4.model.OneTimeCode;
import io.boomerang.v4.model.Team;
import io.boomerang.v4.model.User;
import io.boomerang.v4.model.UserProfile;
import io.boomerang.v4.model.UserRequest;
import io.boomerang.v4.model.UserResponsePage;
import io.boomerang.v4.model.enums.UserType;
import io.boomerang.v4.model.ref.Workflow;

public interface IdentityService {

  public UserEntity getCurrentUser();

  public Optional<User> getUserByID(String userId);

  public Optional<User> getUserByEmail(String userEmail);
  
  public UserProfile getProfile(String userId);

  public ResponseEntity<Boolean> activateSetup(OneTimeCode otc);

  public AuthType getCurrentScope();
  
  public Token getCurrentIdentity();

  Workflow getCurrentWorkflow();

  Team getCurrentTeam();

  Optional<UserEntity> getAndRegisterUser(String email, String firstName, String lastName,
      Optional<UserType> usertype);

  Page<User> query(Optional<Integer> queryPage, Optional<Integer> queryLimit,
      Optional<Direction> queryOrder, Optional<String> querySort,
      Optional<List<String>> queryLabels, Optional<List<String>> queryStatus,
      Optional<List<String>> queryIds);

  UserProfile getCurrentProfile();

  User create(UserRequest request);

  void apply(UserRequest request);

  public void delete(String userId);

  boolean isCurrentUserAdmin();

  boolean isActivated();

  String getCurrentPrincipal();
}
