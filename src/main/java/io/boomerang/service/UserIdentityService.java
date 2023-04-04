package io.boomerang.service;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import io.boomerang.client.model.UserProfile;
import io.boomerang.model.FlowUserProfile;
import io.boomerang.model.OneTimeCode;
import io.boomerang.model.UserQueryResult;
import io.boomerang.mongo.model.TokenScope;
import io.boomerang.security.model.Token;
import io.boomerang.security.model.UserToken;
import io.boomerang.v4.data.entity.UserEntity;
import io.boomerang.v4.model.User;

public interface UserIdentityService {

  public UserProfile getOrRegisterCurrentUser();

  public UserEntity getCurrentUser();

  public UserEntity getUserByID(String userId);
  public FlowUserProfile getFullUserProfile(String userId);

  UserQueryResult getUserViaSearchTerm(String searchTerm, Pageable pageable);

  UserQueryResult getUsers(Pageable pageable);

  List<UserEntity> getUsersForTeams(List<String> teamIds);

  public void updateFlowUser(String userId, User flowUser);

  public ResponseEntity<Boolean> activateSetup(OneTimeCode otc);

  public void deleteFlowUser(String userId);

  public User addFlowUser(User flowUser);

  public UserToken getUserDetails();

  public TokenScope getCurrentScope();
  
  public Token getRequestIdentity();

  UserEntity getUserByEmail(String userEmail);

}
