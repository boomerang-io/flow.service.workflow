package net.boomerangplatform.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import net.boomerangplatform.model.FlowSettings;
import net.boomerangplatform.service.crud.ConfigurationService;

@RestController
@RequestMapping("/workflow/settings")
public class ConfigurationController {

  @Autowired
  private ConfigurationService configurationService;

  @GetMapping(value = "")
  public List<FlowSettings> getAppConfiguration() {
    return configurationService.getAllSettings();
  }

  @PutMapping(value = "")
  public List<FlowSettings> updateSettings(@RequestBody List<FlowSettings> settings) {
    return configurationService.updateSettings(settings);
  }
}
