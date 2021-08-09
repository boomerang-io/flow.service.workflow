package io.boomerang.client;

import io.boomerang.client.model.UserProfile;

public interface ExernalUserService {

  public UserProfile getInternalUserProfile();

  public UserProfile getUserProfileById(String id);

}
