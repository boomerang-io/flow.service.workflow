package io.boomerang.integrations.service;

import java.net.URI;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.spotify.github.v3.checks.Installation;
import com.spotify.github.v3.clients.GitHubClient;
import com.spotify.github.v3.clients.GithubAppClient;
import com.spotify.github.v3.clients.OrganisationClient;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.integrations.data.entity.IntegrationsEntity;
import io.boomerang.integrations.data.repository.IntegrationsRepository;
import io.boomerang.integrations.model.GHInstallationsResponse;
import io.boomerang.integrations.model.GHLinkRequest;
import io.boomerang.service.SettingsService;

@Service
public class GitHubServiceImpl implements GitHubService {

  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  private SettingsService settingsService;
  
  @Autowired
  private IntegrationsRepository integrationsRepository;

  @Override
  public ResponseEntity<?> retrieveAppInstallation(Integer id) {
    final GithubAppClient appClient = getGitHubAppClient(id);
    
//    LOGGER.debug("GitHub Access Token: " + appClient.getAccessToken(Integer.valueOf(code)).join().toString());
    
    List<Installation> installations = appClient.getInstallations().join();
    
    LOGGER.debug("GitHub Installations: " + installations.toString());
    
    if (!installations.isEmpty()) {
      GHInstallationsResponse response = new GHInstallationsResponse(Integer.valueOf(installations.get(0).appId()), Integer.valueOf(installations.get(0).id()), installations.get(0).account().login(), installations.get(0).account().htmlUrl().toString(), Integer.valueOf(installations.get(0).account().id()), installations.get(0).account().type());
      return ResponseEntity.ok(response);
    }
    throw new BoomerangException(BoomerangError.ACTION_INVALID_REF);
  }

  private GithubAppClient getGitHubAppClient(Integer installationId) {
    final String appId = settingsService.getSettingConfig("integration", "github.appId").getValue();
    final GitHubClient githubClient =
        GitHubClient.create(
          URI.create("https://api.github.com/"),
          this.getPEMBytes(),
          Integer.valueOf(appId),
          installationId);
    
    final OrganisationClient orgClient = githubClient.createOrganisationClient("");
    
    final GithubAppClient appClient = orgClient.createGithubAppClient();
    return appClient;
  }  
  
  private byte[] getPEMBytes() {
    final String pem = settingsService.getSettingConfig("integration", "github.pem").getValue();
    LOGGER.debug("getPEMBytes() " + pem);
    
    final String RSA_BEGIN = "-----BEGIN RSA PRIVATE KEY-----";
    final String RSA_END = "-----END RSA PRIVATE KEY-----";

    String middle = pem.replace(RSA_BEGIN, "").replace(RSA_END, "").trim();
    String[] split = middle.split(" ");
    LOGGER.debug(split.length);
    
    StringBuilder builder = new StringBuilder();
    builder.append(RSA_BEGIN).append("\n");
    for (String s : split) {
        builder.append(s).append("\n");
    }
    builder.append(RSA_END);

    return builder.toString().getBytes();
  }

  @Override
  public ResponseEntity<?> linkAppInstallation(GHLinkRequest request) {
    LOGGER.debug("linkAppInstallation() - " + request.toString());
    GithubAppClient appClient = getGitHubAppClient(request.getInstallationId());
    List<Installation> installations = appClient.getInstallations().join();
    IntegrationsEntity entity = new IntegrationsEntity();
    entity.setType("github_app");
    entity.setData((Map<String, Object>) installations.get(0));
    integrationsRepository.save(entity);
    //TODO create Relationship with Team
    return ResponseEntity.ok().build();
  }
}
