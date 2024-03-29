package io.boomerang.service;

import java.util.List;
import java.util.Optional;
import io.boomerang.model.Navigation;

public interface NavigationService {

  List<Navigation> getNavigation(boolean isUserAdmin, Optional<String> teamId);

}
