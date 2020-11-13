package net.boomerangplatform.service.crud;

import java.util.List;
import net.boomerangplatform.model.Navigation;

public interface NavigationService {

  List<Navigation> getNavigation(boolean isUserAdmin);

}
