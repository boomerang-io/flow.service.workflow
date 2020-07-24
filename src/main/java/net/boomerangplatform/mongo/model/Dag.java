package net.boomerangplatform.mongo.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import net.boomerangplatform.mongo.model.next.DAGTask;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class Dag {

  private List<DAGTask> tasks;

  public List<DAGTask> getTasks() {
    return tasks;
  }

  public void setTasks(List<DAGTask> tasks) {
    this.tasks = tasks;
  }
}
