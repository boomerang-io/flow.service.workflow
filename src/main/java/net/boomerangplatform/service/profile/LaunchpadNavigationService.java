package net.boomerangplatform.service.profile;

import net.boomerangplatform.model.profile.NavigationResponse;

public interface LaunchpadNavigationService {

  NavigationResponse getLaunchpadNavigation(boolean isUserAdmin);
}
