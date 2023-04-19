package io.boomerang.v4.service;

import java.util.List;
import io.boomerang.v4.model.ref.ParamLayers;
import io.boomerang.v4.model.ref.ParamSpec;

public interface ParameterManager {

  ParamLayers buildParamLayers(String wfRunId);

  List<String> buildParamKeys(String teamId, List<ParamSpec> workflowParamSpecs);

}
