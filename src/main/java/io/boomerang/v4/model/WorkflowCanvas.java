package io.boomerang.v4.model;

import java.util.List;

public class WorkflowCanvas {

  List<CanvasNode> nodes;
  List<CanvasEdge> edges;
  
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
