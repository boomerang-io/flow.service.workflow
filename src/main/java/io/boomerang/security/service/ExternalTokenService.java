package io.boomerang.security.service;

public interface ExternalTokenService {
  String getToken(boolean encoded);

  String createJWTToken();

  void storeUserToken(String token);

  String getUserToken();

  String createJWTToken(String email);
}
