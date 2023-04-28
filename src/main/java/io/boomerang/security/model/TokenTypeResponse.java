package io.boomerang.security.model;

public class TokenTypeResponse {

  private TokenObject object;
  private TokenAccess access;
  private TokenType[] types;

  public TokenAccess getAccess() {
    return access;
  }
  public void setAccess(TokenAccess access) {
    this.access = access;
  }
  public TokenType[] getTypes() {
    return types;
  }
  public void setTypes(TokenType[] types) {
    this.types = types;
  }
  public TokenObject getObject() {
    return object;
  }
  public void setObject(TokenObject object) {
    this.object = object;
  }


}
