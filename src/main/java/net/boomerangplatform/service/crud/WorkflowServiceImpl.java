package net.boomerangplatform.service.crud;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.boomerangplatform.model.GenerateTokenResponse;
import net.boomerangplatform.model.WorkflowExport;
import net.boomerangplatform.model.WorkflowSummary;
import net.boomerangplatform.mongo.entity.FlowTaskTemplateEntity;
import net.boomerangplatform.mongo.entity.FlowWorkflowEntity;
import net.boomerangplatform.mongo.entity.FlowWorkflowRevisionEntity;
import net.boomerangplatform.mongo.model.Event;
import net.boomerangplatform.mongo.model.FlowProperty;
import net.boomerangplatform.mongo.model.Scheduler;
import net.boomerangplatform.mongo.model.TaskType;
import net.boomerangplatform.mongo.model.Triggers;
import net.boomerangplatform.mongo.model.Webhook;
import net.boomerangplatform.mongo.model.WorkflowQuotas;
import net.boomerangplatform.mongo.model.WorkflowStatus;
import net.boomerangplatform.mongo.model.next.DAGTask;
import net.boomerangplatform.mongo.service.FlowTaskTemplateService;
import net.boomerangplatform.mongo.service.FlowWorkflowService;
import net.boomerangplatform.mongo.service.FlowWorkflowVersionService;
import net.boomerangplatform.scheduler.ScheduledTasks;

@Service
public class WorkflowServiceImpl implements WorkflowService {

  @Autowired
  private ScheduledTasks taskScheduler;

  @Autowired
  private FlowWorkflowService workFlowRepository;

  @Autowired
  private FlowWorkflowVersionService workflowVersionService;

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
    final FlowWorkflowEntity entity = workFlowRepository.getWorkflow(workFlowid);
    entity.setStatus(WorkflowStatus.deleted);
    workFlowRepository.saveWorkflow(entity);

    if (entity.getTriggers() != null) {
      Triggers trigger = entity.getTriggers();
      if (trigger != null) {
        Scheduler scheduler = trigger.getScheduler();
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

    final FlowWorkflowEntity entity = workFlowRepository.getWorkflow(workflowId);

    if (entity.getTriggers().getEvent() == null) {
      Event event = new Event();
      event.setEnable(false);
      event.setTopic("");
      entity.getTriggers().setEvent(event);
    }
    final WorkflowSummary summary = new WorkflowSummary(entity);
    updateSummaryInformation(summary);
    return summary;
  }

  @Override
  public List<WorkflowSummary> getWorkflowsForTeam(String flowTeamId) {
    final List<FlowWorkflowEntity> list = workFlowRepository.getWorkflowsForTeams(flowTeamId);
    final List<WorkflowSummary> newList = new LinkedList<>();
    for (final FlowWorkflowEntity entity : list) {
      final WorkflowSummary summary = new WorkflowSummary(entity);
      updateSummaryInformation(summary);

      if (WorkflowStatus.active == entity.getStatus()) {
        newList.add(summary);
      }

    }
    return newList;
  }

  @Override
  public WorkflowSummary saveWorkflow(final FlowWorkflowEntity flowWorkflowEntity) {

    final FlowWorkflowEntity entity = workFlowRepository.saveWorkflow(flowWorkflowEntity);
    final WorkflowSummary summary = new WorkflowSummary(entity);

    if (summary.getTriggers() != null) {
      Triggers trigger = summary.getTriggers();

      Scheduler scheduler = trigger.getScheduler();
      if (scheduler != null && scheduler.getEnable()) {
        logger.info("Scheduling workflow: {}", scheduler.getSchedule());
        this.taskScheduler.scheduleWorkflow(entity);
      }
    }

    return summary;
  }

  private void updateSummaryInformation(final WorkflowSummary entity) {
    final String workflowId = entity.getId();
    final long versionCount = workflowVersionService.getWorkflowCount(workflowId);
    entity.setRevisionCount(versionCount);
  }

  @Override
  public WorkflowSummary updateWorkflow(WorkflowSummary summary) {
    final FlowWorkflowEntity entity = workFlowRepository.getWorkflow(summary.getId());

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

  private void updateTriggers(final FlowWorkflowEntity entity, Triggers previousTriggers,
      Triggers trigger) {
    if (trigger != null) {

      String currentToken = null;
      String currentTimezone = null;
      String currentTopic = null;
      boolean previous = false;

      if (previousTriggers != null && previousTriggers.getWebhook() != null) {
        currentToken = previousTriggers.getWebhook().getToken();
      }

      if (previousTriggers != null && previousTriggers.getScheduler() != null) {
        currentTimezone = previousTriggers.getScheduler().getTimezone();
        previous = previousTriggers.getScheduler().getEnable();
      }

      if (previousTriggers != null && previousTriggers.getEvent() != null) {
        currentTopic = previousTriggers.getEvent().getTopic();
      }

      Event event = trigger.getEvent();
      updateEvent(entity, currentTopic, event);

      Webhook webhook = trigger.getWebhook();
      updateWebhook(entity, currentToken, webhook);

      Scheduler scheduler = trigger.getScheduler();
      updateSchedule(entity, previousTriggers, currentTimezone, previous, scheduler);

    }
  }

  private void updateEvent(final FlowWorkflowEntity entity, String currentTopic, Event event) {
    if (event != null) {
      String topic = event.getTopic();

      if (topic == null) {
        event.setTopic(currentTopic);
      }

      if (event.getEnable() == null) {
        event.setEnable(false);
      }

      entity.getTriggers().setEvent(event);
    } else {
      event = new Event();
      event.setEnable(false);
      event.setTopic("");
      entity.getTriggers().setEvent(event);
    }
  }

  private void updateWebhook(final FlowWorkflowEntity entity, String currentToken,
      Webhook webhook) {
    if (webhook != null) {
      boolean enabled = webhook.getEnable();

      if (enabled && currentToken == null) {
        webhook.setToken(createUUID());
      } else {
        webhook.setToken(currentToken);
      }

      entity.getTriggers().setWebhook(webhook);
    }
  }

  private void updateSchedule(final FlowWorkflowEntity entity, Triggers previousTriggers,
      String currentTimezone, boolean previous, Scheduler scheduler) {
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

  private void scheduleWorkflow(final FlowWorkflowEntity entity, boolean previous,
      boolean current) {
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
    final FlowWorkflowEntity entity = workFlowRepository.getWorkflow(workflowId);
    entity.setProperties(properties);

    workFlowRepository.saveWorkflow(entity);

    return new WorkflowSummary(entity);
  }

  @Override
  public GenerateTokenResponse generateWebhookToken(String id) {
    GenerateTokenResponse tokenResponse = new GenerateTokenResponse();
    FlowWorkflowEntity entity = workFlowRepository.getWorkflow(id);
    Triggers trigger = entity.getTriggers();
    if (trigger != null) {
      String newToken = createUUID();
      Webhook webhook = trigger.getWebhook();
      webhook.setToken(newToken);

      tokenResponse.setToken(newToken);

    }
    workFlowRepository.saveWorkflow(entity);

    return tokenResponse;
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
    final FlowWorkflowEntity entity = workFlowRepository.getWorkflow(workFlowId);
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

    FlowWorkflowRevisionEntity revision = export.getLatestRevision();
    List<DAGTask> nodes = revision.getDag().getTasks();
    List<String> importTemplateIds = new ArrayList<>();

    for (DAGTask task : nodes) {
      if (task.getType() == TaskType.template) {
        importTemplateIds.add(task.getTemplateId());
      }
    }

    if (templateIds.containsAll(importTemplateIds)) {

      if (update.equals(true)) {
        final FlowWorkflowEntity entity = workFlowRepository.getWorkflow(export.getId());

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
        FlowWorkflowEntity entity = new FlowWorkflowEntity();
        entity.setProperties(export.getProperties());
        entity.setDescription(export.getDescription());

        if (flowTeamId != null && flowTeamId.length() != 0) {
          entity.setFlowTeamId(flowTeamId);
        } else {
          entity.setFlowTeamId(export.getFlowTeamId());
        }

        entity.setName(export.getName());
        entity.setShortDescription(export.getShortDescription());
        entity.setStatus(export.getStatus());
        entity.setTriggers(export.getTriggers());
        entity.setEnablePersistentStorage(export.isEnablePersistentStorage());
        entity.setIcon(export.getIcon());

        FlowWorkflowEntity savedEntity = workFlowRepository.saveWorkflow(entity);

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

  private List<FlowProperty> setupDefaultProperties(FlowWorkflowEntity workflowSummary) {

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
  public boolean canExecuteWorkflow(String teamId) {
    WorkflowQuotas workflowQuotas = teamService.getTeamQuotas(teamId);
    if(workflowQuotas.getCurrentWorkflowCount() > maxWorkflowCount || 
        workflowQuotas.getCurrentConcurrentWorkflows() > maxConcurrentWorkflows ||
        workflowQuotas.getCurrentWorkflowExecutionMonthly() > maxWorkflowExecutionMonthly) {
      return false;
    } else {
      return true;
    }
  }

}
