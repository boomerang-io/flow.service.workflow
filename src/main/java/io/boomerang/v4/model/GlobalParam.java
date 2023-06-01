package io.boomerang.v4.model;

import org.springframework.beans.BeanUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.boomerang.v4.data.entity.GlobalParamEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class GlobalParam extends AbstractParam {
  

  public GlobalParam() {
    
  }

  /*
   * Creates a GlobalParam from GlobalParamwEntity
   */
  public GlobalParam(GlobalParamEntity entity) {
    BeanUtils.copyProperties(entity, this);
  }

}
