package net.boomerangplatform.mongo.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import net.boomerangplatform.mongo.entity.FlowTaskTemplateEntity;
import net.boomerangplatform.mongo.model.FlowTaskTemplateStatus;

public interface FlowTaskTemplateRepository
    extends MongoRepository<FlowTaskTemplateEntity, String> {

  @Override
  Optional<FlowTaskTemplateEntity> findById(String id);

  @Override
  List<FlowTaskTemplateEntity> findAll();

  List<FlowTaskTemplateEntity> findByStatus(FlowTaskTemplateStatus active);

  FlowTaskTemplateEntity findByIdAndStatus(String id, FlowTaskTemplateStatus active);

}
