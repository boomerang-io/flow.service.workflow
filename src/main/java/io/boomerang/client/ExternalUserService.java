package io.boomerang.client;

public interface ExternalUserService {

  public UserProfile getInternalUserProfile();

  public UserProfile getUserProfileById(String id);

}
