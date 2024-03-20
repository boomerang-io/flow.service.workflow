package io.boomerang.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort.Direction;
import io.boomerang.client.TaskResponsePage;
import io.boomerang.model.ref.ChangeLogVersion;
import io.boomerang.model.ref.Task;
import io.boomerang.tekton.TektonTask;

public interface TaskService {

  Task get(String team, String name, Optional<Integer> version);

  Task get(String name, Optional<Integer> version);

  TaskResponsePage query(Optional<Integer> queryLimit, Optional<Integer> queryPage,
      Optional<Direction> querySort, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus, Optional<List<String>> queryNames);

  TaskResponsePage query(String queryTeam, Optional<Integer> queryLimit, Optional<Integer> queryPage,
      Optional<Direction> querySort, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus, Optional<List<String>> queryNames);
  
  Task create(String team, Task request);

  Task create(Task request);

  Task apply(String team, String name, Task request, boolean replace);
  
  Task apply(String name, Task request, boolean replace);

  TektonTask getAsTekton(String team, String name, Optional<Integer> version);
  
  TektonTask getAsTekton(String name, Optional<Integer> version);

  TektonTask createAsTekton(String team, TektonTask tektonTask);

  TektonTask createAsTekton(TektonTask tektonTask);

  TektonTask applyAsTekton(String team, String name, TektonTask tektonTask, boolean replace);

  TektonTask applyAsTekton(String name, TektonTask tektonTask, boolean replace);

  void validateAsTekton(TektonTask tektonTask);

  List<ChangeLogVersion> changelog(String team, String name);
  
  List<ChangeLogVersion> changelog(String name);

  void delete(String team, String name);
}
