package io.boomerang.security.model;

import java.util.Collection;
import java.util.LinkedList;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class FlowAuthenticationToken extends AbstractAuthenticationToken {

  public FlowAuthenticationToken(Collection<? extends GrantedAuthority> authorities) {
    super(authorities);
    // TODO Auto-generated constructor stub
  }

  @Override
  public Object getCredentials() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Object getPrincipal() {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public boolean isAuthenticated() {
    return true;
  }

}
