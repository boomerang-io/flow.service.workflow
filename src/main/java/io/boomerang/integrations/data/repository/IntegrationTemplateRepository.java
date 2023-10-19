package io.boomerang.integrations.data.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.integrations.data.entity.IntegrationTemplateEntity;

public interface IntegrationTemplateRepository extends MongoRepository<IntegrationTemplateEntity, String> {

}

