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
import io.boomerang.model.enums.RelationshipLabel;
import io.boomerang.model.enums.RelationshipType;
import io.boomerang.service.RelationshipServiceImpl;
import io.boomerang.service.SettingsService;

@Service
public class IntegrationServiceImpl implements IntegrationService {

  private static final Logger LOGGER = LogManager.getLogger();
  
  @Autowired
  private IntegrationTemplateRepository integrationTemplateRepository;
  
  @Autowired
  private IntegrationsRepository integrationsRepository;
  
  @Autowired
  private RelationshipServiceImpl relationshipServiceImpl;
  
  @Autowired
  private SettingsService settingsService;

  @Override
  public List<Integration> get(String team) {
    List<IntegrationTemplateEntity> templates = integrationTemplateRepository.findAll();
    List<Integration> integrations = new LinkedList<>();
    templates.forEach(t -> {
      LOGGER.debug(t.toString());
      Integration i = new Integration();
      BeanUtils.copyProperties(t, i);
      List<String> refs = relationshipServiceImpl.getFilteredRefs(Optional.of(RelationshipType.INTEGRATION),
          Optional.empty(), RelationshipLabel.BELONGSTO,
          RelationshipType.TEAM, team, false);
      LOGGER.debug("Refs: " + refs.toString());
      if (!refs.isEmpty()) {
        i.setRef(refs.get(0));
        Optional<IntegrationsEntity> entity = integrationsRepository.findByIdAndType(refs.get(0), t.getType());
        if (entity.isPresent()) {
          i.setStatus(IntegrationStatus.linked);          
        }
      }
      if ("github".equals(i.getName().toLowerCase())) {
        LOGGER.debug(settingsService.getSettingConfig("integration", "github.appName").getValue());
        i.setLink(i.getLink().replace("{app_name}", settingsService.getSettingConfig("integration", "github.appName").getValue()));
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
      String team = relationshipServiceImpl.getTeamSlugFromChild(RelationshipType.INTEGRATION, optEntity.get().getId());
      LOGGER.debug("Team Ref: " + team);
      if (!team.isBlank()) {        
        return team;
      }
    }
    return null;
  }
  
  @Override
  public IntegrationsEntity create(String type, JsonNode data) {
    IntegrationsEntity entity = new IntegrationsEntity();
    entity.setType(type);
    entity.setRef(data.get("id").asText());
    entity.setData(Document.parse(data.toString()));
    entity = integrationsRepository.save(entity);
    
    relationshipServiceImpl.createNode(RelationshipType.INTEGRATION, entity.getId(), "", Optional.empty());
    
    return entity;
  }
  
  @Override
  public void delete(String type, JsonNode data) {
    Optional<IntegrationsEntity> optEntity =
        integrationsRepository.findByRef(data.get("id").asText());
    if (optEntity.isPresent()) {
      IntegrationsEntity entity = optEntity.get();
      integrationsRepository.delete(optEntity.get());
      relationshipServiceImpl.removeNodeByRefOrSlug(RelationshipType.INTEGRATION, entity.getId());
    }
  }
}
