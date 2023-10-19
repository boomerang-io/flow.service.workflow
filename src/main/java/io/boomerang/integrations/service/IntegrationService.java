package io.boomerang.integrations.service;

import java.util.List;
import io.boomerang.integrations.data.entity.IntegrationTemplateEntity;

public interface IntegrationService {

  List<IntegrationTemplateEntity> get(String team);

}
