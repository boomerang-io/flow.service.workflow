package io.boomerang.model;

import java.util.Date;
import org.springframework.beans.BeanUtils;
import io.boomerang.data.entity.WorkflowScheduleEntity;

public class WorkflowSchedule extends WorkflowScheduleEntity {
  
  private Date nextScheduleDate;

  public WorkflowSchedule() {
    
  }

  public WorkflowSchedule(WorkflowScheduleEntity entity) {
    BeanUtils.copyProperties(entity, this);
  }

  public WorkflowSchedule(WorkflowScheduleEntity entity, Date nextScheduleDate) {
    BeanUtils.copyProperties(entity, this);
    this.nextScheduleDate = nextScheduleDate;
  }

  public Date getNextScheduleDate() {
    return nextScheduleDate;
  }

  public void setNextScheduleDate(Date nextScheduleDate) {
    this.nextScheduleDate = nextScheduleDate;
  }
}
