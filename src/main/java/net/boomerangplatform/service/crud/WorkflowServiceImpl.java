package net.boomerangplatform.service.crud;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.boomerangplatform.model.FlowTeam;
import net.boomerangplatform.model.GenerateTokenResponse;
import net.boomerangplatform.model.WorkflowExport;
import net.boomerangplatform.model.WorkflowQuotas;
import net.boomerangplatform.model.WorkflowShortSummary;
import net.boomerangplatform.model.WorkflowSummary;
import net.boomerangplatform.model.WorkflowToken;
import net.boomerangplatform.mongo.entity.FlowTaskTemplateEntity;
import net.boomerangplatform.mongo.entity.RevisionEntity;
import net.boomerangplatform.mongo.entity.WorkflowEntity;
import net.boomerangplatform.mongo.model.FlowProperty;
import net.boomerangplatform.mongo.model.FlowTriggerEnum;
import net.boomerangplatform.mongo.model.TaskType;
import net.boomerangplatform.mongo.model.Trigger;
import net.boomerangplatform.mongo.model.TriggerEvent;
import net.boomerangplatform.mongo.model.TriggerScheduler;
import net.boomerangplatform.mongo.model.Triggers;
import net.boomerangplatform.mongo.model.WorkflowStatus;
import net.boomerangplatform.mongo.model.next.DAGTask;
import net.boomerangplatform.mongo.service.FlowTaskTemplateService;
import net.boomerangplatform.mongo.service.FlowWorkflowService;
import net.boomerangplatform.mongo.service.RevisionService;
import net.boomerangplatform.scheduler.ScheduledTasks;

@Service
public class WorkflowServiceImpl implements WorkflowService {

  @Autowired
  private ScheduledTasks taskScheduler;

  @Autowired
  private FlowWorkflowService workFlowRepository;

  @Autowired
  private RevisionService workflowVersionService;

  @Autowired
  private FlowTaskTemplateService templateService;

  @Autowired
  private TeamService teamService;

  @Value("${max.workflow.count}")
  private Integer maxWorkflowCount;

  @Value("${max.workflow.execution.monthly}")
  private Integer maxWorkflowExecutionMonthly;

  @Value("${max.concurrent.workflows}")
  private Integer maxConcurrentWorkflows;

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
    entity.setEnablePersistentStorage(summary.isEnablePersistentStorage());
    entity.setEnableACCIntegration(summary.isEnableACCIntegration());

    List<FlowProperty> updatedProperties = setupDefaultProperties(summary);
    entity.setProperties(updatedProperties);
    Triggers previousTriggers = entity.getTriggers();
    Triggers trigger = summary.getTriggers();

    updateTriggers(entity, previousTriggers, trigger);

    workFlowRepository.saveWorkflow(entity);
    WorkflowSummary updatedSummary = new WorkflowSummary(entity);
    updateSummaryInformation(updatedSummary);

    return updatedSummary;

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
      List<FlowProperty> properties) {
    final WorkflowEntity entity = workFlowRepository.getWorkflow(workflowId);
    entity.setProperties(properties);

    workFlowRepository.saveWorkflow(entity);

    return new WorkflowSummary(entity);
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
    setupTriggerDefaults(workflow);
    String token = tokenPayload.getToken();
    WorkflowToken workflowToken = workflow.getTokens().stream()
        .filter(customer -> token.equals(customer.getToken())).findAny().orElse(null);
    if (workflowToken != null) {
      return ResponseEntity.ok(HttpStatus.OK);
    }
    return ResponseEntity.ok(HttpStatus.FORBIDDEN);
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
  public void importWorkflow(WorkflowExport export, Boolean update, String flowTeamId) {

    List<FlowTaskTemplateEntity> templates = templateService.getAllTaskTemplates();
    List<String> templateIds = new ArrayList<>();
    for (FlowTaskTemplateEntity template : templates) {
      templateIds.add(template.getId());

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

        entity.setName(export.getName());
        entity.setDescription(export.getDescription());
        entity.setIcon(export.getIcon());
        entity.setShortDescription(export.getShortDescription());
        entity.setStatus(export.getStatus());
        entity.setEnablePersistentStorage(export.isEnablePersistentStorage());
        entity.setProperties(export.getProperties());
        entity.setTriggers(export.getTriggers());

        workFlowRepository.saveWorkflow(entity);

        revision.setId(null);
        revision.setVersion(
            workflowVersionService.getLatestWorkflowVersion(export.getId()).getVersion() + 1);

        workflowVersionService.insertWorkflow(revision);
      } else {

        WorkflowEntity newEntity = new WorkflowEntity();
        newEntity.setProperties(export.getProperties());
        newEntity.setDescription(export.getDescription());

        if (flowTeamId != null && flowTeamId.length() != 0) {
          newEntity.setFlowTeamId(flowTeamId);
        } else {
          newEntity.setFlowTeamId(export.getFlowTeamId());
        }

        newEntity.setName(export.getName());
        newEntity.setShortDescription(export.getShortDescription());
        newEntity.setStatus(export.getStatus());
        newEntity.setTriggers(export.getTriggers());
        newEntity.setEnablePersistentStorage(export.isEnablePersistentStorage());
        newEntity.setIcon(export.getIcon());

        WorkflowEntity savedEntity = workFlowRepository.saveWorkflow(newEntity);

        revision.setId(null);
        revision.setVersion(1);
        revision.setWorkFlowId(savedEntity.getId());

        workflowVersionService.insertWorkflow(revision);

      }
    } else {
      logger.info("Workflow not imported - template(s) not found");
      throw new IllegalArgumentException("Workflow not imported - template(s) not found");
    }

  }

  private List<FlowProperty> setupDefaultProperties(WorkflowEntity workflowSummary) {

    String[] iamKeys = {"execution_id"};

    boolean enabled = workflowSummary.isEnableACCIntegration();

    List<FlowProperty> newProperties = workflowSummary.getProperties();

    if (newProperties == null) {
      newProperties = new LinkedList<>();
    }

    if (enabled) {
      /* Add new Inputs. */
      for (String key : iamKeys) {
        /* Check to see if key already exists. */
        boolean idExists = newProperties.stream().anyMatch(t -> t.getKey().equals(key));
        if (!idExists) {
          FlowProperty newProperty = new FlowProperty();
          newProperty.setReadOnly(true);
          newProperty.setLabel(key);
          newProperty.setKey(key);
          newProperty.setType("text");
          newProperty.setRequired(true);

          newProperties.add(newProperty);
        }
      }
    } else {
      /* Removed existing ones. */
      for (String key : iamKeys) {
        /* Check to see if key already exists. */
        boolean idExists = newProperties.stream().anyMatch(t -> t.getKey().equals(key));
        if (idExists) {
          newProperties.removeIf(t -> t.getKey().equals(key));
        }
      }
    }

    return newProperties;
  }

  @Override
  public boolean canExecuteWorkflowForQuotas(String teamId) {
    WorkflowQuotas workflowQuotas = teamService.getTeamQuotas(teamId);
    if (workflowQuotas.getCurrentConcurrentWorkflows() > workflowQuotas.getMaxConcurrentWorkflows()
        || workflowQuotas.getCurrentWorkflowExecutionMonthly() > workflowQuotas
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
        String token = null;
        if (workflow.getTriggers() != null) {
          Triggers triggers = workflow.getTriggers();
          TriggerEvent webhook = triggers.getWebhook();

          if (webhook != null) {
            webhookEnabled = webhook.getEnable();
          }
        }
        WorkflowShortSummary summary = new WorkflowShortSummary();
        summary.setToken(token);
        summary.setWebhookEnabled(webhookEnabled);
        summary.setWorkflowName(workflowName);
        summary.setWorkflowId(workflow.getId());

        summary.setTeamId(flowTeamId);

        if (flowTeamId != null) {
          FlowTeam flowTeam = teamService.getTeamById(flowTeamId);
          if (flowTeam != null) {
            summary.setTeamName(flowTeam.getName());
            summaryList.add(summary);
          }
        }
      }
    }
    return summaryList;
  }

  @Override
  public boolean canExecuteWorkflow(String workFlowId, Optional<String> trigger) {

    if (!trigger.isPresent() || !"manual".equals(trigger.get())) {
      return true;
    }

    WorkflowEntity workflow = workFlowRepository.getWorkflow(workFlowId);
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
}
