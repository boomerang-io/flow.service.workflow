package io.boomerang.service;

import java.util.List;
import io.boomerang.model.ref.ParamLayers;
import io.boomerang.model.ref.ParamSpec;

public interface ParameterManager {

  ParamLayers buildParamLayers(String teamId);

  List<String> buildParamKeys(String teamId, String workflowId, List<ParamSpec> workflowParamSpecs);

}
