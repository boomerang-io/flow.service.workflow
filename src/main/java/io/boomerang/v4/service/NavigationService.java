package io.boomerang.v4.service;

import java.util.List;
import java.util.Optional;
import io.boomerang.v4.model.Navigation;

public interface NavigationService {

  List<Navigation> getNavigation(boolean isUserAdmin, Optional<String> teamId);

}
