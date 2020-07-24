package net.boomerangplatform.client;

import net.boomerangplatform.client.model.UserProfile;

public interface BoomerangUserService {

  public UserProfile getInternalUserProfile();

  public UserProfile getUserProfileById(String id);

}
