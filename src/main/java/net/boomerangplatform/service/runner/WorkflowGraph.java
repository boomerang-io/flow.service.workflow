package net.boomerangplatform.service.runner;

import org.jgrapht.Graph;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;

public class WorkflowGraph {

  private Graph<String, DefaultEdge> graph;
  private String start;
  private String end;
  private DijkstraShortestPath<String, DefaultEdge> dijkstraAlg;

  public Graph<String, DefaultEdge> getGraph() {
    return graph;
  }

  public void setGraph(Graph<String, DefaultEdge> graph) {
    this.graph = graph;
  }

  public String getStart() {
    return start;
  }

  public void setStart(String start) {
    this.start = start;
  }

  public String getEnd() {
    return end;
  }

  public void setEnd(String end) {
    this.end = end;
  }

  public DijkstraShortestPath<String, DefaultEdge> getDijkstraAlg() {
    return dijkstraAlg;
  }

  public void setDijkstraAlg(DijkstraShortestPath<String, DefaultEdge> dijkstraAlg) {
    this.dijkstraAlg = dijkstraAlg;
  }

  public WorkflowGraph(Graph<String, DefaultEdge> graph, String start, String end,
      DijkstraShortestPath<String, DefaultEdge> dijkstraAlg) {
    this.graph = graph;
    this.start = start;
    this.end = end;
    this.dijkstraAlg = dijkstraAlg;
  }
}
