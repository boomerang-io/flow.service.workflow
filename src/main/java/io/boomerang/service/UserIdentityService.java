package io.boomerang.service;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import io.boomerang.client.model.UserProfile;
import io.boomerang.model.FlowUser;
import io.boomerang.model.FlowUserProfile;
import io.boomerang.model.OneTimeCode;
import io.boomerang.model.UserQueryResult;
import io.boomerang.mongo.entity.FlowUserEntity;
import io.boomerang.mongo.model.TokenScope;
import io.boomerang.security.model.Token;
import io.boomerang.security.model.UserToken;

public interface UserIdentityService {

  public UserProfile getOrRegisterCurrentUser();

  public FlowUserEntity getCurrentUser();

  public FlowUserEntity getUserByID(String userId);
  public FlowUserProfile getFullUserProfile(String userId);

  UserQueryResult getUserViaSearchTerm(String searchTerm, Pageable pageable);

  UserQueryResult getUsers(Pageable pageable);

  List<FlowUserEntity> getUsersForTeams(List<String> teamIds);

  public void updateFlowUser(String userId, FlowUser flowUser);

  public ResponseEntity<Boolean> activateSetup(OneTimeCode otc);

  public void deleteFlowUser(String userId);

  public FlowUser addFlowUser(FlowUser flowUser);

  public UserToken getUserDetails();

  public TokenScope getCurrentScope();
  
  public Token getRequestIdentity();

  FlowUserEntity getUserByEmail(String userEmail);

}
