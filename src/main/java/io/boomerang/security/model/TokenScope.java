package io.boomerang.security.model;

public enum TokenScope {

  EVENTS_READ(new TokenType[] {TokenType.global, TokenType.user}, TokenObject.events, TokenAccess.read),
  EVENTS_WRITE(new TokenType[] {TokenType.global}, TokenObject.events, TokenAccess.read),
  TOKEN_READ(new TokenType[] {TokenType.global}, TokenObject.tokens, TokenAccess.read),
  TOKEN_WRITE(new TokenType[] {TokenType.global},TokenObject.tokens, TokenAccess.write);

  private TokenType[] types;

  private TokenObject object;
  private TokenAccess access;

  public TokenType[] types() {
    return types;
  }


  public TokenObject object() {
    return object;
  }

  public TokenAccess access() {
    return access;
  }

  TokenScope(TokenType[] types, TokenObject object, TokenAccess access) {
    this.types = types;
    this.object = object;
    this.access = access;
  }
}


