package io.boomerang.v4.service;

import io.boomerang.v4.model.HeaderNavigationResponse;

public interface HeaderNavigationService {

  HeaderNavigationResponse getHeaderNavigation(boolean isUserAdmin);
}
