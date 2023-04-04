package io.boomerang.security.interceptors;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import io.boomerang.model.FlowSettings;
import io.boomerang.mongo.model.UserType;
import io.boomerang.service.UserIdentityService;
import io.boomerang.service.crud.ConfigurationService;
import io.boomerang.v4.data.entity.UserEntity;

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

    UserEntity userEntity = service.getCurrentUser();
    if (userEntity == null || (!userEntity.getType().equals(UserType.admin)
        && !userEntity.getType().equals(UserType.operator))) {

      throw new HttpClientErrorException(HttpStatus.FORBIDDEN);
    }
  }

}
