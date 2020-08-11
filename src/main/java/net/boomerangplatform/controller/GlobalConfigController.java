package net.boomerangplatform.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import net.boomerangplatform.service.config.model.GlobalConfig;
import net.boomerangplatform.service.crud.GlobalConfigurationService;

@RestController
@RequestMapping("/workflow/config")
public class GlobalConfigController {

  @Autowired
  private GlobalConfigurationService configService;

  @GetMapping
  public List<GlobalConfig> getAllGlobalConfigurations() {
    return configService.getAllGlobalConfigs();
  }

  @DeleteMapping(value = "/{flowConfigurationId}")
  public void deleteConfiguration(@PathVariable String flowConfigurationId) {
    configService.deleteConfiguration(flowConfigurationId);
  }

  @PutMapping(value = "/{flowConfigurationId}")
  public List<GlobalConfig> updateGlobalConfiguration(@RequestBody GlobalConfig newConfigs,
      @PathVariable String flowConfigurationId) {
    return configService.updateGlobalSetting(newConfigs);
  }

  @PostMapping
  public GlobalConfig createNewGlobalConfig(@RequestBody GlobalConfig newConfig) {
    return configService.createNewGlobalConfig(newConfig);
  }

  @PatchMapping(value = "/{flowConfigurationId}")
  public GlobalConfig updateGlobalConfig(@RequestBody GlobalConfig newConfigs,
      @PathVariable String flowConfigurationId) {
    return configService.updateGlobalConfig(newConfigs);
  }

}
