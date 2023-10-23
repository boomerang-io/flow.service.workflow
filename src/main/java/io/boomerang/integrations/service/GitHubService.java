package io.boomerang.integrations.service;

import org.springframework.http.ResponseEntity;
import io.boomerang.integrations.model.GHLinkRequest;

public interface GitHubService {

  ResponseEntity<?> linkAppInstallation(GHLinkRequest request);

  ResponseEntity<?> getInstallation(Integer id);

  void unlinkAppInstallation(GHLinkRequest request);

  ResponseEntity<?> getInstallationForTeam(String team);

}
