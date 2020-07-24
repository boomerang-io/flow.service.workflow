package net.boomerangplatform.service.crud;

import java.util.LinkedList;
import java.util.List;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import net.boomerangplatform.mongo.entity.FlowGlobalConfigEntity;
import net.boomerangplatform.mongo.service.FlowGlobalConfigService;
import net.boomerangplatform.service.config.model.GlobalConfig;

@Service
public class GlobalConfigurationServiceImpl implements GlobalConfigurationService {

  @Autowired
  private FlowGlobalConfigService configService;

  @Override
  public List<GlobalConfig> getAllGlobalConfigs() {
    List<FlowGlobalConfigEntity> globalConfigEntites = configService.getGlobalConfigs();
    List<GlobalConfig> globalConfigs = new LinkedList<>();
    for (FlowGlobalConfigEntity globalEntity : globalConfigEntites) {
      GlobalConfig newConfig = new GlobalConfig();
      BeanUtils.copyProperties(globalEntity, newConfig);
      globalConfigs.add(newConfig);
    }
    return globalConfigs;
  }

  @Override
  public List<GlobalConfig> updateGlobalSetting(GlobalConfig newConfigs) {

    FlowGlobalConfigEntity entity = this.configService.getGlobalConfig(newConfigs.getId());
    if (entity != null) {
      BeanUtils.copyProperties(newConfigs, entity);
      configService.save(entity);
    }

    return getAllGlobalConfigs();
  }

  @Override
  public GlobalConfig createNewGlobalConfig(GlobalConfig globalConfig) {
    FlowGlobalConfigEntity newEntity = new FlowGlobalConfigEntity();
    BeanUtils.copyProperties(globalConfig, newEntity);
    newEntity = configService.save(newEntity);

    globalConfig.setId(newEntity.getId());

    return globalConfig;
  }

  @Override
  public void deleteConfiguration(String flowConfigurationId) {
    FlowGlobalConfigEntity entity = configService.getGlobalConfig(flowConfigurationId);
    configService.delete(entity);
  }

  @Override
  public GlobalConfig updateGlobalConfig(GlobalConfig newConfigs) {

    FlowGlobalConfigEntity entity = this.configService.getGlobalConfig(newConfigs.getId());
    if (entity != null) {
      BeanUtils.copyProperties(newConfigs, entity);
      entity = configService.save(entity);
    }
    GlobalConfig newConfig = new GlobalConfig();
    if (entity != null) {
      BeanUtils.copyProperties(entity, newConfig);
    }

    return newConfig;
  }
}
