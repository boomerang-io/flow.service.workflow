package net.boomerangplatform.mongo.model;

import java.util.List;
import org.springframework.data.annotation.Id;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class WorkflowConfiguration {

  @Id
  private String id;

  private List<TaskConfigurationNode> nodes;

  public WorkflowConfiguration() {
    // Do nothing
  }

  public String getId() {
    return id;
  }

  public List<TaskConfigurationNode> getNodes() {
    return nodes;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setNodes(List<TaskConfigurationNode> nodes) {
    this.nodes = nodes;
  }

}
