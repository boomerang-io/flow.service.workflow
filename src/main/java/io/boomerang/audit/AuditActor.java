package io.boomerang.audit;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.boomerang.security.model.AuthType;
import io.boomerang.security.model.Token;

@JsonInclude(Include.NON_NULL)
public class AuditActor {
  
  private String principal;
  private AuthType type;
  private String tokenRef;
  
  public AuditActor() {
    // TODO Auto-generated constructor stub
  }
  
  public AuditActor(Token token) {
    this.principal = token.getPrincipal();
    this.type = token.getType();
    this.tokenRef = token.getId();
  }
  
  public String getPrincipal() {
    return principal;
  }
  public void setPrincipal(String principal) {
    this.principal = principal;
  }
  public AuthType getType() {
    return type;
  }
  public void setType(AuthType type) {
    this.type = type;
  }
  public String getTokenRef() {
    return tokenRef;
  }
  public void setTokenRef(String tokenRef) {
    this.tokenRef = tokenRef;
  }
}
