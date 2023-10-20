package io.boomerang.integrations.service;

import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import io.boomerang.integrations.data.entity.IntegrationsEntity;
import io.boomerang.integrations.model.Integration;

public interface IntegrationService {

  List<Integration> get(String team);

  IntegrationsEntity create(String type, JsonNode data);

}
