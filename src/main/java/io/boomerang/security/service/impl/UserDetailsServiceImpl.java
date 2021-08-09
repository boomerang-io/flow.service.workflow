package io.boomerang.security.service.impl;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import io.boomerang.mongo.model.TokenScope;
import io.boomerang.security.model.GlobalToken;
import io.boomerang.security.model.TeamToken;
import io.boomerang.security.model.UserToken;
import io.boomerang.security.service.UserDetailsService;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

  @Override
  @NoLogging
  public UserToken getUserDetails() {
    if (SecurityContextHolder.getContext() != null
        && SecurityContextHolder.getContext().getAuthentication() != null
        && SecurityContextHolder.getContext().getAuthentication().getDetails() != null
        && SecurityContextHolder.getContext().getAuthentication()
            .getDetails() instanceof UserToken) {
      return (UserToken) SecurityContextHolder.getContext().getAuthentication().getDetails();
    } else {
      return new UserToken("boomerang@us.ibm.com", "boomerang", "joe");
    }
  }

  
  @Override
  public TokenScope getCurrentScope() {
    if (SecurityContextHolder.getContext() != null
        && SecurityContextHolder.getContext().getAuthentication() != null
        && SecurityContextHolder.getContext().getAuthentication().getDetails() != null) {
      Object details = SecurityContextHolder.getContext().getAuthentication()
          .getDetails();
      if (details instanceof UserToken) {
        return TokenScope.user;
      } else if (details instanceof TeamToken ) {
        return TokenScope.team;
      }
      else if (details instanceof GlobalToken) {
        return TokenScope.global;
      }
    }
    return null;
  }
}
