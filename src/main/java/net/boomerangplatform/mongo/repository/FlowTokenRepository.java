package net.boomerangplatform.mongo.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import net.boomerangplatform.mongo.entity.TokenEntity;

public interface FlowTokenRepository extends MongoRepository<TokenEntity, String> {

}
