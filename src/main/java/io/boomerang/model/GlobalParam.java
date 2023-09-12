package io.boomerang.model;

import org.springframework.beans.BeanUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.boomerang.data.entity.GlobalParamEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class GlobalParam extends GlobalParamEntity {
  

  public GlobalParam() {
    
  }

  /*
   * Creates a GlobalParam from GlobalParamEntity
   * 
   * Ignore id as the end user does not need to use it
   */
  public GlobalParam(GlobalParamEntity entity) {
    BeanUtils.copyProperties(entity, this, "id");
  }

}
