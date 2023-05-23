package io.boomerang.service;

import java.util.List;
import io.boomerang.v4.model.ref.ParamLayers;
import io.boomerang.v4.model.ref.ParamSpec;

public interface ParameterManager {

  List<String> buildParamKeys(String teamId, List<ParamSpec> workflowParamSpecs);

  ParamLayers buildParamLayers(String teamId);

}
