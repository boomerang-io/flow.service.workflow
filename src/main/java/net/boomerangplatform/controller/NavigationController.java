package net.boomerangplatform.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import net.boomerangplatform.model.Navigation;
import net.boomerangplatform.service.crud.NavigationService;

@RestController
@RequestMapping("/workflow/navigation")
public class NavigationController {
  
  @Autowired
  NavigationService navigationService;
  
  @GetMapping(value="")
  List<Navigation> getNavigation(@RequestParam String teamId) {
     return navigationService.getNavigation(teamId);
  }

}
