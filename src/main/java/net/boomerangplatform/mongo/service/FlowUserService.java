package net.boomerangplatform.mongo.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import net.boomerangplatform.mongo.entity.FlowUserEntity;
import net.boomerangplatform.mongo.model.UserType;

public interface FlowUserService {

  public Long getUserCount();
  
  public FlowUserEntity save(FlowUserEntity user);

  public Optional<FlowUserEntity> getUserById(String id);

  public FlowUserEntity getOrRegisterUser(String userNane, String firstName, String lastName, UserType userType);

  Page<FlowUserEntity> findBySearchTerm(String term, Pageable pageable);

  Page<FlowUserEntity> findAll(Pageable pageable);

  List<FlowUserEntity> getUsersforTeams(List<String> teamIds);

  FlowUserEntity getUserWithEmail(String userEmail);


}
