package io.boomerang.mongo.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import io.boomerang.mongo.entity.FlowUserEntity;
import io.boomerang.mongo.model.UserType;

public interface FlowUserService {

  public Long getUserCount();

  public FlowUserEntity save(FlowUserEntity user);

  public Optional<FlowUserEntity> getUserById(String id);

  public FlowUserEntity getOrRegisterUser(String email, String name,
      UserType userType);

  Page<FlowUserEntity> findBySearchTerm(String term, Pageable pageable);

  Page<FlowUserEntity> findAll(Pageable pageable);

  List<FlowUserEntity> getUsersforTeams(List<String> teamIds);

  FlowUserEntity getUserWithEmail(String userEmail);

  FlowUserEntity registerUser(FlowUserEntity user);


}
