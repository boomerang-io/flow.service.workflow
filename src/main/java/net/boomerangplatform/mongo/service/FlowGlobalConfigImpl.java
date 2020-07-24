package net.boomerangplatform.mongo.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import net.boomerangplatform.mongo.entity.FlowGlobalConfigEntity;
import net.boomerangplatform.mongo.repository.FlowGlobalConfigRepository;

@Service
public class FlowGlobalConfigImpl implements FlowGlobalConfigService {

  @Autowired
  private FlowGlobalConfigRepository repository;

  @Override
  public FlowGlobalConfigEntity save(FlowGlobalConfigEntity entity) {
    return repository.save(entity);
  }

  @Override
  public List<FlowGlobalConfigEntity> getGlobalConfigs() {
    return repository.findAll();
  }

  @Override
  public FlowGlobalConfigEntity getGlobalConfig(String id) {
    return repository.findById(id).orElse(null);
  }

  @Override
  public FlowGlobalConfigEntity update(FlowGlobalConfigEntity entity) {
    return repository.save(entity);
  }

  @Override
  public void delete(FlowGlobalConfigEntity entity) {
    repository.delete(entity);
  }

}
