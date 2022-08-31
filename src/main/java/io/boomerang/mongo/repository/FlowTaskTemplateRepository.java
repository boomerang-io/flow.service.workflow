package io.boomerang.mongo.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import io.boomerang.mongo.entity.FlowTaskTemplateEntity;
import io.boomerang.mongo.model.FlowTaskTemplateStatus;

public interface FlowTaskTemplateRepository
    extends MongoRepository<FlowTaskTemplateEntity, String> {

  @Override
  Optional<FlowTaskTemplateEntity> findById(String id);
  
  List<FlowTaskTemplateEntity> findByIdIn(List<String> ids);

  @Override
  List<FlowTaskTemplateEntity> findAll();

  List<FlowTaskTemplateEntity> findByStatus(FlowTaskTemplateStatus active);

  FlowTaskTemplateEntity findByIdAndStatus(String id, FlowTaskTemplateStatus active);
  
  @Query(value = "{  \n"
      + "    $and : [{\n"
      + "       $or: [\n"
      + "        {\"scope\" : \"team\", \"flowTeamId\" : ?0},\n"
      + "        {\"scope\" : \"global\"},\n"
      + "        {\"scope\" : null}\n"
      + "    ]}\n"
      + "    ]\n"
      + "}")
  List<FlowTaskTemplateEntity> findAllByFlowTeamId(String flowTeamId);
  
  @Query(value = "{  \n"
      + "    $and : [{\n"
      + "       $or: [\n"
      + "        {\"scope\" : \"system\"},\n"
      + "        {\"scope\" : \"global\"},\n"
      + "        {\"scope\" : null}\n"
      + "    ]}\n"
      + "    ]\n"
      + "}")
  List<FlowTaskTemplateEntity> findAllForSystemTasks();

  @Query(value = "{\"scope\" : \"team\", \"flowTeamId\" : ?0, \"status\" : \"active\"}")
  List<FlowTaskTemplateEntity> findByFlowTeamId(String flowTeamId);
  
  
  @Query(value = "{\"scope\" : \"system\"}")
  List<FlowTaskTemplateEntity> findAllSystemTasks();

 
  @Query(value = "{\n"
      + "       $or: [\n"
      + "        {\"scope\" : \"global\"},\n"
      + "        {\"scope\" : null}\n"
      + "    ]}\n")
  List<FlowTaskTemplateEntity> findAllGlobalTasks();
}


