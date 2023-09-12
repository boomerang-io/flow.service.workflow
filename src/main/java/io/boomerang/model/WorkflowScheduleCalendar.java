package io.boomerang.model;

import java.util.Date;
/*
 * Maps a list of Dates returned by Quartz utils to a schedule for displaying on the calendar
 */
import java.util.List;

public class WorkflowScheduleCalendar {

  private String scheduleId;
  
  private List<Date> dates;

  public String getScheduleId() {
    return scheduleId;
  }

  public void setScheduleId(String scheduleId) {
    this.scheduleId = scheduleId;
  }

  public List<Date> getDates() {
    return dates;
  }

  public void setDates(List<Date> dates) {
    this.dates = dates;
  }
}
