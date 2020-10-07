package net.boomerangplatform.mongo.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import net.boomerangplatform.mongo.entity.ActivityEntity;
import net.boomerangplatform.mongo.model.TaskStatus;
import net.boomerangplatform.mongo.model.converter.FlowTaskStatusConverter;
import net.boomerangplatform.mongo.model.converter.FlowTriggerEnumConverter;
import net.boomerangplatform.mongo.repository.FlowWorkflowActivityRepository;

@Service
public class FlowWorkflowActivityServiceImpl implements FlowWorkflowActivityService {

  @Autowired
  private FlowWorkflowActivityRepository repository;

  @Override
  public Page<ActivityEntity> findAllActivities(Optional<Date> fromDate,
      Optional<Date> toDate, Pageable page, Optional<String> workflowId) {
    if (workflowId.isPresent()) {
      return repository.findByworkflowId(workflowId.get(), page);
    } else if (fromDate.isPresent() && toDate.isPresent()) {
      return repository.findAll(page);
    } else {
      return repository.findAll(page);
    }
  }

  @Override
  public Page<ActivityEntity> findAllActivities(Optional<Date> fromDate,
      Optional<Date> toDate, Pageable page) {
    if (fromDate.isPresent() && toDate.isPresent()) {
      return repository.findAll(fromDate.get(), toDate.get(), page);
    } else {
      return repository.findAll(page);
    }
  }

  @Override
  public Page<ActivityEntity> findAllActivitiesForWorkflows(Optional<Date> fromDate,
      Optional<Date> toDate, List<String> workflows, Pageable page) {
    return repository.findAll(page);
  }

  @Override
  public ActivityEntity findWorkflowActivtyById(String id) {
    return repository.findById(id).orElse(null);
  }

  @Override
  public ActivityEntity saveWorkflowActivity(ActivityEntity entity) {
    return repository.save(entity);

  }

  @Override
  public Page<ActivityEntity> getAllActivites(Optional<Date> from, Optional<Date> to,
      Pageable page, Optional<List<String>> workflowIds, Optional<List<String>> statuses,
      Optional<List<String>> triggers) {

    if (workflowIds.isPresent()) {
      return findActivityWithWorkflowIds(from, to, page, workflowIds.get(), statuses, triggers);
    }

    else {
      return findActivityWithoutWorkflowIds(from, to, page, statuses, triggers);
    }

  }

  private Page<ActivityEntity> findActivityWithWorkflowIds(Optional<Date> from,
      Optional<Date> to, Pageable page, List<String> workflowIds, Optional<List<String>> statuses,
      Optional<List<String>> triggers) {

    if (statuses.isPresent()) {
      return findActivityWithWorkflowIdsAndStatuses(from, to, page, workflowIds, statuses.get(),
          triggers);
    } else {
      return findActivityWithWorkflowIdsAndWithoutStatuses(from, to, page, workflowIds, triggers);
    }
  }

  private Page<ActivityEntity> findActivityWithWorkflowIdsAndWithoutStatuses(
      Optional<Date> from, Optional<Date> to, Pageable page, List<String> workflowIds,
      Optional<List<String>> triggers) {
    if (triggers.isPresent() && !from.isPresent() && !to.isPresent()) {
      return repository.findByWorkflowIdInAndTriggerIn(workflowIds,
          FlowTriggerEnumConverter.convert(triggers.get()), page);
    } else if (!triggers.isPresent() && from.isPresent() && !to.isPresent()) {
      return repository.findByWorkflowIdInAndCreationDateAfter(workflowIds, from.get(), page);
    } else if (triggers.isPresent() && from.isPresent() && !to.isPresent()) {
      return repository.findByWorkflowIdInAndTriggerInAndCreationDateAfter(workflowIds,
          FlowTriggerEnumConverter.convert(triggers.get()), from.get(), page);
    } else if (!triggers.isPresent() && from.isPresent()) {
      return repository.findByWorkflowIdInAndCreationDateBetween(workflowIds, from.get(), to.get(),
          page);
    } else if (triggers.isPresent() && from.isPresent()) {
      return repository.findByWorkflowIdInAndTriggerInAndCreationDateBetween(workflowIds,
          FlowTriggerEnumConverter.convert(triggers.get()), from.get(), to.get(), page);
    } else if (!triggers.isPresent() && to.isPresent()) {
      return repository.findByWorkflowIdInAndCreationDateBefore(workflowIds, to.get(), page);
    } else if (triggers.isPresent()) {
      return repository.findByWorkflowIdInAndTriggerInAndCreationDateBefore(workflowIds,
          FlowTriggerEnumConverter.convert(triggers.get()), to.get(), page);
    } else {
      return repository.findByWorkflowIdIn(workflowIds, page);
    }

  }

  private Page<ActivityEntity> findActivityWithWorkflowIdsAndStatuses(
      Optional<Date> from, Optional<Date> to, Pageable page, List<String> workflowIds,
      List<String> statuses, Optional<List<String>> triggers) {

    if (triggers.isPresent()) {
      return findActivityWithWorkflowIdsAndStatusesAndTriggers(from, to, page, workflowIds,
          statuses, triggers.get());
    }

    else {
      return findActivityWithWorkflowIdsAndStatusesAndWithoutTriggers(from, to, page, workflowIds,
          statuses);
    }
  }

  private Page<ActivityEntity> findActivityWithWorkflowIdsAndStatusesAndWithoutTriggers(
      Optional<Date> from, Optional<Date> to, Pageable page, List<String> workflowIds,
      List<String> statuses) {
    if (!from.isPresent() && !to.isPresent()) {
      return repository.findByWorkflowIdInAndStatusIn(workflowIds,
          FlowTaskStatusConverter.convert(statuses), page);
    } else if (from.isPresent() && !to.isPresent()) {
      return repository.findByWorkflowIdInAndStatusInAndCreationDateAfter(workflowIds,
          FlowTaskStatusConverter.convert(statuses), from.get(), page);
    } else if (from.isPresent()) {
      return repository.findByWorkflowIdInAndStatusInAndCreationDateBetween(workflowIds,
          FlowTaskStatusConverter.convert(statuses), from.get(), to.get(), page);
    } else {
      return repository.findByWorkflowIdInAndStatusInAndCreationDateBefore(workflowIds,
          FlowTaskStatusConverter.convert(statuses), to.get(), page);
    }
  }

  private Page<ActivityEntity> findActivityWithWorkflowIdsAndStatusesAndTriggers(
      Optional<Date> from, Optional<Date> to, Pageable page, List<String> workflowIds,
      List<String> statuses, List<String> triggers) {
    if (!from.isPresent() && to.isPresent()) {
      return repository.findByWorkflowIdInAndStatusInAndTriggerInAndCreationDateBefore(workflowIds,
          FlowTaskStatusConverter.convert(statuses), FlowTriggerEnumConverter.convert(triggers),
          to.get(), page);
    }

    else if (from.isPresent() && !to.isPresent()) {
      return repository.findByWorkflowIdInAndStatusInAndTriggerInAndCreationDateAfter(workflowIds,
          FlowTaskStatusConverter.convert(statuses), FlowTriggerEnumConverter.convert(triggers),
          from.get(), page);
    } else if (from.isPresent()) {
      return repository.findByWorkflowIdInAndStatusInAndTriggerInAndCreationDateBetween(workflowIds,
          FlowTaskStatusConverter.convert(statuses), FlowTriggerEnumConverter.convert(triggers),
          from.get(), to.get(), page);
    } else {
      return repository.findByWorkflowIdInAndStatusInAndTriggerIn(workflowIds,
          FlowTaskStatusConverter.convert(statuses), FlowTriggerEnumConverter.convert(triggers),
          page);
    }
  }

  private Page<ActivityEntity> findActivityWithoutWorkflowIds(Optional<Date> from,
      Optional<Date> to, Pageable page, Optional<List<String>> statuses,
      Optional<List<String>> triggers) {
    if (statuses.isPresent()) {
      return findActivityWithoutWorkflowIdsAndWithStatuses(from, to, page, statuses.get(),
          triggers);
    } else {
      return findActivityWithoutWorkflowIdsAndWithoutStatuses(from, to, page, triggers);
    }
  }


  private Page<ActivityEntity> findActivityWithoutWorkflowIdsAndWithStatuses(
      Optional<Date> from, Optional<Date> to, Pageable page, List<String> statuses,
      Optional<List<String>> triggers) {
    if (triggers.isPresent()) {
      return findActivityWithoutWorkflowIdsAndWithStatusesAndWithTriggers(from, to, page, statuses,
          triggers.get());
    } else {
      return findActivityWithoutWorkflowIdsandWithStatusAndWithoutTriggers(from, to, page,
          statuses);
    }
  }

  private Page<ActivityEntity> findActivityWithoutWorkflowIdsandWithStatusAndWithoutTriggers(
      Optional<Date> from, Optional<Date> to, Pageable page, List<String> statuses) {
    if (!from.isPresent() && !to.isPresent()) {
      return repository.findByStatusIn(FlowTaskStatusConverter.convert(statuses), page);
    } else if (from.isPresent() && !to.isPresent()) {
      return repository.findByStatusInAndCreationDateAfter(
          FlowTaskStatusConverter.convert(statuses), from.get(), page);
    } else if (!from.isPresent()) {
      return repository.findByStatusInAndCreationDateBefore(
          FlowTaskStatusConverter.convert(statuses), to.get(), page);
    } else {
      return repository.findByStatusInAndCreationDateBetween(
          FlowTaskStatusConverter.convert(statuses), from.get(), to.get(), page);
    }
  }

  private Page<ActivityEntity> findActivityWithoutWorkflowIdsAndWithStatusesAndWithTriggers(
      Optional<Date> from, Optional<Date> to, Pageable page, List<String> statuses,
      List<String> triggers) {
    if (from.isPresent() && !to.isPresent()) {
      return repository.findByStatusInAndTriggerInAndCreationDateAfter(
          FlowTaskStatusConverter.convert(statuses), FlowTriggerEnumConverter.convert(triggers),
          from.get(), page);
    } else if (!from.isPresent() && to.isPresent()) {
      return repository.findByStatusInAndTriggerInAndCreationDateBefore(
          FlowTaskStatusConverter.convert(statuses), FlowTriggerEnumConverter.convert(triggers),
          to.get(), page);
    } else if (from.isPresent()) {
      return repository.findByStatusInAndTriggerInAndCreationDateBetween(
          FlowTaskStatusConverter.convert(statuses), FlowTriggerEnumConverter.convert(triggers),
          from.get(), to.get(), page);
    } else {
      return repository.findByStatusInAndTriggerIn(FlowTaskStatusConverter.convert(statuses),
          FlowTriggerEnumConverter.convert(triggers), page);
    }
  }

  private Page<ActivityEntity> findActivityWithoutWorkflowIdsAndWithoutStatuses(
      Optional<Date> from, Optional<Date> to, Pageable page, Optional<List<String>> triggers) {

    if (triggers.isPresent()) {
      return findActivityWithoutWorkflowIdsAndWithoutStatusesAndWithTriggers(from, to, page,
          triggers.get());
    } else {
      return findActivityWithoutWorkflowIdsAndWithoutStatusesAndWithoutTriggers(from, to, page);
    }

  }

  private Page<ActivityEntity> findActivityWithoutWorkflowIdsAndWithoutStatusesAndWithoutTriggers(
      Optional<Date> from, Optional<Date> to, Pageable page) {
    if (from.isPresent() && !to.isPresent()) {
      return repository.findByCreationDateAfter(from.get(), page);
    } else if (from.isPresent()) {
      return repository.findByCreationDateBetween(from.get(), to.get(), page);
    } else if (to.isPresent()) {
      return repository.findByCreationDateBefore(to.get(), page);
    } else {
      return repository.findAll(page);
    }
  }

  private Page<ActivityEntity> findActivityWithoutWorkflowIdsAndWithoutStatusesAndWithTriggers(
      Optional<Date> from, Optional<Date> to, Pageable page, List<String> triggers) {
    if (from.isPresent() && !to.isPresent()) {
      return repository.findByTriggerInAndCreationDateAfter(
          FlowTriggerEnumConverter.convert(triggers), from.get(), page);
    } else if (from.isPresent()) {
      return repository.findByTriggerInAndCreationDateBetween(
          FlowTriggerEnumConverter.convert(triggers), from.get(), to.get(), page);
    } else if (to.isPresent()) {
      return repository.findByTriggerInAndCreationDateBefore(
          FlowTriggerEnumConverter.convert(triggers), to.get(), page);
    } else {
      return repository.findByTriggerIn(FlowTriggerEnumConverter.convert(triggers), page);
    }
  }

  @Override
  public ActivityEntity findByWorkflowAndProperty(String workflowId, String key,
      String value) {
    return repository.findByWorkflowAndProperty(workflowId, key, value);
  }
  
  @Override
  public List<ActivityEntity> findbyWorkflowIdsAndStatus(List<String> workflowIds, TaskStatus status){
    return repository.findByWorkflowIdInAndStatus(workflowIds, status);
  }
}
