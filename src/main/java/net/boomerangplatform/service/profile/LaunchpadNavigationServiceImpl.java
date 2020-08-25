package net.boomerangplatform.service.profile;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import net.boomerangplatform.model.profile.Features;
import net.boomerangplatform.model.profile.Navigation;
import net.boomerangplatform.model.profile.NavigationResponse;
import net.boomerangplatform.model.profile.Platform;

@Service
public class LaunchpadNavigationServiceImpl implements LaunchpadNavigationService {

  @Value("${core.feature.notifications.enable}")
  private Boolean enableFeatureNotification;

  @Value("${core.feature.docs.enable}")
  private boolean enableDocs;

  @Value("${core.feature.support.enable}")
  private Boolean enableSupport;

  @Value("${core.platform.name}")
  private String platformName;

  @Value("${core.platform.version}")
  private String platformVersion;

  @Value("${boomerang.signOutUrl}")
  private String platformSignOutUrl;

  @Value("${boomerang.baseUrl}")
  private String platformBaseUrl;

  @Override
  public NavigationResponse getLaunchpadNavigation(boolean isUserAdmin) {

    final List<Navigation> navList = new ArrayList<>();
    NavigationResponse navigationResponse = new NavigationResponse();
    navigationResponse.setNavigation(navList);

    Features features = new Features();
    features.setNotificationsEnabled(false);
    features.setDocsEnabled(false);
    features.setSupportEnabled(false);
    navigationResponse.setFeatures(features);

    Platform platform = new Platform();
    platform.setName("Boomerang Flow");
    platform.setVersion("1.0");
    platform.setSignOutUrl(platformBaseUrl + platformSignOutUrl);

    platform.setDisplayLogo(false);
    platform.setPrivateTeams(false);
    platform.setSendMail(false);
    navigationResponse.setPlatform(platform);

    return navigationResponse;
  }
}
