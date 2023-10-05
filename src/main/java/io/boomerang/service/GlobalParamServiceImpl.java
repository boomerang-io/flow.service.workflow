package io.boomerang.service;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
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
    if (!Objects.isNull(param) && param.getKey() != null) {
      Optional<GlobalParamEntity> optParamEntity = paramRepository.findOneByKey(param.getKey());
      if (!optParamEntity.isEmpty()) {    
        // Copy updatedParam to ParamEntity except for ID (requester should not know ID);
        BeanUtils.copyProperties(param, optParamEntity.get(), "id");
        GlobalParamEntity paramEntity = paramRepository.save(optParamEntity.get());
        return new GlobalParam(paramEntity);
      }
    }
    throw new BoomerangException(BoomerangError.PARAMS_INVALID_REFERENCE);
  }

  @Override
  public GlobalParam create(GlobalParam param) {
    if (!Objects.isNull(param) && param.getKey() != null) {

      // Ensure key is unique
      if (paramRepository.countByKey(param.getKey()) > 0) {
        throw new BoomerangException(BoomerangError.PARAMS_NON_UNIQUE_KEY);
      }

      GlobalParamEntity entity = new GlobalParamEntity();
      BeanUtils.copyProperties(param, entity, "id");
      entity = paramRepository.save(entity);
      return new GlobalParam(entity);
    }
    throw new BoomerangException(BoomerangError.PARAMS_INVALID_REFERENCE);
  }

  @Override
  public void delete(String key) {
    if (!Objects.isNull(key) && key != null && paramRepository.countByKey(key) > 0) {
      paramRepository.deleteByKey(key);
    }
    throw new BoomerangException(BoomerangError.PARAMS_INVALID_REFERENCE);
  }
}
