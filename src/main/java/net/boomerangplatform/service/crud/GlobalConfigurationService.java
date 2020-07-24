package net.boomerangplatform.service.crud;

import java.util.List;
import net.boomerangplatform.service.config.model.GlobalConfig;

public interface GlobalConfigurationService {

  GlobalConfig createNewGlobalConfig(GlobalConfig globalConfig);

  List<GlobalConfig> getAllGlobalConfigs();

  List<GlobalConfig> updateGlobalSetting(GlobalConfig newConfigs);

  void deleteConfiguration(String flowConfigurationId);

  GlobalConfig updateGlobalConfig(GlobalConfig newConfigs);

}
