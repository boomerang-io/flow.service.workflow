package io.boomerang.security.interceptors;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

import io.boomerang.model.FlowSettings;
import io.boomerang.security.service.UserValidationService;
import io.boomerang.service.crud.ConfigurationService;

@RestController
@RequestMapping("/workflow/settings")
public class ConfigurationController {

  @Autowired
  private ConfigurationService configurationService;
  
  @Autowired
  private UserValidationService userValidationService;

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
	try {
	  userValidationService.validateUserAdminOrOperator();
	} catch (ResponseStatusException e) {
	  throw new HttpClientErrorException(e.getStatus());	
	}
  }

}
