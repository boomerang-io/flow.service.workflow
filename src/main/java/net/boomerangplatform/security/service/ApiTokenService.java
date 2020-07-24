package net.boomerangplatform.security.service;

public interface ApiTokenService {
  String getToken(boolean encoded);

  String createJWTToken();

  void storeUserToken(String token);

  String getUserToken();

  String createJWTToken(String email);
}
