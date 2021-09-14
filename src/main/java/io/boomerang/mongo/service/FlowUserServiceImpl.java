package io.boomerang.mongo.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import io.boomerang.mongo.entity.FlowUserEntity;
import io.boomerang.mongo.model.UserStatus;
import io.boomerang.mongo.model.UserType;
import io.boomerang.mongo.repository.FlowUserRepository;

@Service
public class FlowUserServiceImpl implements FlowUserService {

  @Autowired
  private FlowUserRepository flowUserRepository;

  @Override
  public FlowUserEntity getOrRegisterUser(String email, String firstName, String lastName, UserType usertype) {

    Long count = this.flowUserRepository.countByEmailIgnoreCase(email);
    if (count == 1) {
      return this.flowUserRepository.findByEmailIgnoreCase(email);
    }

    String name = String.format("%s %s", Optional.ofNullable(firstName).orElse(""), Optional.ofNullable(lastName).orElse("")).trim();
    if (firstName == null && lastName == null && email != null) {
      name = email;
    }

    FlowUserEntity userEntity = new FlowUserEntity();
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
  public Optional<FlowUserEntity> getUserById(String id) {
    return flowUserRepository.findById(id);
  }

  @Override
  public Page<FlowUserEntity> findBySearchTerm(String term, Pageable pageable) {
    return flowUserRepository.findByNameLikeIgnoreCaseOrEmailLikeIgnoreCase(term, term, pageable);
  }

  @Override
  public Page<FlowUserEntity> findAll(Pageable pageable) {
    return flowUserRepository.findAll(pageable);
  }

  @Override
  public List<FlowUserEntity> getUsersforTeams(List<String> teamIds) {
    return flowUserRepository.findByFlowTeamsContaining(teamIds);
  }

  @Override
  public FlowUserEntity save(FlowUserEntity user) {
    return flowUserRepository.save(user);
  }

  @Override
  public Long getUserCount() {
    return flowUserRepository.count();
}
  
  @Override
  @NoLogging
  public FlowUserEntity getUserWithEmail(String userEmail) {

    if (userEmail != null) {
    return flowUserRepository.findByEmailIgnoreCase(userEmail);
    }
    return null;

  }

  @Override
  public FlowUserEntity registerUser(FlowUserEntity user) {
    String email = user.getEmail();
    
    Long count = this.flowUserRepository.countByEmailIgnoreCase(email);
    if (count == 1) {
      return this.flowUserRepository.findByEmailIgnoreCase(email);
    }


    return flowUserRepository.save(user);
  }
}
