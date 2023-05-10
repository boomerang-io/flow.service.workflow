package io.boomerang.security.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.ResponseEntity;
import io.boomerang.security.model.Token;
import io.boomerang.security.model.TokenType;
import io.boomerang.v4.data.entity.TeamEntity;
import io.boomerang.v4.data.entity.UserEntity;
import io.boomerang.v4.data.entity.ref.WorkflowEntity;
import io.boomerang.v4.model.OneTimeCode;
import io.boomerang.v4.model.User;
import io.boomerang.v4.model.UserProfile;
import io.boomerang.v4.model.UserRequest;
import io.boomerang.v4.model.UserResponsePage;
import io.boomerang.v4.model.UserType;

public interface IdentityService {

  public UserEntity getCurrentUser();

  public Optional<User> getUserByID(String userId);

  public Optional<User> getUserByEmail(String userEmail);
  
  public UserProfile getProfile(String userId);

  public ResponseEntity<Boolean> activateSetup(OneTimeCode otc);

  public TokenType getCurrentScope();
  
  public Token getCurrentIdentity();

  WorkflowEntity getCurrentWorkflow();

  TeamEntity getCurrentTeam();

  Optional<UserEntity> getAndRegisterUser(String email, String firstName, String lastName,
      Optional<UserType> usertype);

  public UserResponsePage query(Optional<Integer> page, Optional<Integer> limit, Optional<Direction> sort,
      Optional<List<String>> labels, Optional<List<String>> status, Optional<List<String>> ids);

  UserProfile getCurrentProfile();

  User create(UserRequest request);

  void apply(UserRequest request);

  public void delete(String userId);

  boolean isCurrentUserAdmin();

  boolean isActivated();
}
