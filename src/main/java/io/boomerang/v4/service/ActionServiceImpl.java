package io.boomerang.v4.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.mongo.model.Audit;
import io.boomerang.security.service.IdentityService;
import io.boomerang.v4.client.EngineClient;
import io.boomerang.v4.data.entity.ApproverGroupEntity;
import io.boomerang.v4.data.entity.UserEntity;
import io.boomerang.v4.data.entity.ref.ActionEntity;
import io.boomerang.v4.data.repository.ApproverGroupRepository;
import io.boomerang.v4.data.repository.ref.ActionRepository;
import io.boomerang.v4.model.Action;
import io.boomerang.v4.model.ActionRequest;
import io.boomerang.v4.model.ActionSummary;
import io.boomerang.v4.model.enums.RelationshipRef;
import io.boomerang.v4.model.enums.RelationshipType;
import io.boomerang.v4.model.enums.ref.ActionStatus;
import io.boomerang.v4.model.enums.ref.ActionType;
import io.boomerang.v4.model.enums.ref.RunStatus;
import io.boomerang.v4.model.ref.TaskRun;
import io.boomerang.v4.model.ref.TaskRunEndRequest;
import io.boomerang.v4.model.ref.Workflow;

@Service
public class ActionServiceImpl implements ActionService {

  @Autowired
  private ActionRepository actionRepository;

  @Autowired
  private ApproverGroupRepository approverGroupRepository;

  @Autowired
  private EngineClient engineClient;

  @Autowired
  private RelationshipService relationshipService;

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
  public void action(List<ActionRequest> requests) {
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
      List<String> workflowRefs =
          relationshipService.getFilteredRefs(Optional.of(RelationshipRef.WORKFLOW),
              Optional.of(List.of(actionEntity.getWorkflowRef())),
              Optional.of(RelationshipType.BELONGSTO), Optional.empty(), Optional.empty());
      if (workflowRefs.isEmpty()) {
        throw new BoomerangException(BoomerangError.ACTION_INVALID_REF);
      }
      
      boolean canBeActioned = false;
      UserEntity userEntity = userIdentityService.getCurrentUser();
      if (actionEntity.getType() == ActionType.manual) {
        // Manual tasks only require a single yes or no
        canBeActioned = true;
      } else if (actionEntity.getType() == ActionType.approval) {
        if (actionEntity.getApproverGroupRef() != null) {
          List<String> approverGroupRefs = relationshipService.getFilteredRefs(
              Optional.of(RelationshipRef.APPROVERGROUP), Optional.of(List.of(actionEntity.getApproverGroupRef())),
              Optional.of(RelationshipType.BELONGSTO), Optional.empty(), Optional.empty());
          if (approverGroupRefs.isEmpty()) {
            //TODO better error around INVALID APPROVER GROUP REF
            throw new BoomerangException(BoomerangError.ACTION_INVALID_REF);
          }
          Optional<ApproverGroupEntity> approverGroupEntity = approverGroupRepository.findById(actionEntity.getApproverGroupRef());
          if (approverGroupEntity.isEmpty()) {
            //TODO better error around INVALID APPROVER GROUP REF
            throw new BoomerangException(BoomerangError.ACTION_INVALID_REF);
          }
          boolean partOfGroup = approverGroupEntity.get().getApproverRefs().contains(userEntity.getId());
          if (partOfGroup) {
            canBeActioned = true;
          }
        } else {
          canBeActioned = true;
        }
      }

      if (canBeActioned) {
        Audit audit = new Audit();
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
        TaskRunEndRequest endRequest = new TaskRunEndRequest();
        endRequest.setStatus(approved ? RunStatus.succeeded : RunStatus.failed);
        engineClient.endTaskRun(actionEntity.getTaskRunRef(), endRequest);
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
      for (Audit audit : actionEntity.getActioners()) {
        UserEntity user = this.userIdentityService.getUserByID(audit.getApproverId());
        if (user != null) {
          audit.setApproverName(user.getName());
          audit.setApproverEmail(user.getEmail());
        }
      }
      action.setActioners(actionEntity.getActioners());
    }

    Workflow workflow = engineClient.getWorkflow(actionEntity.getWorkflowRef(), Optional.empty(), false);
    action.setWorkflowName(workflow.getName());
    TaskRun taskRun = engineClient.getTaskRun(actionEntity.getTaskRunRef());
    action.setTaskName(taskRun.getName());

    return action;
  }

  @Override
  public Action get(String id) {
    Optional<ActionEntity> actionEntity = this.actionRepository.findById(id);
    if (actionEntity.isEmpty()) {
      throw new BoomerangException(BoomerangError.ACTION_INVALID_REF);
    }
    return this.convertToAction(actionEntity.get());
  }

  @Override
  public Action getByTaskRun(String id) {
    Optional<ActionEntity> actionEntity = this.actionRepository.findByTaskRunRef(id);
    if (actionEntity.isEmpty()) {
      throw new BoomerangException(BoomerangError.ACTION_INVALID_REF);
    }
    return this.convertToAction(actionEntity.get());
  }

  @Override
  public Page<Action> query(Optional<Date> from, Optional<Date> to, Pageable pageable,
      Optional<List<ActionType>> types, Optional<List<ActionStatus>> status,
      Optional<List<String>> workflowIds, Optional<List<String>> teamIds) {
    List<String> workflowRefs =
        relationshipService.getFilteredRefs(Optional.of(RelationshipRef.WORKFLOW), workflowIds,
            Optional.of(RelationshipType.BELONGSTO), Optional.of(RelationshipRef.TEAM), teamIds);

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
  public ActionSummary summary(Optional<Date> fromDate, Optional<Date> toDate, Optional<List<String>> workflowIds, Optional<List<String>> teamIds) {
    List<String> workflowRefs =
        relationshipService.getFilteredRefs(Optional.of(RelationshipRef.WORKFLOW),
            workflowIds, Optional.of(RelationshipType.BELONGSTO), Optional.of(RelationshipRef.TEAM), teamIds);
    
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
    
    if (type.isPresent()) {
      Criteria dynamicCriteria = Criteria.where("type").in(type.get());
      criterias.add(dynamicCriteria);
    }
    
    if (status.isPresent()) {
      Criteria dynamicCriteria = Criteria.where("status").in(status.get());
      criterias.add(dynamicCriteria);
    }
    
    if (workflowRefs.isPresent()) {
      Criteria workflowIdsCriteria = Criteria.where("workflowRef").in(workflowRefs);
      criterias.add(workflowIdsCriteria);
    }
    return new Criteria().andOperator(criterias.toArray(new Criteria[criterias.size()]));
  }

}
