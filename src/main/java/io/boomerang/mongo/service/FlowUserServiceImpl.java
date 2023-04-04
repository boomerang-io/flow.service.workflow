package io.boomerang.mongo.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import io.boomerang.mongo.model.UserStatus;
import io.boomerang.mongo.model.UserType;
import io.boomerang.v4.data.entity.UserEntity;
import io.boomerang.v4.data.repository.UserRepository;

@Service
public class FlowUserServiceImpl implements FlowUserService {

  @Autowired
  private UserRepository flowUserRepository;

  @Override
  public UserEntity getOrRegisterUser(String email, String name, UserType usertype) {

    Long count = this.flowUserRepository.countByEmailIgnoreCase(email);
    if (count == 1) {
      return this.flowUserRepository.findByEmailIgnoreCase(email);
    }

    UserEntity userEntity = new UserEntity();
    userEntity.setEmail(email);
    userEntity.setName(name);
    userEntity.setFirstLoginDate(new Date());
    userEntity.setIsFirstVisit(true);
    userEntity.setLastLoginDate(new Date());
    userEntity.setStatus(UserStatus.active);
    userEntity.setType(usertype);

    return flowUserRepository.save(userEntity);
  }

  @Override
  public Optional<UserEntity> getUserById(String id) {
    return flowUserRepository.findById(id);
  }

  @Override
  public Page<UserEntity> findBySearchTerm(String term, Pageable pageable) {
    return flowUserRepository.findByNameLikeIgnoreCaseOrEmailLikeIgnoreCase(term, term, pageable);
  }

  @Override
  public Page<UserEntity> findAll(Pageable pageable) {
    return flowUserRepository.findAll(pageable);
  }

  @Override
  public List<UserEntity> getUsersforTeams(List<String> teamIds) {
    return flowUserRepository.findByFlowTeamsContaining(teamIds);
  }

  @Override
  public UserEntity save(UserEntity user) {
    return flowUserRepository.save(user);
  }

  @Override
  public Long getUserCount() {
    return flowUserRepository.count();
}
  
  @Override
  @NoLogging
  public UserEntity getUserWithEmail(String userEmail) {

    if (userEmail != null) {
    return flowUserRepository.findByEmailIgnoreCase(userEmail);
    }
    return null;

  }

  @Override
  public UserEntity registerUser(UserEntity user) {
    String email = user.getEmail();
    
    Long count = this.flowUserRepository.countByEmailIgnoreCase(email);
    if (count == 1) {
      return this.flowUserRepository.findByEmailIgnoreCase(email);
    }


    return flowUserRepository.save(user);
  }
}
