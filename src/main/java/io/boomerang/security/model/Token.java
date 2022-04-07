package io.boomerang.security.model;

import io.boomerang.mongo.model.TokenScope;

public class Token {
  
  private TokenScope scope;

  public TokenScope getScope() {
    return scope;
  }

  protected void setScope(TokenScope scope) {
    this.scope = scope;
  }

}
