package net.boomerangplatform.dag;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.junit.Test;
import net.boomerangplatform.util.GraphProcessor;


public class GraphTest {

  @Test
  public void testBranch() {

    final String[] verties = {"1", "2", "4", "3"};
    final List<Pair<String, String>> edges = new LinkedList<Pair<String, String>>();
    edges.add(Pair.of("1", "2"));
    edges.add(Pair.of("2", "3"));
    edges.add(Pair.of("2", "4"));
    edges.add(Pair.of("4", "3"));
    final Graph<String, DefaultEdge> graph =
        GraphProcessor.createGraph(Arrays.asList(verties), edges);

    final List<String> nodes = GraphProcessor.createOrderedTaskList(graph, "1", "3");
    final List<String> expected = Arrays.asList("2", "4");
    assertThat(nodes, is(expected));
  }

  @Test
  public void testOrphanNode() {

    final String[] verties = {"1", "2", "4", "3"};
    final List<Pair<String, String>> edges = new LinkedList<Pair<String, String>>();
    edges.add(Pair.of("1", "2"));
    edges.add(Pair.of("2", "3"));
    final Graph<String, DefaultEdge> graph =
        GraphProcessor.createGraph(Arrays.asList(verties), edges);

    final List<String> nodes = GraphProcessor.createOrderedTaskList(graph, "1", "3");

    final List<String> expected = Arrays.asList("2");
    assertThat(nodes, is(expected));
  }

  @Test
  public void testPartialOrphanNode() {

    final String[] verties = {"1", "2", "4", "3"};
    final List<Pair<String, String>> edges = new LinkedList<Pair<String, String>>();
    edges.add(Pair.of("1", "2"));
    edges.add(Pair.of("2", "3"));
    edges.add(Pair.of("2", "4"));
    final Graph<String, DefaultEdge> graph =
        GraphProcessor.createGraph(Arrays.asList(verties), edges);

    final List<String> nodes = GraphProcessor.createOrderedTaskList(graph, "1", "3");

    final List<String> expected = Arrays.asList("2");
    assertThat(nodes, is(expected));
  }

  @Test
  public void testReverseOrphan() {

    final String[] verties = {"1", "2", "4", "3"};
    final List<Pair<String, String>> edges = new LinkedList<Pair<String, String>>();
    edges.add(Pair.of("1", "2"));
    edges.add(Pair.of("2", "3"));
    edges.add(Pair.of("4", "2"));

    final Graph<String, DefaultEdge> graph =
        GraphProcessor.createGraph(Arrays.asList(verties), edges);

    final List<String> nodes = GraphProcessor.createOrderedTaskList(graph, "1", "3");

    final List<String> expected = Arrays.asList("2");
    assertThat(nodes, is(expected));
  }

  @Test
  public void testSimpleGraph() {

    final String[] verties = {"1", "2", "3"};
    final List<Pair<String, String>> edges = new LinkedList<Pair<String, String>>();
    edges.add(Pair.of("1", "2"));
    edges.add(Pair.of("2", "3"));
    final Graph<String, DefaultEdge> graph =
        GraphProcessor.createGraph(Arrays.asList(verties), edges);

    final List<String> nodes = GraphProcessor.createOrderedTaskList(graph, "1", "3");
    final List<String> expected = Arrays.asList("2");
    assertThat(nodes, is(expected));
  }

}
