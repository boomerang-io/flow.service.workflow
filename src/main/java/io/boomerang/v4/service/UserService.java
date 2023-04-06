package io.boomerang.v4.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import io.boomerang.mongo.model.UserType;
import io.boomerang.v4.data.entity.UserEntity;

public interface UserService {

  public Long getUserCount();

  public UserEntity save(UserEntity user);

  public Optional<UserEntity> getUserById(String id);

  public UserEntity getOrRegisterUser(String email, String name,
      UserType userType);

  Page<UserEntity> findBySearchTerm(String term, Pageable pageable);

  Page<UserEntity> findAll(Pageable pageable);

  List<UserEntity> getUsersforTeams(List<String> teamIds);

  UserEntity getUserWithEmail(String userEmail);

  UserEntity registerUser(UserEntity user);


}
