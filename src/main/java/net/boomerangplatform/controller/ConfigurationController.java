package net.boomerangplatform.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import net.boomerangplatform.model.FlowSettings;
import net.boomerangplatform.mongo.entity.FlowUserEntity;
import net.boomerangplatform.mongo.model.UserType;
import net.boomerangplatform.service.UserIdentityService;
import net.boomerangplatform.service.crud.ConfigurationService;

@RestController
@RequestMapping("/workflow/settings")
public class ConfigurationController {

  @Autowired
  private ConfigurationService configurationService;
  
  @Autowired
  UserIdentityService service;

  @GetMapping(value = "")
  public List<FlowSettings> getAppConfiguration() {
    validateUser();
    return configurationService.getAllSettings();
  }

  @PutMapping(value = "")
  public List<FlowSettings> updateSettings(@RequestBody List<FlowSettings> settings) {
    validateUser();
    return configurationService.updateSettings(settings);
  }
  
  protected void validateUser() {

    FlowUserEntity userEntity = service.getCurrentUser();
    if (userEntity == null || (!userEntity.getType().equals(UserType.admin)
        && !userEntity.getType().equals(UserType.operator))) {

      throw new HttpClientErrorException(HttpStatus.FORBIDDEN);
    }
  }

}
