package io.boomerang.integrations.service;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.boomerang.integrations.data.entity.IntegrationTemplateEntity;
import io.boomerang.integrations.data.repository.IntegrationTemplateRepository;

@Service
public class IntegrationServiceImpl implements IntegrationService {

  private static final Logger LOGGER = LogManager.getLogger();
  
  @Autowired
  private IntegrationTemplateRepository integrationTemplateRepository;

  @Override
  public List<IntegrationTemplateEntity> get() {
    return integrationTemplateRepository.findAll();
  }
}
