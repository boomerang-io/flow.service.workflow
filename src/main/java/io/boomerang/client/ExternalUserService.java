package io.boomerang.client;

public interface ExternalUserService {

  ExternalUserProfile getUserProfileByEmail(String email);

  public ExternalUserProfile getUserProfileById(String id);

}
