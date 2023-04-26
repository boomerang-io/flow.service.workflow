package io.boomerang.service;

import java.util.List;
import io.boomerang.model.Navigation;

public interface NavigationService {

  List<Navigation> getNavigation(boolean isUserAdmin, String teamId);

}
