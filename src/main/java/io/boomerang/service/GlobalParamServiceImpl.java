package io.boomerang.service;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.boomerang.data.entity.GlobalParamEntity;
import io.boomerang.data.repository.GlobalParamRepository;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.model.GlobalParam;

/*
 * CRUD for Global Params
 * 
 * TODO: validate no need to hide sensitive params as only Admins can access
 * 
 * TODO: check if this is feature gated in settings
 */
@Service
public class GlobalParamServiceImpl implements GlobalParamService {

  @Autowired
  private GlobalParamRepository paramRepository;

  @Override
  public List<GlobalParam> getAll() {
    List<GlobalParamEntity> paramEntites = paramRepository.findAll();
    List<GlobalParam> params = new LinkedList<>();
    for (GlobalParamEntity entity : paramEntites) {
      GlobalParam param = new GlobalParam(entity);
      params.add(param);
    }
    return params;
  }

  @Override
  public GlobalParam update(GlobalParam param) {
    if (param.getKey() != null) {
      //TODO Better exception related to Params
      throw new BoomerangException(BoomerangError.REQUEST_INVALID_PARAMS);
    }
    Optional<GlobalParamEntity> optParamEntity = paramRepository.findOneByKey(param.getKey());
    if (optParamEntity.isEmpty()) {
      //TODO Better exception related to Params
      throw new BoomerangException(BoomerangError.REQUEST_INVALID_PARAMS);
    }
    
    // Copy updatedParam to ParamEntity except for ID (requester should not know ID);
    BeanUtils.copyProperties(param, optParamEntity.get(), "id");
    GlobalParamEntity paramEntity = paramRepository.save(optParamEntity.get());

    return new GlobalParam(paramEntity);
  }

  @Override
  public GlobalParam create(GlobalParam param) {
    //Check mandatory elements
    if (param.getKey() == null || param.getKey().isEmpty()) {
      //TODO Better exception related to Params
      throw new BoomerangException(BoomerangError.REQUEST_INVALID_PARAMS);
    }
    
    // Ensure key is unique
    if (paramRepository.countByKey(param.getKey()) > 0) {
      //TODO Better exception related to Params
      throw new BoomerangException(BoomerangError.REQUEST_INVALID_PARAMS);
    }
    
    GlobalParamEntity entity = new GlobalParamEntity();
    BeanUtils.copyProperties(param, entity, "id");
    entity = paramRepository.save(entity);

    return new GlobalParam(entity);
  }

  @Override
  public void delete(String key) {
    // Ensure key exists
    if (paramRepository.countByKey(key) > 0) {
      //TODO Better exception related to Params
      throw new BoomerangException(BoomerangError.REQUEST_INVALID_PARAMS);
    }
    
    paramRepository.deleteByKey(key);
  }
}
