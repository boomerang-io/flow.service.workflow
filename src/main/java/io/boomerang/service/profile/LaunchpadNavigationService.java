package io.boomerang.service.profile;

import io.boomerang.model.profile.NavigationResponse;

public interface LaunchpadNavigationService {

  NavigationResponse getLaunchpadNavigation(boolean isUserAdmin);
}
