package io.boomerang.audit;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import io.boomerang.model.Team;
import io.boomerang.model.TeamRequest;
import io.boomerang.model.WorkflowCanvas;
import io.boomerang.model.ref.Workflow;
import io.boomerang.model.ref.WorkflowRun;
import io.boomerang.model.ref.WorkflowRunInsight;
import io.boomerang.security.model.Token;
import io.boomerang.security.service.IdentityService;

/*
 * Intercepts all of the Create, Update, Delete, and Actions performed on objects and creates an Audit log
 * 
 * Ref: https://docs.spring.io/spring-framework/reference/core/aop/ataspectj/advice.html
 * Ref: https://www.baeldung.com/spring-boot-authentication-audit
 */
@Aspect
@Component
public class AuditInterceptor {  
  private static final Logger LOGGER = LogManager.getLogger();
  
  @Autowired
  private IdentityService identityService;
  
  @Autowired
  private AuditRepository auditRepository;

  @Autowired
  private MongoTemplate mongoTemplate;
  
  private Map<String, String> teamNameToAuditId = new HashMap<>();
  
  // Future: using an annotation
//  @AfterReturning("@annotation(audit)")
//  public void audit(JoinPoint thisJoinPoint, Audit audit) {
//    LOGGER.debug("AuditInterceptor - using annotation");
//    LOGGER.debug("AuditInterceptor - {}", thisJoinPoint.getSignature().getDeclaringType());
//    LOGGER.debug("AuditInterceptor - {}", audit.scope());
//  }
  
  /*
   * WORKFLOW auditing
   * 
   * TODO: adjust if Workflow moves to slug
   */
  @AfterReturning(pointcut="execution(* io.boomerang.service.WorkflowService.create(..)) && args(request, team)", returning="entity")
  public void createWorkflow(JoinPoint joinPoint, Workflow request, String team, Workflow entity) {
    createLog(AuditScope.WORKFLOW, entity.getId(), Optional.empty(), Optional.of(getTeamAuditIdFromName(team)), Optional.of(Map.of("name", entity.getName())));
  }
  
  /*
   * Duplicate won't be captured by create (even though it calls create) because AOP is only triggered by proxied request
   * 
   * Ref: https://docs.spring.io/spring-framework/reference/core/aop/proxying.html#aop-understanding-aop-proxies
   */
  @AfterReturning(pointcut="execution(* io.boomerang.service.WorkflowService.duplicate(..)) && args(id)", returning="entity")
  private void duplicateWorkflow(JoinPoint joinPoint, String id, Workflow entity) {
    Map<String, String> data = new HashMap<>();
    data.put("duplicateOf", id);
    data.put("name", entity.getName());
    createLog(AuditScope.WORKFLOW, entity.getId(), Optional.empty(), Optional.of(getParentAuditIdFromWorkflow(id)), Optional.of(data));
  }
  
  @AfterReturning(pointcut="execution(* io.boomerang.service.WorkflowService.apply(..)) && args(request, replace, team)", returning="entity")
  private void updateWorkflow(JoinPoint thisJoinPoint, Workflow request, boolean replace, String team, Workflow entity) {
    updateLog(AuditType.updated, entity.getId(), Optional.empty(), Optional.empty(), Optional.of(Map.of("name", entity.getName())));
  }
  
  @AfterReturning(pointcut="execution(* io.boomerang.service.WorkflowService.composeApply(..)) && args(request, replace, team)", returning="entity")
  private void updateWorkflow(JoinPoint thisJoinPoint, WorkflowCanvas request, boolean replace, Optional<String> team, WorkflowCanvas entity) {
    updateLog(AuditType.updated, entity.getId(), Optional.empty(), Optional.empty(), Optional.of(Map.of("name", entity.getName())));
  }
  
  @AfterReturning(pointcut="execution(* io.boomerang.service.WorkflowService.submit(..)) && args(id, team)", returning="entity")
  private void updateWorkflow(JoinPoint thisJoinPoint, String id, String team, WorkflowRun entity) {
    updateLog(AuditType.submitted, id, Optional.empty(), Optional.of(getTeamAuditIdFromName(team)), Optional.empty());
  }
  
  @AfterReturning("execution(* io.boomerang.service.WorkflowService.delete(..))"
      + " && args(id)")
  private void deleteWorkflow(JoinPoint thisJoinPoint, String id) {
    LOGGER.debug("AuditInterceptor - {}", thisJoinPoint.getSignature().getDeclaringType());
    updateLog(AuditType.deleted, id, Optional.empty(), Optional.empty(), Optional.empty());
  }
  
  /*
   * TEAM auditing
   */
  @AfterReturning(pointcut="execution(* io.boomerang.service.TeamService.create(..)) && args(request)", returning="entity")
  private void createTeam(JoinPoint thisJoinPoint, TeamRequest request, Team entity) {
    AuditEntity log = createLog(AuditScope.TEAM, entity.getId(), Optional.of(entity.getName()), Optional.empty(), Optional.of(Map.of("name", entity.getName())));
    teamNameToAuditId.put(entity.getName(), log.getId());
  }
  
  @AfterReturning(pointcut="execution(* io.boomerang.service.TeamService.patch(..))", returning="entity")
  private void updateTeam(JoinPoint thisJoinPoint, Team entity) {
    AuditEntity log = updateLog(AuditType.updated, entity.getId(), Optional.of(entity.getName()), Optional.empty(), Optional.of(Map.of("name", entity.getName())));
    teamNameToAuditId.put(entity.getName(), log.getId());
  }
  
  @AfterReturning("execution(* io.boomerang.service.TeamService.delete(..))"
      + " && args(id)")
  private void deleteTeam(JoinPoint thisJoinPoint, String id) {
    updateLogByAuditId(AuditType.deleted, getTeamAuditIdFromName(id), Optional.of(""), Optional.empty(), Optional.empty());
    teamNameToAuditId.remove(id);
  }
  
  /*
   * Creates an AuditEntity
   */
  private AuditEntity createLog(AuditScope scope, String selfRef, Optional<String> selfName, Optional<String> parent, Optional<Map<String, String>> data) {
    try {
      LOGGER.debug("AuditInterceptor - Creating new Audit for: {} - {}.", selfRef, selfName.isPresent() ? selfName.get() : "n/a");        
      Token accessToken = this.identityService.getCurrentIdentity();
      AuditEvent auditEvent = new AuditEvent(AuditType.created, accessToken);        
      return auditRepository.insert(new AuditEntity(scope, selfRef, selfName, parent, auditEvent, data));
    } catch (Exception ex) {
      LOGGER.error("Unable to create Audit record with exception: {}.", ex.toString());
    }
    return null;
  }
  
  /*
   * Updates an AuditEntity
   */
  private AuditEntity updateLog(AuditType type, String selfRef, Optional<String> selfName, Optional<String> parent, Optional<Map<String, String>> data) {
    try {
      LOGGER.debug("AuditInterceptor - Updating Audit for: {} with event: {}.", selfRef, type);
      Token accessToken = this.identityService.getCurrentIdentity();
      Optional<AuditEntity> auditEntity = auditRepository.findFirstBySelfRef(selfRef);
      if (auditEntity.isPresent()) {
        if (data.isPresent()) {
          auditEntity.get().getData().putAll(data.get());
        }
        if (selfName.isPresent()) {
          auditEntity.get().setSelfName(selfName.get());
        }
        AuditEvent auditEvent = new AuditEvent(type, accessToken);
        auditEntity.get().getEvents().add(auditEvent);
        return auditRepository.save(auditEntity.get());
      }
    } catch (Exception ex) {
      LOGGER.error("Unable to create Audit record with exception: {}.", ex.toString());
    }
    return null;
  }
  
  /*
   * Updates an AuditEntity
   */
  private AuditEntity updateLogByAuditId(AuditType type, String auditId, Optional<String> selfName, Optional<String> parent, Optional<Map<String, String>> data) {
    try {
      LOGGER.debug("AuditInterceptor - Updating Audit for: {} with event: {}.", auditId, type);
      Token accessToken = this.identityService.getCurrentIdentity();
      Optional<AuditEntity> auditEntity = auditRepository.findById(auditId);
      if (auditEntity.isPresent()) {
        if (data.isPresent()) {
          auditEntity.get().getData().putAll(data.get());
        }
        if (selfName.isPresent()) {
          auditEntity.get().setSelfName(selfName.get());
        }
        AuditEvent auditEvent = new AuditEvent(type, accessToken);
        auditEntity.get().getEvents().add(auditEvent);
        return auditRepository.save(auditEntity.get());
      }
    } catch (Exception ex) {
      LOGGER.error("Unable to create Audit record with exception: {}.", ex.toString());
    }
    return null;
  }

  private String getTeamAuditIdFromName(String name) {
    if (teamNameToAuditId.containsKey(name)) {
      return teamNameToAuditId.get(name);
    }
    Optional<AuditEntity> optAuditEntity = auditRepository.findFirstBySelfName(name);
    if (optAuditEntity.isPresent()) {
      teamNameToAuditId.put(name, optAuditEntity.get().getId());
      return optAuditEntity.get().getId();
    }
    LOGGER.error("Unable to find Audit record for team: {}", name);
    return null;
  }

  private String getParentAuditIdFromWorkflow(String name) {
    Optional<AuditEntity> optAuditEntity = auditRepository.findFirstByWorkflowDuplicateOf(name);
    if (optAuditEntity.isPresent()) {
      return optAuditEntity.get().getParent();
    }
    LOGGER.error("Unable to find parent Audit record for: {}", name);
    return null;
  }
  
  public WorkflowRunInsight insights(Optional<Long> from, Optional<Long> to, String queryTeam) {
    List<Criteria> criteriaList = new ArrayList<>();

    Optional<Date> fromDate = Optional.empty();
    Optional<Date> toDate = Optional.empty();
    if (from.isPresent()) {
      fromDate = Optional.of(new Date(from.get()));
    }
    if (to.isPresent()) {
      toDate = Optional.of(new Date(to.get()));
    }
    if (fromDate.isPresent() && !toDate.isPresent()) {
      Criteria criteria = Criteria.where("creationDate").gte(fromDate.get());
      criteriaList.add(criteria);
    } else if (!fromDate.isPresent() && toDate.isPresent()) {
      Criteria criteria = Criteria.where("creationDate").lt(toDate.get());
      criteriaList.add(criteria);
    } else if (fromDate.isPresent() && toDate.isPresent()) {
      Criteria criteria = Criteria.where("creationDate").gte(fromDate.get()).lt(toDate.get());
      criteriaList.add(criteria);
    }
    criteriaList.add(Criteria.where("parent").is(queryTeam));
    criteriaList.add(Criteria.where("scope").is(AuditScope.WORKFLOWRUN));
    Criteria[] criteriaArray = criteriaList.toArray(new Criteria[criteriaList.size()]);
    Criteria allCriteria = new Criteria();
    if (criteriaArray.length > 0) {
      allCriteria.andOperator(criteriaArray);
    }
    Query query = new Query(allCriteria);
    LOGGER.debug("Query: " + query.toString());
    List<AuditEntity> auditEntities = mongoTemplate.find(query, AuditEntity.class);

    // Collect the Stats
    Long totalDuration = 0L;
    Long duration;

    for (AuditEntity auditEntity : auditEntities) {
      duration = Long.valueOf(auditEntity.getData().get("duration"));
      if (duration != null) {
        totalDuration += duration;
      }
    }

    WorkflowRunInsight wfRunInsight = new WorkflowRunInsight();
    wfRunInsight.setTotalRuns(Long.valueOf(auditEntities.size()));
//    wfRunInsight.setConcurrentRuns(
//        wfRunEntities.stream().filter(run -> RunPhase.running.equals(run.getPhase())).count());
    wfRunInsight.setTotalDuration(totalDuration);
    if (auditEntities.size() != 0) {
      wfRunInsight.setMedianDuration(totalDuration / auditEntities.size());
    } else {
      wfRunInsight.setMedianDuration(0L);
    }
    return wfRunInsight;
  }
}