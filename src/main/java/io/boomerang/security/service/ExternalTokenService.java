package io.boomerang.security.service;

public interface ExternalTokenService {
  String createJWTToken();
  String createJWTToken(String email);
}
