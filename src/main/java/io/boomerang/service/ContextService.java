package io.boomerang.service;

import io.boomerang.v4.model.HeaderNavigationResponse;

public interface ContextService {

  HeaderNavigationResponse getHeaderNavigation(boolean isUserAdmin);
}
