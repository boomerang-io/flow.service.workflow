package io.boomerang.integrations.service;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import io.boomerang.integrations.data.entity.IntegrationTemplateEntity;
import io.boomerang.integrations.data.entity.IntegrationsEntity;
import io.boomerang.integrations.data.repository.IntegrationTemplateRepository;
import io.boomerang.integrations.data.repository.IntegrationsRepository;
import io.boomerang.integrations.model.Integration;
import io.boomerang.integrations.model.enums.IntegrationStatus;
import io.boomerang.model.enums.RelationshipRef;
import io.boomerang.model.enums.RelationshipType;
import io.boomerang.service.RelationshipService;

@Service
public class IntegrationServiceImpl implements IntegrationService {

  private static final Logger LOGGER = LogManager.getLogger();
  
  @Autowired
  private IntegrationTemplateRepository integrationTemplateRepository;
  
  @Autowired
  private IntegrationsRepository integrationsRepository;
  
  @Autowired
  private RelationshipService relationshipService;

  @Override
  public List<Integration> get(String team) {
    List<IntegrationTemplateEntity> templates = integrationTemplateRepository.findAll();
    List<Integration> integrations = new LinkedList<>();
    templates.forEach(t -> {
      Integration i = new Integration();
      BeanUtils.copyProperties(t, i);
      List<String> refs = relationshipService.getFilteredFromRefs(Optional.of(RelationshipRef.INTEGRATION),
          Optional.empty(), Optional.of(RelationshipType.BELONGSTO),
          Optional.of(RelationshipRef.TEAM), Optional.of(List.of(team)));
      if (!refs.isEmpty()) {
        Optional<IntegrationsEntity> entity = integrationsRepository.findByIdAndType(refs.get(0), t.getName());
        if (entity.isPresent()) {
          i.setStatus(IntegrationStatus.linked);          
          i.setRef(refs.get(0));
        }
      }
      integrations.add(i);
    });
    return integrations;
  }
  
  @Override
  public IntegrationsEntity create(String type, JsonNode data) {
    IntegrationsEntity entity = new IntegrationsEntity();
    entity.setType(type);
    entity.setRef(data.get("id").toString());
    entity.setData(data);
    return integrationsRepository.save(entity);
  }
}
