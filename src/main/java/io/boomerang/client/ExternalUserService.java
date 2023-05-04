package io.boomerang.client;

public interface ExternalUserService {

  UserProfile getInternalUserProfile(String email);

  public UserProfile getUserProfileById(String id);

}
