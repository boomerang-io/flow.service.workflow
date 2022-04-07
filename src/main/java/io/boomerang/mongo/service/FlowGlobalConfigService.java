package io.boomerang.mongo.service;

import java.util.List;
import io.boomerang.mongo.entity.FlowGlobalConfigEntity;

public interface FlowGlobalConfigService {

  FlowGlobalConfigEntity save(FlowGlobalConfigEntity entity);

  List<FlowGlobalConfigEntity> getGlobalConfigs();

  FlowGlobalConfigEntity getGlobalConfig(String id);

  FlowGlobalConfigEntity update(FlowGlobalConfigEntity entity);

  void delete(FlowGlobalConfigEntity entity);

}
