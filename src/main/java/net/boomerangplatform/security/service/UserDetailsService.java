package net.boomerangplatform.security.service;

import net.boomerangplatform.mongo.model.TokenScope;
import net.boomerangplatform.security.model.UserToken;

public interface UserDetailsService {
  public UserToken getUserDetails();
  public TokenScope getCurrentScope();
}
