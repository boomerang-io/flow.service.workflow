package io.boomerang.service;

import java.util.List;
import io.boomerang.model.GlobalParam;

public interface GlobalParamService {

  GlobalParam create(GlobalParam globalParam);

  List<GlobalParam> getAll();

  GlobalParam update(GlobalParam params);

  void delete(String key);

}
