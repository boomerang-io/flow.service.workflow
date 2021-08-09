package io.boomerang.security.service;

import io.boomerang.mongo.model.TokenScope;
import io.boomerang.security.model.UserToken;

public interface UserDetailsService {
  public UserToken getUserDetails();
  public TokenScope getCurrentScope();
}
