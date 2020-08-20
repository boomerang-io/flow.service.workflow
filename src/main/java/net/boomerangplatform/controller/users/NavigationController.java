package net.boomerangplatform.controller.users;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import net.boomerangplatform.model.profile.NavigationResponse;
import net.boomerangplatform.mongo.entity.FlowUserEntity;
import net.boomerangplatform.mongo.model.UserType;
import net.boomerangplatform.service.UserIdentityService;
import net.boomerangplatform.service.profile.LaunchpadNavigationService;

@RestController
@RequestMapping("/workflow/users/navigation")
@ConditionalOnProperty(value = "boomerang.standalone", havingValue = "true", matchIfMissing = false)
public class NavigationController {

  @Autowired
  private LaunchpadNavigationService launchpadNavigationService;

  @Autowired
  private UserIdentityService userService;

  @GetMapping(value = "")
  public NavigationResponse getLaunchpadNavigation() {

    boolean isUserAdmin = false;
    final FlowUserEntity userEntity = userService.getCurrentUser();
    if (userEntity != null && (userEntity.getType() == UserType.admin
        || userEntity.getType() == UserType.operator || userEntity.getType() == UserType.auditor
        || userEntity.getType() == UserType.author)) {
      isUserAdmin = true;
    }
    return this.launchpadNavigationService.getLaunchpadNavigation(isUserAdmin);
  }
}
