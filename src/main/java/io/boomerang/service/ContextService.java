package io.boomerang.service;

import io.boomerang.model.HeaderNavigationResponse;

public interface ContextService {

  HeaderNavigationResponse getHeaderNavigation(boolean isUserAdmin);
}
