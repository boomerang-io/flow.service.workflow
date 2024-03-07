package io.boomerang.integrations.service;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
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
import io.boomerang.model.enums.RelationshipNodeType;
import io.boomerang.model.enums.RelationshipLabel;
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
      List<String> refs = relationshipService.getFilteredFromRefs(Optional.of(RelationshipNodeType.INTEGRATION),
          Optional.empty(), Optional.of(RelationshipLabel.BELONGSTO),
          Optional.of(RelationshipNodeType.TEAM), Optional.of(List.of(team)));
      LOGGER.debug("Refs: " + refs.toString());
      if (!refs.isEmpty()) {
        i.setRef(refs.get(0));
        Optional<IntegrationsEntity> entity = integrationsRepository.findByIdAndType(refs.get(0), t.getType());
        if (entity.isPresent()) {
          i.setStatus(IntegrationStatus.linked);          
        }
      }
      integrations.add(i);
    });
    return integrations;
  }
  
  @Override
  public String getTeamByRef(String ref) {
    Optional<IntegrationsEntity> optEntity = integrationsRepository.findByRef(ref);
    if (optEntity.isPresent()) {
      LOGGER.debug("Integration Entity ID: " + optEntity.get().getId());
      List<String> refs = relationshipService.getFilteredToRefs(Optional.of(RelationshipNodeType.INTEGRATION),
          Optional.of(List.of(optEntity.get().getId())), Optional.of(RelationshipLabel.BELONGSTO),
          Optional.of(RelationshipNodeType.TEAM), Optional.empty());
      LOGGER.debug("Team Refs: " + refs.toString());
      return refs.get(0);
    }
    return null;
  }
  
  @Override
  public IntegrationsEntity create(String type, JsonNode data) {
    IntegrationsEntity entity = new IntegrationsEntity();
    entity.setType(type);
    entity.setRef(data.get("id").asText());
    entity.setData(Document.parse(data.toString()));
    return integrationsRepository.save(entity);
  }
  
  @Override
  public void delete(String type, JsonNode data) {
    Optional<IntegrationsEntity> optEntity =
        integrationsRepository.findByRef(data.get("id").asText());
    if (optEntity.isPresent()) {
      IntegrationsEntity entity = optEntity.get();
      integrationsRepository.delete(optEntity.get());
      List<String> rels =
          relationshipService.getFilteredToRefs(Optional.of(RelationshipNodeType.INTEGRATION),
              Optional.of(List.of(entity.getId())), Optional.of(RelationshipLabel.BELONGSTO),
              Optional.of(RelationshipNodeType.TEAM), Optional.empty());
      rels.forEach(r -> relationshipService.removeRelationshipById(r));
    }
  }
}
