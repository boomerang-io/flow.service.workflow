package io.boomerang.v4.service;

import java.util.List;
import io.boomerang.v4.model.Navigation;

public interface MenuNavigationService {

  List<Navigation> getNavigation(boolean isUserAdmin, String teamId);

}
