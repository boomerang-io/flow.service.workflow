package net.boomerangplatform.service.runner;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import net.boomerangplatform.model.Task;
import net.boomerangplatform.model.TaskResult;

public interface FlowTaskRunnerService {
  CompletableFuture<TaskResult> runTasks(Graph<String, DefaultEdge> g, List<Task> tasksToRun,
      String activityId, String start, String end);
}
