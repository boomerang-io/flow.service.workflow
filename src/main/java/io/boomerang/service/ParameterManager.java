package io.boomerang.service;

import java.util.List;
import io.boomerang.model.ref.ParamLayers;
import io.boomerang.model.ref.ParamSpec;
import io.boomerang.model.ref.Workflow;

public interface ParameterManager {

  List<String> buildParamKeys(String teamId, Workflow workflow, List<ParamSpec> workflowParamSpecs);

  ParamLayers buildParamLayers(String teamId, Workflow workflow);

}
