package io.boomerang.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import io.boomerang.client.EngineClient;
import io.boomerang.data.entity.ApproverGroupEntity;
import io.boomerang.data.entity.UserEntity;
import io.boomerang.data.entity.ref.ActionEntity;
import io.boomerang.data.model.ref.Actioner;
import io.boomerang.data.repository.ApproverGroupRepository;
import io.boomerang.data.repository.ref.ActionRepository;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.model.Action;
import io.boomerang.model.ActionRequest;
import io.boomerang.model.ActionSummary;
import io.boomerang.model.User;
import io.boomerang.model.enums.RelationshipType;
import io.boomerang.model.enums.RelationshipLabel;
import io.boomerang.model.enums.ref.ActionStatus;
import io.boomerang.model.enums.ref.ActionType;
import io.boomerang.model.enums.ref.RunStatus;
import io.boomerang.model.ref.TaskRun;
import io.boomerang.model.ref.TaskRunEndRequest;
import io.boomerang.model.ref.Workflow;
import io.boomerang.security.service.IdentityService;

@Service
public class ActionServiceImpl implements ActionService {

  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  private ActionRepository actionRepository;

  @Autowired
  private ApproverGroupRepository approverGroupRepository;

  @Autowired
  private EngineClient engineClient;

  @Autowired
  private RelationshipServiceImpl relationshipServiceImpl;

  @Autowired
  private IdentityService userIdentityService;
  
  @Autowired
  private MongoTemplate mongoTemplate;

  /*
   * Updates / Processes an Action
   * 
   * TODO: at this point in time, only users can process Actions even though we have an API that
   * allows it. Once fixed will need to adjust the token scope on the Controller
   */
  @Override
  public void action(String team, List<ActionRequest> requests) {
    for (ActionRequest request : requests) {
      Optional<ActionEntity> optActionEntity = this.actionRepository.findById(request.getId());
      if (!optActionEntity.isPresent()) {
        throw new BoomerangException(BoomerangError.ACTION_INVALID_REF);
      }

      ActionEntity actionEntity = optActionEntity.get();
      if (actionEntity.getActioners() == null) {
        actionEntity.setActioners(new LinkedList<>());
      }

      // Check if requester has access to the Workflow the Action Entity belongs to
      if (relationshipServiceImpl.hasTeamRelationship(Optional.of(RelationshipType.WORKFLOW),
          Optional.of(actionEntity.getWorkflowRef()), RelationshipLabel.BELONGSTO, team, true)) {
        throw new BoomerangException(BoomerangError.ACTION_INVALID_REF);
      }
      
      boolean canBeActioned = false;
      UserEntity userEntity = userIdentityService.getCurrentUser();
      if (actionEntity.getType() == ActionType.manual) {
        // Manual tasks only require a single yes or no
        canBeActioned = true;
      } else if (actionEntity.getType() == ActionType.approval) {
        if (actionEntity.getApproverGroupRef() != null) {
          List<String> approverGroupRefs = relationshipServiceImpl.getFilteredRefs(
              Optional.of(RelationshipType.APPROVERGROUP), Optional.of(List.of(actionEntity.getApproverGroupRef())),
              RelationshipLabel.BELONGSTO, RelationshipType.TEAM, team, false);
          if (approverGroupRefs.isEmpty()) {
            throw new BoomerangException(BoomerangError.ACTION_INVALID_APPROVERGROUP);
          }
          Optional<ApproverGroupEntity> approverGroupEntity = approverGroupRepository.findById(actionEntity.getApproverGroupRef());
          if (approverGroupEntity.isEmpty()) {
            throw new BoomerangException(BoomerangError.ACTION_INVALID_APPROVERGROUP);
          }
          boolean partOfGroup = approverGroupEntity.get().getApprovers().contains(userEntity.getId());
          if (partOfGroup) {
            canBeActioned = true;
          }
        } else {
          canBeActioned = true;
        }
      }

      if (canBeActioned) {
        Actioner audit = new Actioner();
        audit.setActionDate(new Date());
        audit.setApproverId(userEntity.getId());
        audit.setComments(request.getComments());
        audit.setApproved(request.isApproved());
        actionEntity.getActioners().add(audit);
      }

      int numberApprovals = actionEntity.getNumberOfApprovers();
      long approvedCount =
          actionEntity.getActioners().stream().filter(x -> x.isApproved()).count();
      long numberOfActioners = actionEntity.getActioners().size();

      if (numberOfActioners >= numberApprovals) {
        boolean approved = false;
        if (approvedCount == numberApprovals) {
          approved = true;
        }
        actionEntity.setStatus(approved ? ActionStatus.approved : ActionStatus.rejected);
        try {
          TaskRunEndRequest endRequest = new TaskRunEndRequest();
          endRequest.setStatus(approved ? RunStatus.succeeded : RunStatus.failed);
          engineClient.endTaskRun(actionEntity.getTaskRunRef(), endRequest);
        } catch (BoomerangException e) {
          throw new BoomerangException(BoomerangError.ACTION_UNABLE_TO_ACTION);
        }
        this.actionRepository.save(actionEntity);
      }
    }
  }

  private Action convertToAction(ActionEntity actionEntity) {
    Action action = new Action(actionEntity);

    action.setApprovalsRequired(actionEntity.getNumberOfApprovers());

    if (actionEntity.getActioners() != null) {
      long aprovalCount =
          actionEntity.getActioners().stream().filter(x -> x.isApproved()).count();

      action.setNumberOfApprovals(aprovalCount);
      for (Actioner audit : actionEntity.getActioners()) {
        Optional<User> user = userIdentityService.getUserByID(audit.getApproverId());
        if (user.isPresent()) {
          audit.setApproverName(user.get().getName());
          audit.setApproverEmail(user.get().getEmail());
        }
      }
      action.setActioners(actionEntity.getActioners());
    }

    Workflow workflow = engineClient.getWorkflow(actionEntity.getWorkflowRef(), Optional.empty(), false);
    action.setWorkflowName(workflow.getName());
    try {
      TaskRun taskRun = engineClient.getTaskRun(actionEntity.getTaskRunRef());
      action.setTaskName(taskRun.getName());
    } catch (BoomerangException e) {
      LOGGER.error("convertToAction() - Skipping specific TaskRun as not available. Most likely bad data");
    }

    return action;
  }

  @Override
  public Action get(String team, String id) {
    Optional<ActionEntity> actionEntity = this.actionRepository.findById(id);
    if (actionEntity.isEmpty()) {
      throw new BoomerangException(BoomerangError.ACTION_INVALID_REF);
    }
    return this.convertToAction(actionEntity.get());
  }

//  @Override
//  public Action getByTaskRun(String id) {
//    Optional<ActionEntity> actionEntity = this.actionRepository.findByTaskRunRef(id);
//    if (actionEntity.isEmpty()) {
//      throw new BoomerangException(BoomerangError.ACTION_INVALID_REF);
//    }
//    return this.convertToAction(actionEntity.get());
//  }

  @Override
  public Page<Action> query(String team, Optional<Date> from, Optional<Date> to, Pageable pageable,
      Optional<List<ActionType>> types, Optional<List<ActionStatus>> status,
      Optional<List<String>> workflowIds) {
    List<String> workflowRefs =
        relationshipServiceImpl.getFilteredRefs(Optional.of(RelationshipType.WORKFLOW), workflowIds,
            RelationshipLabel.BELONGSTO, RelationshipType.TEAM, team, false);

    Criteria criteria = buildCriteriaList(from, to, Optional.of(workflowRefs), types, status);
    Query query = new Query(criteria).with(pageable);

    List<ActionEntity> actionEntities =
        mongoTemplate.find(query.with(pageable), ActionEntity.class);

    List<Action> actions = new LinkedList<>();
    actionEntities.forEach(a -> {
      actions.add(this.convertToAction(a));
    });

    Page<Action> pages = PageableExecutionUtils.getPage(actions, pageable,
        () -> mongoTemplate.count(query, ActionEntity.class));

    return pages;
  }

  @Override
  public ActionSummary summary(String team, Optional<Date> fromDate, Optional<Date> toDate, Optional<List<String>> workflowIds) {
    List<String> workflowRefs =
        relationshipServiceImpl.getFilteredRefs(Optional.of(RelationshipType.WORKFLOW), workflowIds,
            RelationshipLabel.BELONGSTO, RelationshipType.TEAM, team, false);
    
    long approvalCount = this.getActionCountForType(ActionType.approval, fromDate,
        toDate, Optional.of(workflowRefs));
    long manualCount = this.getActionCountForType(ActionType.manual, fromDate, toDate,
        Optional.of(workflowRefs));
    long rejectedCount =
        getActionCountForStatus(ActionStatus.rejected, fromDate, toDate);
    long approvedCount =
        getActionCountForStatus(ActionStatus.approved, fromDate, toDate);
    long submittedCount =
        getActionCountForStatus(ActionStatus.submitted, fromDate, toDate);
    long total = rejectedCount + approvedCount + submittedCount;
    long approvalRateCount = 0;

    if (total != 0) {
      approvalRateCount = (((approvedCount + rejectedCount) / total) * 100);
    }

    ActionSummary summary = new ActionSummary();
    summary.setApprovalsRate(approvalRateCount);
    summary.setManual(manualCount);
    summary.setApprovals(approvalCount);

    return summary;
  }

  private long getActionCountForType(ActionType type,  Optional<Date> from, Optional<Date> to, Optional<List<String>> workflowRefs) {
    Criteria criteria = this.buildCriteriaList(from, to, workflowRefs, Optional.of(List.of(type)), Optional.of(List.of(ActionStatus.submitted)));
    return mongoTemplate.count(new Query(criteria), ActionEntity.class);
  }
  
  private long getActionCountForStatus(ActionStatus status, Optional<Date> from,
      Optional<Date> to) {
    Criteria criteria = this.buildCriteriaList(from, to, Optional.empty(), Optional.empty(), Optional.of(List.of(status)));
    return mongoTemplate.count(new Query(criteria), ActionEntity.class);
  }
  
  private Criteria buildCriteriaList(Optional<Date> from, Optional<Date> to, Optional<List<String>> workflowRefs,
      Optional<List<ActionType>> type, Optional<List<ActionStatus>> status) {
    List<Criteria> criterias = new ArrayList<>();
    
    if (from.isPresent()) {
      Criteria dynamicCriteria = Criteria.where("creationDate").gte(from.get());
      criterias.add(dynamicCriteria);
    }
    
    if (to.isPresent()) {
      Criteria dynamicCriteria = Criteria.where("creationDate").lte(to.get());
      criterias.add(dynamicCriteria);
    }
    
    if (workflowRefs.isPresent()) {
      Criteria workflowIdsCriteria = Criteria.where("workflowRef").in(workflowRefs.get());
      criterias.add(workflowIdsCriteria);
    }
    
    if (type.isPresent()) {
      Criteria dynamicCriteria = Criteria.where("type").in(type.get());
      criterias.add(dynamicCriteria);
    }
    
    if (status.isPresent()) {
      Criteria dynamicCriteria = Criteria.where("status").in(status.get());
      criterias.add(dynamicCriteria);
    }
    
    return new Criteria().andOperator(criterias.toArray(new Criteria[criterias.size()]));
  }
  
  @Override
  public void deleteAllByWorkflow(String workflowRef) {
    actionRepository.deleteByWorkflowRef(workflowRef);
  }
}
