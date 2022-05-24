package net.boomerangplatform.mongo.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import net.boomerangplatform.mongo.entity.RevisionEntity;
import net.boomerangplatform.mongo.entity.WorkFlowRevisionAggr;

public interface FlowWorkflowVersionRepository
    extends MongoRepository<RevisionEntity, String> {

  long countByworkFlowId(String workFlowId);
  
  Object countByworkFlowIdIn(List<String> wofkFlowIds);
  
  @Override
  Optional<RevisionEntity> findById(String id);

  RevisionEntity findByworkFlowIdAndVersion(String workFlowId, long version);

  Page<RevisionEntity> findByworkFlowId(String string, Pageable pageable);
  
  @Aggregation(pipeline = {
		  "{'$match':{'workFlowId': {$in: ?0}}}",
	      "{'$sort':{'workFlowId': -1, version: -1}}",
	      "{'$group': { _id: '$workFlowId', 'count': { $sum: 1 }}}"
	})
  List<WorkFlowRevisionAggr> findWorkFlowVersionCount(List<String> workflowIds);
  
  @Aggregation(pipeline = {
		  "{'$match':{'workFlowId': {$in: ?0}}}",
	      "{'$sort':{'workFlowId': -1, version: -1}}",
	      "{'$group': { _id: '$workFlowId', 'count': { $sum: 1 }, 'latestVersion': {$first: '$$ROOT'}}}"
	})
  List<WorkFlowRevisionAggr> findWorkFlowVersionCountAndLatestVersion(List<String> workflowIds);
}
