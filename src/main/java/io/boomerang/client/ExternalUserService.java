package io.boomerang.client;

import io.boomerang.client.model.UserProfile;

public interface ExternalUserService {

  public UserProfile getInternalUserProfile();

  public UserProfile getUserProfileById(String id);

}
