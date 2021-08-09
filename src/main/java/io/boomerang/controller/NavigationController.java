package io.boomerang.controller;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.model.Navigation;
import io.boomerang.mongo.entity.FlowUserEntity;
import io.boomerang.mongo.model.UserType;
import io.boomerang.service.UserIdentityService;
import io.boomerang.service.crud.NavigationService;

@RestController
@RequestMapping("/workflow/navigation")
public class NavigationController {

  @Autowired
  NavigationService navigationService;

  @Autowired
  private UserIdentityService userService;

  @GetMapping(value = "")
  public ResponseEntity<List<Navigation>> getNavigation(@RequestParam(required = false) String teamId) {
    boolean isUserAdmin = false;
    final FlowUserEntity userEntity = userService.getCurrentUser();
    if (userEntity != null
        && (userEntity.getType() == UserType.admin || userEntity.getType() == UserType.operator)) {
      isUserAdmin = true;
    }
    List<Navigation> response = navigationService.getNavigation(isUserAdmin, teamId);
    
    CacheControl cacheControl = CacheControl.maxAge(1, TimeUnit.HOURS);

    return ResponseEntity.ok().cacheControl(cacheControl).body(response);
  }
}
