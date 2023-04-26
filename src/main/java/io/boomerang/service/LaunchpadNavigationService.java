package io.boomerang.service;

import io.boomerang.model.profile.NavigationResponse;

public interface LaunchpadNavigationService {

  NavigationResponse getLaunchpadNavigation(boolean isUserAdmin);
}
