package net.boomerangplatform.client;

import net.boomerangplatform.client.model.UserProfile;

public interface ExernalUserService {

  public UserProfile getInternalUserProfile();

  public UserProfile getUserProfileById(String id);

}
