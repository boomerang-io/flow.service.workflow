package net.boomerangplatform.security.service.impl;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import net.boomerangplatform.security.model.UserDetails;
import net.boomerangplatform.security.service.UserDetailsService;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

  @Override
  @NoLogging
  public UserDetails getUserDetails() {

    if (SecurityContextHolder.getContext() != null
        && SecurityContextHolder.getContext().getAuthentication() != null
        && SecurityContextHolder.getContext().getAuthentication().getDetails() != null
        && SecurityContextHolder.getContext().getAuthentication()
            .getDetails() instanceof UserDetails) {
      return (UserDetails) SecurityContextHolder.getContext().getAuthentication().getDetails();
    } else {
      return new UserDetails("boomerang@us.ibm.com", "boomerang", "joe");
    }
  }
}
