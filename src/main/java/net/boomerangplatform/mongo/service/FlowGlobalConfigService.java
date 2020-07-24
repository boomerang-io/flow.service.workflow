package net.boomerangplatform.mongo.service;

import java.util.List;
import net.boomerangplatform.mongo.entity.FlowGlobalConfigEntity;

public interface FlowGlobalConfigService {

  FlowGlobalConfigEntity save(FlowGlobalConfigEntity entity);

  List<FlowGlobalConfigEntity> getGlobalConfigs();

  FlowGlobalConfigEntity getGlobalConfig(String id);

  FlowGlobalConfigEntity update(FlowGlobalConfigEntity entity);

  void delete(FlowGlobalConfigEntity entity);

}
