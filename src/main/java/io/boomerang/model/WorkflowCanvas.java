package io.boomerang.model;

import java.util.List;
import org.springframework.beans.BeanUtils;
import io.boomerang.model.ref.Workflow;

public class WorkflowCanvas extends Workflow {

  List<CanvasNode> nodes;
  List<CanvasEdge> edges;
  
  public WorkflowCanvas() {
    
  }
  
  public WorkflowCanvas(Workflow workflow) {
    BeanUtils.copyProperties(workflow, this, "tasks");
  }
  
  public List<CanvasNode> getNodes() {
    return nodes;
  }
  public void setNodes(List<CanvasNode> nodes) {
    this.nodes = nodes;
  }
  public List<CanvasEdge> getEdges() {
    return edges;
  }
  public void setEdges(List<CanvasEdge> edges) {
    this.edges = edges;
  }
  
}
