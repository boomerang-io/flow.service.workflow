package io.boomerang.controller.users;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.model.profile.NavigationResponse;
import io.boomerang.mongo.model.UserType;
import io.boomerang.service.UserIdentityService;
import io.boomerang.service.profile.LaunchpadNavigationService;
import io.boomerang.v4.data.entity.UserEntity;

@RestController
@RequestMapping("/workflow/users/navigation")
public class PlatformNavigationController {

  @Autowired
  private LaunchpadNavigationService launchpadNavigationService;

  @Autowired
  private UserIdentityService userService;

  @GetMapping(value = "")
  public NavigationResponse getLaunchpadNavigation() {

    boolean isUserAdmin = false;
    final UserEntity userEntity = userService.getCurrentUser();
    if (userEntity != null && (userEntity.getType() == UserType.admin
        || userEntity.getType() == UserType.operator || userEntity.getType() == UserType.auditor
        || userEntity.getType() == UserType.author)) {
      isUserAdmin = true;
    }
    return this.launchpadNavigationService.getLaunchpadNavigation(isUserAdmin);
  }
}
