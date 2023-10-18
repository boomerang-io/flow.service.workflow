package io.boomerang.integrations.service;

import org.springframework.http.ResponseEntity;

public interface GitHubService {

  ResponseEntity<?> retrieveAppInstallation(String code);

}
