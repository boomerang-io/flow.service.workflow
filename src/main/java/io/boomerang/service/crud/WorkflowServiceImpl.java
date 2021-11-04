package io.boomerang.service.crud;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.cronutils.mapper.CronMapper;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.boomerang.client.model.Team;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.model.CronValidationResponse;
import io.boomerang.model.DuplicateRequest;
import io.boomerang.model.FlowTeam;
import io.boomerang.model.FlowWorkflowRevision;
import io.boomerang.model.GenerateTokenResponse;
import io.boomerang.model.TemplateWorkflowSummary;
import io.boomerang.model.UserWorkflowSummary;
import io.boomerang.model.WorkflowExport;
import io.boomerang.model.WorkflowQuotas;
import io.boomerang.model.WorkflowShortSummary;
import io.boomerang.model.WorkflowSummary;
import io.boomerang.model.WorkflowToken;
import io.boomerang.model.controller.TaskResult;
import io.boomerang.model.projectstormv5.WorkflowRevision;
import io.boomerang.mongo.entity.ActivityEntity;
import io.boomerang.mongo.entity.FlowTaskTemplateEntity;
import io.boomerang.mongo.entity.FlowUserEntity;
import io.boomerang.mongo.entity.RevisionEntity;
import io.boomerang.mongo.entity.WorkflowEntity;
import io.boomerang.mongo.model.Dag;
import io.boomerang.mongo.model.Quotas;
import io.boomerang.mongo.model.Revision;
import io.boomerang.mongo.model.TaskStatus;
import io.boomerang.mongo.model.TaskType;
import io.boomerang.mongo.model.Trigger;
import io.boomerang.mongo.model.TriggerEvent;
import io.boomerang.mongo.model.TriggerScheduler;
import io.boomerang.mongo.model.Triggers;
import io.boomerang.mongo.model.UserType;
import io.boomerang.mongo.model.WorkflowProperty;
import io.boomerang.mongo.model.WorkflowScope;
import io.boomerang.mongo.model.WorkflowStatus;
import io.boomerang.mongo.model.next.DAGTask;
import io.boomerang.mongo.service.FlowSettingsService;
import io.boomerang.mongo.service.FlowTaskTemplateService;
import io.boomerang.mongo.service.FlowWorkflowActivityService;
import io.boomerang.mongo.service.FlowWorkflowService;
import io.boomerang.mongo.service.RevisionService;
import io.boomerang.quartz.ScheduledTasks;
import io.boomerang.service.PropertyManager;
import io.boomerang.service.UserIdentityService;
import io.boomerang.service.runner.misc.ControllerClient;
import io.boomerang.util.ModelConverterV5;

@Service
public class WorkflowServiceImpl implements WorkflowService {

  @Autowired
  private FlowWorkflowActivityService flowWorkflowActivityService;

  @Autowired
  private ScheduledTasks taskScheduler;

  @Autowired
  private FlowWorkflowService workFlowRepository;

  @Autowired
  private RevisionService workflowVersionService;

  @Autowired
  private FlowTaskTemplateService templateService;

  @Autowired
  private UserIdentityService userIdentityService;

  @Autowired
  private PropertyManager propertyManager;

  @Autowired
  private TeamService teamService;

  @Autowired
  @Lazy
  private ControllerClient controllerClient;

  @Autowired
  private FlowSettingsService flowSettingsService;

  @Value("${max.workflow.count}")
  private Integer maxWorkflowCount;

  @Value("${max.workflow.execution.monthly}")
  private Integer maxWorkflowExecutionMonthly;

  @Value("${max.concurrent.workflows}")
  private Integer maxConcurrentWorkflows;

  @Value("${max.workflow.storage}")
  private Integer maxWorkflowStorage;

  @Value("${max.workflow.execution.time}")
  private Integer maxWorkflowExecutionTime;

  private final Logger logger = LogManager.getLogger(getClass());

  @Override
  public void deleteWorkflow(String workFlowid) {
    final WorkflowEntity entity = workFlowRepository.getWorkflow(workFlowid);
    entity.setStatus(WorkflowStatus.deleted);
    workFlowRepository.saveWorkflow(entity);

    if (entity.getTriggers() != null) {
      Triggers trigger = entity.getTriggers();
      if (trigger != null) {
        TriggerScheduler scheduler = trigger.getScheduler();
        if (scheduler != null && scheduler.getEnable()) {
          try {
            this.taskScheduler.cancelJob(entity.getId());
          } catch (SchedulerException e) {
            logger.info("Unable to remove job. ");
            logger.error(e);
          }
        }
      }
    }
  }

  @Override
  public WorkflowSummary getWorkflow(String workflowId) {

    final WorkflowEntity entity = workFlowRepository.getWorkflow(workflowId);

    // if (entity.getScope() == WorkflowScope.user
    // && !entity.getOwnerUserId().equals(userIdentityService.getCurrentUser().getId())) {
    // throw new HttpClientErrorException(HttpStatus.FORBIDDEN);
    // }

    setupTriggerDefaults(entity);


    final WorkflowSummary summary = new WorkflowSummary(entity);
    updateSummaryInformation(summary);
    return summary;
  }

  @Override
  public List<WorkflowSummary> getWorkflowsForTeam(String flowTeamId) {
    final List<WorkflowEntity> list = workFlowRepository.getWorkflowsForTeams(flowTeamId);
    final List<WorkflowSummary> newList = new LinkedList<>();
    for (final WorkflowEntity entity : list) {

      setupTriggerDefaults(entity);

      final WorkflowSummary summary = new WorkflowSummary(entity);
      updateSummaryInformation(summary);

      if (WorkflowStatus.active == entity.getStatus()) {
        newList.add(summary);
      }

    }
    return newList;
  }

  @Override
  public WorkflowSummary saveWorkflow(final WorkflowEntity flowWorkflowEntity) {

    boolean isNewWorkflow = flowWorkflowEntity.getId() == null;
    setupTriggerDefaults(flowWorkflowEntity);

    if (flowWorkflowEntity.getScope() == WorkflowScope.user) {
      FlowUserEntity user = userIdentityService.getCurrentUser();
      flowWorkflowEntity.setOwnerUserId(user.getId());
    }

    WorkflowEntity entity = workFlowRepository.saveWorkflow(flowWorkflowEntity);
    if (isNewWorkflow) {
      this.generateTriggerToken(entity.getId(), "default");
    }

    entity = workFlowRepository.getWorkflow(entity.getId());

    final WorkflowSummary summary = new WorkflowSummary(entity);


    if (summary.getTriggers() != null) {
      Triggers trigger = summary.getTriggers();
      TriggerScheduler scheduler = trigger.getScheduler();
      if (scheduler != null && scheduler.getEnable()) {
        logger.info("Scheduling workflow: {}", scheduler.getSchedule());
        this.taskScheduler.scheduleWorkflow(entity);
      }
    }

    return summary;
  }

  private void setupTriggerDefaults(final WorkflowEntity flowWorkflowEntity) {

    if (flowWorkflowEntity.getTokens() == null) {
      flowWorkflowEntity.setTokens(new LinkedList<>());
    }

    if (flowWorkflowEntity.getTriggers() == null) {
      flowWorkflowEntity.setTriggers(new Triggers());
      flowWorkflowEntity.getTriggers().getManual().setEnable(true);
    }
    if (flowWorkflowEntity.getTriggers().getManual() == null) {
      TriggerEvent manual = new TriggerEvent();
      manual.setEnable(Boolean.TRUE);

      flowWorkflowEntity.getTriggers().setManual(manual);
    }

    if (flowWorkflowEntity.getTriggers().getSlack() == null) {
      TriggerEvent slack = new TriggerEvent();
      flowWorkflowEntity.getTriggers().setSlack(slack);
    }

    if (flowWorkflowEntity.getTriggers().getDockerhub() == null) {
      TriggerEvent dockerhub = new TriggerEvent();
      flowWorkflowEntity.getTriggers().setDockerhub(dockerhub);
    }

    if (flowWorkflowEntity.getTriggers().getCustom() == null) {
      TriggerEvent custom = new TriggerEvent();
      flowWorkflowEntity.getTriggers().setCustom(custom);
    }

    if (flowWorkflowEntity.getTriggers().getWebhook() == null) {
      TriggerEvent webhook = new TriggerEvent();
      flowWorkflowEntity.getTriggers().setWebhook(webhook);
    }
  }

  private void updateSummaryInformation(final WorkflowSummary entity) {
    final String workflowId = entity.getId();
    final long versionCount = workflowVersionService.getWorkflowCount(workflowId);
    entity.setRevisionCount(versionCount);
  }

  @Override
  public WorkflowSummary updateWorkflow(WorkflowSummary summary) {
    final WorkflowEntity entity = workFlowRepository.getWorkflow(summary.getId());

    entity.setFlowTeamId(summary.getFlowTeamId());
    entity.setName(summary.getName());
    entity.setDescription(summary.getDescription());
    entity.setIcon(summary.getIcon());
    entity.setShortDescription(summary.getShortDescription());
    entity.setStatus(summary.getStatus());


    createOrDeleteWorkspace(summary, entity);
    entity.setStorage(summary.getStorage());


    entity.setLabels(summary.getLabels());

    List<WorkflowProperty> updatedProperties = setupDefaultProperties(summary);
    entity.setProperties(updatedProperties);
    Triggers previousTriggers = entity.getTriggers();
    Triggers trigger = summary.getTriggers();

    updateTriggers(entity, previousTriggers, trigger);

    workFlowRepository.saveWorkflow(entity);
    WorkflowSummary updatedSummary = new WorkflowSummary(entity);
    updateSummaryInformation(updatedSummary);

    return updatedSummary;

  }

  private void createOrDeleteWorkspace(WorkflowSummary summary, final WorkflowEntity entity) {
    boolean previousStorageState = false;
    if (entity.getStorage() != null && entity.getStorage().getWorkflow() != null) {
      previousStorageState = entity.getStorage().getWorkflow().getEnabled();
    }

    boolean newStorageState = false;
    if (summary.getStorage() != null && summary.getStorage().getWorkflow() != null) {
      newStorageState = summary.getStorage().getWorkflow().getEnabled();
    }

    if (!previousStorageState && newStorageState) {
      logger.info("Creating workspace for: {}", summary.getId());
      this.controllerClient.createWorkspace(summary.getId());
    }

    if (previousStorageState && !newStorageState) {
      logger.info("Deleting workspace for: {}", summary.getId());
      this.controllerClient.deleteWorkspace(summary.getId());
    }
  }

  private void updateTriggers(final WorkflowEntity entity, Triggers currentTriggers,
      Triggers updatedTriggers) {
    if (currentTriggers == null) {
      currentTriggers = new Triggers();
      entity.setTriggers(currentTriggers);
    }
    if (updatedTriggers != null) {
      String currentTimezone = null;
      boolean previous = false;

      if (currentTriggers.getScheduler() != null) {
        currentTimezone = currentTriggers.getScheduler().getTimezone();
        previous = currentTriggers.getScheduler().getEnable();
      }

      TriggerScheduler scheduler = updatedTriggers.getScheduler();
      updateSchedule(entity, currentTriggers, currentTimezone, previous, scheduler);

      /* Save new triggers. */
      currentTriggers.setSlack(updatedTriggers.getSlack());
      currentTriggers.setWebhook(updatedTriggers.getWebhook());
      currentTriggers.setManual(updatedTriggers.getManual());
      currentTriggers.setDockerhub(updatedTriggers.getDockerhub());
      currentTriggers.setCustom(updatedTriggers.getCustom());

      /* Set new tokens if needed. */
      updateTokenForTrigger(updatedTriggers.getSlack());
      updateTokenForTrigger(updatedTriggers.getWebhook());
      updateTokenForTrigger(updatedTriggers.getDockerhub());
      updateTokenForTrigger(updatedTriggers.getCustom());

    }
  }

  private void updateTokenForTrigger(TriggerEvent updateTrigger) {
    if (updateTrigger != null) {
      boolean enabled = updateTrigger.getEnable();
      if (enabled && updateTrigger.getToken() == null) {
        updateTrigger.setToken(createUUID());
      }
    }
  }

  private void updateSchedule(final WorkflowEntity entity, Triggers previousTriggers,
      String currentTimezone, boolean previous, TriggerScheduler scheduler) {
    if (scheduler != null) {

      String timezone = scheduler.getTimezone();

      if (timezone == null) {
        scheduler.setTimezone(currentTimezone);
      }

      entity.getTriggers().setScheduler(scheduler);

      if (previousTriggers != null && previousTriggers.getScheduler() != null) {
        previousTriggers.getScheduler().getEnable();
      }

      boolean current = scheduler.getEnable();

      scheduleWorkflow(entity, previous, current);
    }
  }

  private void scheduleWorkflow(final WorkflowEntity entity, boolean previous, boolean current) {
    if (!previous && current) {
      this.taskScheduler.scheduleWorkflow(entity);
    } else if (previous && !current) {
      try {
        this.taskScheduler.cancelJob(entity.getId());
      } catch (SchedulerException e) {
        logger.info("Unable to schedule job. ");
        logger.error(e);
      }
    } else if (current) {
      try {
        this.taskScheduler.cancelJob(entity.getId());
        this.taskScheduler.scheduleWorkflow(entity);

      } catch (SchedulerException e) {
        logger.info("Unable to reschedule job. ");
        logger.error(e);
      }
    }
  }

  @Override
  public WorkflowSummary updateWorkflowProperties(String workflowId,
      List<WorkflowProperty> properties) {
    final WorkflowEntity entity = workFlowRepository.getWorkflow(workflowId);

    if (entity.getScope() == WorkflowScope.team) {
      FlowUserEntity user = userIdentityService.getCurrentUser();

      FlowTeam team = teamService
          .getTeamByIdDetailed(workFlowRepository.getWorkflow(workflowId).getFlowTeamId());

      List<String> userIds = new ArrayList<>();
      if (team.getUsers() != null) {
        for (FlowUserEntity teamUser : team.getUsers()) {
          userIds.add(teamUser.getId());
        }
      }

      List<String> userTeamIds = new ArrayList<>();
      if (user.getTeams() != null) {
        for (Team userTeam : user.getTeams()) {
          userTeamIds.add(userTeam.getId());
        }
      }

      if (user.getType() == UserType.admin || user.getType() == UserType.operator
          || userIds.contains(user.getId()) || userTeamIds.contains(team.getHigherLevelGroupId())) {

        entity.setProperties(properties);

        workFlowRepository.saveWorkflow(entity);

        return new WorkflowSummary(entity);
      } else {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
      }
    } else {
      entity.setProperties(properties);
      workFlowRepository.saveWorkflow(entity);
      return new WorkflowSummary(entity);
    }
  }

  @Override
  public GenerateTokenResponse generateTriggerToken(String id, String label) {
    GenerateTokenResponse tokenResponse = new GenerateTokenResponse();
    WorkflowEntity entity = workFlowRepository.getWorkflow(id);
    List<WorkflowToken> tokens = entity.getTokens();
    if (tokens == null) {
      tokens = new LinkedList<>();
      entity.setTokens(tokens);
    }
    WorkflowToken newToken = new WorkflowToken();
    newToken.setLabel(label);
    newToken.setToken(createUUID());
    tokens.add(newToken);
    workFlowRepository.saveWorkflow(entity);

    tokenResponse.setToken(newToken.getToken());

    return tokenResponse;
  }

  @Override
  public ResponseEntity<HttpStatus> validateWorkflowToken(String id,
      GenerateTokenResponse tokenPayload) {
    WorkflowEntity workflow = workFlowRepository.getWorkflow(id);
    if (workflow != null) {
      setupTriggerDefaults(workflow);
      String token = tokenPayload.getToken();
      WorkflowToken workflowToken = workflow.getTokens().stream()
          .filter(customer -> token.equals(customer.getToken())).findAny().orElse(null);
      if (workflowToken != null) {
        return ResponseEntity.ok(HttpStatus.OK);
      }
    }
    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
  }

  private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

  private String createUUID() {
    try {
      MessageDigest salt = MessageDigest.getInstance("SHA-256");
      salt.update(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
      return bytesToHex(salt.digest());
    } catch (NoSuchAlgorithmException e) {

      logger.error(e);
    }

    return "";
  }

  public static String bytesToHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = hexArray[v >>> 4];
      hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }
    return new String(hexChars);
  }

  @Override
  public ResponseEntity<InputStreamResource> exportWorkflow(String workFlowId) {
    final WorkflowEntity entity = workFlowRepository.getWorkflow(workFlowId);
    WorkflowExport export = new WorkflowExport(entity);

    export.setLatestRevision(workflowVersionService.getLatestWorkflowVersion(workFlowId));
    export
        .setRevisionCount(workflowVersionService.getLatestWorkflowVersion(workFlowId).getVersion());

    HttpHeaders headers = new HttpHeaders();
    headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
    headers.add("Pragma", "no-cache");
    headers.add("Expires", "0");

    headers.add("Content-Disposition", "attachment; filename=\"any_name.json\"");

    try {

      ObjectMapper mapper = new ObjectMapper();

      byte[] buf = mapper.writeValueAsBytes(export);

      return ResponseEntity.ok().contentLength(buf.length)
          .contentType(MediaType.parseMediaType("application/octet-stream"))
          .body(new InputStreamResource(new ByteArrayInputStream(buf)));
    } catch (IOException e) {

      logger.error(e);
    }
    return null;

  }

  @Override
  public void importWorkflow(WorkflowExport export, Boolean update, String flowTeamId,
      WorkflowScope scope) {

    List<String> templateIds = new ArrayList<>();


    if (scope == null || scope == WorkflowScope.team) {
      List<FlowTaskTemplateEntity> templates =
          templateService.getAllTaskTemplatesforTeamId(flowTeamId);
      for (FlowTaskTemplateEntity template : templates) {
        templateIds.add(template.getId());
      }
    } else if (scope == WorkflowScope.system) {
      List<FlowTaskTemplateEntity> templates = templateService.getAllTaskTemplatesForSystem();
      for (FlowTaskTemplateEntity template : templates) {
        templateIds.add(template.getId());
      }
    }


    RevisionEntity revision = export.getLatestRevision();
    List<DAGTask> nodes = revision.getDag().getTasks();
    List<String> importTemplateIds = new ArrayList<>();

    for (DAGTask task : nodes) {
      if (task.getType() == TaskType.template) {
        importTemplateIds.add(task.getTemplateId());
      }
    }

    if (templateIds.containsAll(importTemplateIds)) {

      final WorkflowEntity entity = workFlowRepository.getWorkflow(export.getId());

      if (entity != null) {
        if (update != null && !update) {
          entity.setId(null);
          revision.setVersion(1L);
        } else {
          revision.setVersion(
              workflowVersionService.getLatestWorkflowVersion(export.getId()).getVersion() + 1);
        }
        entity.setName(export.getName());
        entity.setDescription(export.getDescription());
        entity.setIcon(export.getIcon());
        entity.setShortDescription(export.getShortDescription());
        entity.setStatus(export.getStatus());
        entity.setStorage(export.getStorage());
        entity.setProperties(export.getProperties());
        entity.setTriggers(export.getTriggers());
        entity.setScope(scope);

        if (WorkflowScope.team.equals(scope)) {
          if (flowTeamId != null && flowTeamId.length() != 0) {
            entity.setFlowTeamId(flowTeamId);
          } else {
            entity.setFlowTeamId(export.getFlowTeamId());
          }
        } else {
          entity.setFlowTeamId(null);
        }

        if (WorkflowScope.user.equals(scope)) {
          FlowUserEntity user = userIdentityService.getCurrentUser();
          if (user != null) {
            entity.setOwnerUserId(user.getId());
          }
        } else {
          entity.setOwnerUserId(null);
        }

        WorkflowEntity workflow = workFlowRepository.saveWorkflow(entity);

        revision.setId(null);
        revision.setWorkFlowId(workflow.getId());

        workflowVersionService.insertWorkflow(revision);
      } else {

        WorkflowEntity newEntity = new WorkflowEntity();
        newEntity.setProperties(export.getProperties());
        newEntity.setDescription(export.getDescription());


        newEntity.setScope(scope);
        if (WorkflowScope.team.equals(scope)) {
          if (flowTeamId != null && flowTeamId.length() != 0) {
            newEntity.setFlowTeamId(flowTeamId);
          } else {
            newEntity.setFlowTeamId(export.getFlowTeamId());
          }
        } else {
          newEntity.setFlowTeamId(null);
        }

        newEntity.setName(export.getName());
        newEntity.setShortDescription(export.getShortDescription());
        newEntity.setStatus(export.getStatus());
        newEntity.setTriggers(export.getTriggers());
        newEntity.setStorage(export.getStorage());
        newEntity.setIcon(export.getIcon());

        WorkflowEntity savedEntity = workFlowRepository.saveWorkflow(newEntity);

        revision.setId(null);
        revision.setVersion(1);
        revision.setWorkFlowId(savedEntity.getId());

        workflowVersionService.insertWorkflow(revision);

      }
    } else {
      String message = "Workflow not imported - template(s) not found";
      logger.info(message);
      throw new BoomerangException(BoomerangError.IMPORT_WORKFLOW_FAILED);
    }
  }

  private List<WorkflowProperty> setupDefaultProperties(WorkflowEntity workflowSummary) {


    List<WorkflowProperty> newProperties = workflowSummary.getProperties();

    if (newProperties == null) {
      newProperties = new LinkedList<>();
    }
    return newProperties;
  }

  @Override
  public boolean canExecuteWorkflowForQuotas(String teamId) {
    if (!flowSettingsService.getConfiguration("features", "workflowQuotas").getBooleanValue()) {
      return true;
    }

    WorkflowQuotas workflowQuotas = teamService.getTeamQuotas(teamId);
    if (workflowQuotas.getCurrentConcurrentWorkflows() >= workflowQuotas.getMaxConcurrentWorkflows()
        || workflowQuotas.getCurrentWorkflowExecutionMonthly() >= workflowQuotas
            .getMaxWorkflowExecutionMonthly()) {
      return false;
    } else {
      return true;
    }
  }

  @Override
  public List<WorkflowShortSummary> getWorkflowShortSummaryList() {
    List<WorkflowShortSummary> summaryList = new LinkedList<>();
    List<WorkflowEntity> workfows = workFlowRepository.getAllWorkflows();
    for (WorkflowEntity workflow : workfows) {

      if (WorkflowStatus.active.equals(workflow.getStatus())) {


        String workflowName = workflow.getName();
        boolean webhookEnabled = false;
        String flowTeamId = workflow.getFlowTeamId();

        if (workflow.getTriggers() != null) {
          Triggers triggers = workflow.getTriggers();
          TriggerEvent webhook = triggers.getWebhook();

          if (webhook != null) {
            webhookEnabled = webhook.getEnable();
          }
        }
        WorkflowShortSummary summary = new WorkflowShortSummary();
        summary.setWebhookEnabled(webhookEnabled);
        summary.setWorkflowName(workflowName);
        summary.setWorkflowId(workflow.getId());

        if (workflow.getScope() != null) {
          summary.setScope(workflow.getScope());
        } else {
          summary.setScope(WorkflowScope.team);
        }

        summary.setTeamId(flowTeamId);

        if (WorkflowScope.system == summary.getScope()) {
          summaryList.add(summary);
        } else if (WorkflowScope.team == summary.getScope()) {
          if (flowTeamId != null) {
            FlowTeam flowTeam = teamService.getTeamById(flowTeamId);
            if (flowTeam != null) {
              summary.setTeamName(flowTeam.getName());
              summaryList.add(summary);
            }
          }
        }
      }
    }
    return summaryList;
  }

  @Override
  public boolean canExecuteWorkflow(String workFlowId, Optional<String> trigger) {


    WorkflowEntity workflow = workFlowRepository.getWorkflow(workFlowId);
    if (!trigger.isPresent() || !"manual".equals(trigger.get())) {
      return true;
    }

    Boolean isActive = teamService.getTeamById(workflow.getFlowTeamId()).getIsActive();
    if (isActive == null || !isActive) {
      return false;
    }

    if (workflow != null) {
      if (workflow.getTriggers() != null) {
        Triggers triggers = workflow.getTriggers();
        if (triggers.getManual() != null) {
          Trigger manualTrigger = triggers.getManual();
          if (manualTrigger != null) {
            return manualTrigger.getEnable();
          }
        }

      }
    }
    return true;
  }

  @Override
  public void deleteToken(String id, String label) {
    WorkflowEntity entity = workFlowRepository.getWorkflow(id);
    List<WorkflowToken> tokens = entity.getTokens();
    if (tokens == null) {
      tokens = new LinkedList<>();
      entity.setTokens(tokens);
    }

    WorkflowToken token = tokens.stream().filter(customer -> label.equals(customer.getLabel()))
        .findAny().orElse(null);

    if (token != null) {
      tokens.remove(token);
    }

    workFlowRepository.saveWorkflow(entity);
  }

  @Override
  public List<WorkflowSummary> getSystemWorkflows() {
    final List<WorkflowEntity> list = workFlowRepository.getSystemWorkflows();

    final List<WorkflowSummary> newList = new LinkedList<>();
    for (final WorkflowEntity entity : list) {

      setupTriggerDefaults(entity);

      final WorkflowSummary summary = new WorkflowSummary(entity);
      updateSummaryInformation(summary);


      if (WorkflowStatus.active == entity.getStatus()) {
        newList.add(summary);
      }

    }
    teamService.updateSummaryWithUpgradeFlags(newList);

    return newList;
  }

  @Override
  public List<WorkflowShortSummary> getSystemWorkflowShortSummaryList() {
    List<WorkflowShortSummary> summaryList = new LinkedList<>();

    List<WorkflowEntity> workfows = workFlowRepository.getSystemWorkflows();
    for (WorkflowEntity workflow : workfows) {

      if (WorkflowStatus.active.equals(workflow.getStatus())) {


        String workflowName = workflow.getName();
        boolean webhookEnabled = false;

        if (workflow.getTriggers() != null) {
          Triggers triggers = workflow.getTriggers();
          TriggerEvent webhook = triggers.getWebhook();

          if (webhook != null) {
            webhookEnabled = webhook.getEnable();
          }
        }
        WorkflowShortSummary summary = new WorkflowShortSummary();
        summary.setWebhookEnabled(webhookEnabled);
        summary.setWorkflowName(workflowName);
        summary.setWorkflowId(workflow.getId());
        summary.setScope(WorkflowScope.system);
        summaryList.add(summary);
      }
    }
    return summaryList;
  }

  @Override
  public List<String> getWorkflowParameters(String workFlowId) {

    RevisionEntity revision = this.workflowVersionService.getLatestWorkflowVersion(workFlowId);

    return buildAvailableParamList(workFlowId, revision);
  }

  private List<String> buildAvailableParamList(String workFlowId, RevisionEntity revision) {
    List<String> parameters = new ArrayList<>();
    WorkflowEntity workflow = workFlowRepository.getWorkflow(workFlowId);

    if (flowSettingsService.getConfiguration("features", "globalParameters").getBooleanValue()) {
      Map<String, String> globalProperties = new HashMap<>();
      propertyManager.buildGlobalProperties(globalProperties);

      for (Map.Entry<String, String> globalProperty : globalProperties.entrySet()) {
        parameters.add("global.params." + globalProperty.getKey());
        parameters.add("params." + globalProperty.getKey());
      }
    }

    if (flowSettingsService.getConfiguration("features", "teamParameters").getBooleanValue()
        && workflow.getScope() != null && WorkflowScope.team.equals(workflow.getScope())) {
      Map<String, String> teamProperties = new HashMap<>();
      propertyManager.buildTeamProperties(teamProperties, workflow.getId());

      for (Map.Entry<String, String> teamProperty : teamProperties.entrySet()) {
        parameters.add("team.params." + teamProperty.getKey());
        parameters.add("params." + teamProperty.getKey());
      }
    }

    Map<String, String> workflowProperties = new HashMap<>();
    propertyManager.buildWorkflowProperties(workflowProperties, null, workflow.getId());
    for (Map.Entry<String, String> workflowProperty : workflowProperties.entrySet()) {
      parameters.add("workflow.params." + workflowProperty.getKey());
      parameters.add("params." + workflowProperty.getKey());
    }

    Map<String, String> systemProperties = new HashMap<>();
    propertyManager.buildSystemProperties(null, null, workflow.getId(), systemProperties);
    for (Map.Entry<String, String> systemProperty : systemProperties.entrySet()) {
      parameters.add("system.params." + systemProperty.getKey());
      parameters.add("params." + systemProperty.getKey());
    }


    if (revision != null) {
      Dag dag = revision.getDag();
      List<DAGTask> dagkTasks = dag.getTasks();
      if (dagkTasks != null) {
        for (DAGTask task : dagkTasks) {
          String taskName = task.getLabel();
          if (task.getTemplateId() != null) {
            String templateId = task.getTemplateId();
            FlowTaskTemplateEntity taskTemplate = templateService.getTaskTemplateWithId(templateId);

            if (taskTemplate != null) {
              if ("templateTask".equals(taskTemplate.getNodetype())) {
                int revisionCount = taskTemplate.getRevisions().size();
                Revision latestRevision = taskTemplate.getRevisions().stream()
                    .filter(x -> x.getVersion() == revisionCount).findFirst().orElse(null);
                if (latestRevision != null) {
                  List<TaskResult> results = latestRevision.getResults();
                  if (results != null) {
                    for (TaskResult result : results) {
                      String key = "tasks." + taskName + ".results." + result.getName();
                      parameters.add(key);
                    }
                  }
                }
              } else {
                List<TaskResult> results = task.getResults();
                if (results != null) {
                  for (TaskResult result : results) {
                    String key = "tasks." + taskName + ".results." + result.getName();
                    parameters.add(key);
                  }
                }
              }
            }
          }
        }
      }

    }

    return parameters;
  }

  @Override
  public List<String> getWorkflowParameters(String workflowId,
      FlowWorkflowRevision workflowSummaryEntity) {

    RevisionEntity revisionEntity = workflowSummaryEntity.convertToEntity();
    return buildAvailableParamList(workflowId, revisionEntity);
  }

  @Override
  public WorkflowSummary duplicateWorkflow(String id, DuplicateRequest duplicateRequest) {
    WorkflowEntity existingWorkflow = workFlowRepository.getWorkflow(id);
    String newName = existingWorkflow.getName() + " (duplicate)";
    existingWorkflow.setId(null);
    existingWorkflow.setName(newName);
    existingWorkflow.setTriggers(null);
    existingWorkflow.setTokens(null);

    if (duplicateRequest != null) {
      if (duplicateRequest.getDescription() != null) {
        existingWorkflow.setDescription(duplicateRequest.getDescription());
      }
      if (duplicateRequest.getName() != null) {
        existingWorkflow.setName(duplicateRequest.getName());
      }
      if (duplicateRequest.getSummary() != null) {
        existingWorkflow.setShortDescription(duplicateRequest.getSummary());
      }
      if (duplicateRequest.getIcon() != null) {
        existingWorkflow.setIcon(duplicateRequest.getIcon());
      }
      if (duplicateRequest.getScope() == WorkflowScope.team
          && duplicateRequest.getTeamId() != null) {
        existingWorkflow.setFlowTeamId(duplicateRequest.getTeamId());
        existingWorkflow.setScope(WorkflowScope.team);
      }
      if (duplicateRequest.getScope() == WorkflowScope.user) {
        FlowUserEntity user = userIdentityService.getCurrentUser();
        existingWorkflow.setOwnerUserId(user.getId());
        existingWorkflow.setScope(WorkflowScope.user);
      }
    }

    WorkflowEntity newWorkflow = this.saveWorkflow(existingWorkflow);

    String newWorkflowId = newWorkflow.getId();

    RevisionEntity revisionEntity = this.workflowVersionService.getLatestWorkflowVersion(id);
    revisionEntity.setId(null);
    revisionEntity.setWorkFlowId(newWorkflowId);
    revisionEntity.setVersion(1l);

    this.workflowVersionService.insertWorkflow(revisionEntity);

    WorkflowSummary updatedSummary = new WorkflowSummary(newWorkflow);
    updateSummaryInformation(updatedSummary);

    return updatedSummary;
  }

  @Override
  public UserWorkflowSummary getUserWorkflows() {
    FlowUserEntity user = userIdentityService.getCurrentUser();

    if (user == null) {
      return null;
    }

    final List<WorkflowEntity> workflows = workFlowRepository.getUserWorkflows(user.getId());

    final List<WorkflowSummary> newList = new LinkedList<>();
    for (final WorkflowEntity entity : workflows) {
      setupTriggerDefaults(entity);
      final WorkflowSummary summary = new WorkflowSummary(entity);
      updateSummaryInformation(summary);
      if (WorkflowStatus.active == entity.getStatus()) {
        newList.add(summary);
      }

    }
    teamService.updateSummaryWithUpgradeFlags(newList);
    UserWorkflowSummary summary = new UserWorkflowSummary();
    summary.setWorkflows(newList);

    WorkflowQuotas quotas = getQuotasForUser(user, workflows);
    summary.setUserQuotas(quotas);

    return summary;
  }


  @Override
  public UserWorkflowSummary getUserWorkflows(String userId) {
    FlowUserEntity user = userIdentityService.getUserByID(userId);

    if (user == null) {
      return null;
    }

    final List<WorkflowEntity> workflows = workFlowRepository.getUserWorkflows(user.getId());

    final List<WorkflowSummary> newList = new LinkedList<>();
    for (final WorkflowEntity entity : workflows) {
      setupTriggerDefaults(entity);
      final WorkflowSummary summary = new WorkflowSummary(entity);
      updateSummaryInformation(summary);
      if (WorkflowStatus.active == entity.getStatus()) {
        newList.add(summary);
      }

    }
    teamService.updateSummaryWithUpgradeFlags(newList);
    UserWorkflowSummary summary = new UserWorkflowSummary();
    summary.setWorkflows(newList);

    WorkflowQuotas quotas = getQuotasForUser(user, workflows);
    summary.setUserQuotas(quotas);

    return summary;
  }


  private WorkflowQuotas getQuotasForUser(FlowUserEntity user, List<WorkflowEntity> workflows) {

    final String configurationKey = "users";

    int maxUserWorkflowCount = Integer.parseInt(flowSettingsService
        .getConfiguration(configurationKey, "max.user.workflow.count").getValue());
    int maxExecutionsMonthly = Integer.parseInt(flowSettingsService
        .getConfiguration(configurationKey, "max.user.workflow.execution.monthly").getValue());
    int maxConcurrentExecutions = Integer.parseInt(flowSettingsService
        .getConfiguration(configurationKey, "max.user.concurrent.workflows").getValue());
    int maxWorkflowDuration = Integer.parseInt(flowSettingsService
        .getConfiguration(configurationKey, "max.user.workflow.duration").getValue());


    Quotas quotas = setTeamQuotas(user);

    Pageable page = Pageable.unpaged();
    List<ActivityEntity> concurrentActivities = getConcurrentWorkflowActivities(workflows);
    List<ActivityEntity> activitiesMonthly = getMonthlyWorkflowActivities(page, user.getId());

    WorkflowQuotas workflowQuotas = new WorkflowQuotas();
    workflowQuotas.setMaxWorkflowCount(maxUserWorkflowCount);
    workflowQuotas.setMaxWorkflowExecutionMonthly(maxExecutionsMonthly);
    workflowQuotas.setMaxWorkflowStorage(quotas.getMaxWorkflowStorage());
    workflowQuotas.setMaxWorkflowExecutionTime(maxWorkflowDuration);
    workflowQuotas.setMaxConcurrentWorkflows(maxConcurrentExecutions);


    workflowQuotas.setCurrentWorkflowCount(workflows.size());
    workflowQuotas.setCurrentConcurrentWorkflows(concurrentActivities.size());
    workflowQuotas.setCurrentWorkflowExecutionMonthly(activitiesMonthly.size());

    setWorkflowResetDate(workflowQuotas);
    return workflowQuotas;
  }

  private void setWorkflowResetDate(WorkflowQuotas workflowQuotas) {
    Calendar nextMonth = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    nextMonth.add(Calendar.MONTH, 1);
    nextMonth.set(Calendar.DAY_OF_MONTH, 1);
    nextMonth.set(Calendar.HOUR_OF_DAY, 0);
    nextMonth.set(Calendar.MINUTE, 0);
    nextMonth.set(Calendar.SECOND, 0);
    nextMonth.set(Calendar.MILLISECOND, 0);
    workflowQuotas.setMonthlyResetDate(nextMonth.getTime());
  }

  private Quotas setTeamQuotas(FlowUserEntity team) {
    if (team.getQuotas() == null) {
      team.setQuotas(new Quotas());
    }

    Quotas quotas = new Quotas();

    if (team.getQuotas().getMaxWorkflowCount() != null) {
      quotas.setMaxWorkflowCount(team.getQuotas().getMaxWorkflowCount());
    } else {
      quotas.setMaxWorkflowCount(maxWorkflowCount);
    }
    if (team.getQuotas().getMaxWorkflowExecutionMonthly() != null) {
      quotas.setMaxWorkflowExecutionMonthly(team.getQuotas().getMaxWorkflowExecutionMonthly());
    } else {
      quotas.setMaxWorkflowExecutionMonthly(maxWorkflowExecutionMonthly);
    }
    if (team.getQuotas().getMaxWorkflowStorage() != null) {
      quotas.setMaxWorkflowStorage(team.getQuotas().getMaxWorkflowStorage());
    } else {
      quotas.setMaxWorkflowStorage(maxWorkflowStorage);
    }
    if (team.getQuotas().getMaxWorkflowExecutionTime() != null) {
      quotas.setMaxWorkflowExecutionTime(team.getQuotas().getMaxWorkflowExecutionTime());
    } else {
      quotas.setMaxWorkflowExecutionTime(maxWorkflowExecutionTime);
    }
    if (team.getQuotas().getMaxConcurrentWorkflows() != null) {
      quotas.setMaxConcurrentWorkflows(team.getQuotas().getMaxConcurrentWorkflows());
    } else {
      quotas.setMaxConcurrentWorkflows(maxConcurrentWorkflows);
    }
    return quotas;
  }

  private List<ActivityEntity> getConcurrentWorkflowActivities(List<WorkflowEntity> workflows) {
    List<String> workflowIds = new ArrayList<>();
    for (WorkflowEntity workflow : workflows) {
      workflowIds.add(workflow.getId());
    }
    return flowWorkflowActivityService.findbyWorkflowIdsAndStatus(workflowIds,
        TaskStatus.inProgress);
  }

  private List<ActivityEntity> getMonthlyWorkflowActivities(Pageable page, String userId) {
    Calendar c = Calendar.getInstance();
    c.set(Calendar.DAY_OF_MONTH, 1);
    return flowWorkflowActivityService
        .findAllActivitiesForUser(Optional.of(c.getTime()), Optional.of(new Date()), userId, page)
        .getContent();
  }

  @Override
  public boolean canExecuteWorkflowForQuotasForUser(String workflowId) {

    if (!flowSettingsService.getConfiguration("features", "workflowQuotas").getBooleanValue()) {
      return true;
    }

    UserWorkflowSummary summary =
        getUserWorkflows(workFlowRepository.getWorkflow(workflowId).getOwnerUserId());

    WorkflowQuotas workflowQuotas = summary.getUserQuotas();
    if (workflowQuotas.getCurrentConcurrentWorkflows() >= workflowQuotas.getMaxConcurrentWorkflows()
        || workflowQuotas.getCurrentWorkflowExecutionMonthly() >= workflowQuotas
            .getMaxWorkflowExecutionMonthly()) {

      return false;
    } else {
      return true;
    }
  }

  @Override
  public List<TemplateWorkflowSummary> getTemplateWorkflows() {
    List<WorkflowEntity> workflows = this.workFlowRepository.getTemplateWorkflows();
    List<TemplateWorkflowSummary> summaryList = new LinkedList<>();

    for (WorkflowEntity workflow : workflows) {
      if (WorkflowStatus.active == workflow.getStatus()) {
        TemplateWorkflowSummary summary = new TemplateWorkflowSummary();
        summary.setId(workflow.getId());
        summary.setDescription(workflow.getDescription());
        summary.setIcon(workflow.getIcon());
        summary.setParameters(workflow.getProperties());
        summary.setName(workflow.getName());
        summary.setSummary(workflow.getShortDescription());

        RevisionEntity revision =
            this.workflowVersionService.getLatestWorkflowVersion(workflow.getId());
        WorkflowRevision workflowRevision = ModelConverterV5.convertToRestModel(revision);
        summary.setRevision(workflowRevision);
        summaryList.add(summary);
      }
    }
    return summaryList;
  }

  @Override
  public CronValidationResponse validateCron(String cronString) {

    logger.info("CRON: {}", cronString);

    CronValidationResponse response = new CronValidationResponse();
    CronParser parser =
        new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));
    try {
      cronString = parser.parse(cronString).asString();
      response.setCron(cronString);
      response.setValid(true);
      logger.info("Final CRON: {} .", cronString);
    } catch (IllegalArgumentException e) {
      logger.info("Invalid CRON: {} . Attempting cron to quartz conversion", cronString);
      parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.CRON4J));
      try {
        Cron cron = parser.parse(cronString);
        CronMapper quartzMapper = CronMapper.fromCron4jToQuartz();
        Cron quartzCron = quartzMapper.map(cron);
        cronString = quartzCron.asString();
        response.setCron(cronString);
        response.setValid(true);
      } catch (IllegalArgumentException exc) {
        logger.info("Invalid CRON: {} . Cannot convert", cronString);
        response.setCron(null);
        response.setValid(false);
        response.setMessage(e.getMessage());
      }

      logger.info("Final CRON: {} .", cronString);
    }
    return response;

  }

}
