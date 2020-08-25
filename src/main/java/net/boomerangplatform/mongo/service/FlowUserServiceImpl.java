package net.boomerangplatform.mongo.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import net.boomerangplatform.mongo.entity.FlowUserEntity;
import net.boomerangplatform.mongo.model.UserStatus;
import net.boomerangplatform.mongo.model.UserType;
import net.boomerangplatform.mongo.repository.FlowUserRepository;

@Service
public class FlowUserServiceImpl implements FlowUserService {

  @Autowired
  private FlowUserRepository flowUserRepository;

  @Override
  public FlowUserEntity getOrRegisterUser(String userNane, String firstName, String lastName, UserType usertype) {

    Long count = this.flowUserRepository.countByEmailIgnoreCase(userNane);
    if (count == 1) {
      return this.flowUserRepository.findByEmailIgnoreCase(userNane);
    }

    final String name = String.format("%s %s", firstName, lastName);

    FlowUserEntity userEntity = new FlowUserEntity();
    userEntity.setEmail(userNane);
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
}
