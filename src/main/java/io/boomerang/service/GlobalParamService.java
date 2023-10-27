package io.boomerang.service;

import java.util.List;
import io.boomerang.model.AbstractParam;

public interface GlobalParamService {

  AbstractParam create(AbstractParam globalParam);

  List<AbstractParam> getAll();

  AbstractParam update(AbstractParam params);

  void delete(String key);

}
