package io.boomerang.service;

import java.util.List;
import io.boomerang.model.ref.ParamLayers;
import io.boomerang.model.ref.ParamSpec;
import io.boomerang.model.ref.Workflow;

public interface ParameterManager {

  List<String> buildParamKeys(String teamId, Workflow workflow);

  ParamLayers buildParamLayers(String teamId, Workflow workflow);

}
