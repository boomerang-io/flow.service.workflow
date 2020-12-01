package net.boomerangplatform.controller;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import net.boomerangplatform.model.Navigation;
import net.boomerangplatform.mongo.entity.FlowUserEntity;
import net.boomerangplatform.mongo.model.UserType;
import net.boomerangplatform.service.UserIdentityService;
import net.boomerangplatform.service.crud.NavigationService;

@RestController
@RequestMapping("/workflow/navigation")
public class NavigationController {

  @Autowired
  NavigationService navigationService;

  @Autowired
  private UserIdentityService userService;

  @GetMapping(value = "")
  ResponseEntity<List<Navigation>> getNavigation() {
    boolean isUserAdmin = false;
    final FlowUserEntity userEntity = userService.getCurrentUser();
    if (userEntity != null
        && (userEntity.getType() == UserType.admin || userEntity.getType() == UserType.operator)) {
      isUserAdmin = true;
    }
    List<Navigation> response = navigationService.getNavigation(isUserAdmin);
    
    CacheControl cacheControl = CacheControl.maxAge(1, TimeUnit.HOURS);

    return ResponseEntity.ok().cacheControl(cacheControl).body(response);


  }

}
