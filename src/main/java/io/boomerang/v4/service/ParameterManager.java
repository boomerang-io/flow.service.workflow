package io.boomerang.v4.service;

import java.util.List;
import io.boomerang.v4.model.ref.ParamLayers;
import io.boomerang.v4.model.ref.RunParam;

public interface ParameterManager {
  
  ParamLayers buildParameterLayering(String wfRunId, List<RunParam> wfRunParams,
      List<RunParam> taskRunParams);

}
