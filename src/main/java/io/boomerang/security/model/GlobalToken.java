package io.boomerang.security.model;

import io.boomerang.mongo.model.TokenScope;

public class GlobalToken extends Token {

  public GlobalToken() {
    this.setScope(TokenScope.global);
  }
}
