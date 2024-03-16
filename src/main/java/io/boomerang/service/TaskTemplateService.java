package io.boomerang.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort.Direction;
import io.boomerang.client.TaskTemplateResponsePage;
import io.boomerang.model.ref.ChangeLogVersion;
import io.boomerang.model.ref.TaskTemplate;
import io.boomerang.tekton.TektonTask;

public interface TaskTemplateService {

  TaskTemplate get(String name, Optional<Integer> version, String team);

  TaskTemplate get(String name, Optional<Integer> version);

  TaskTemplateResponsePage query(Optional<Integer> queryLimit, Optional<Integer> queryPage,
      Optional<Direction> querySort, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus, Optional<List<String>> queryNames);

  TaskTemplateResponsePage query(Optional<Integer> queryLimit, Optional<Integer> queryPage,
      Optional<Direction> querySort, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus, Optional<List<String>> queryNames, String queryTeam);
  
  TaskTemplate create(TaskTemplate request, String team);

  TaskTemplate create(TaskTemplate request);

  TaskTemplate apply(TaskTemplate request, boolean replace,
      String team);
  
  TaskTemplate apply(TaskTemplate request, boolean replace);

  TektonTask getAsTekton(String name, Optional<Integer> version,
      String team);
  
  TektonTask getAsTekton(String name, Optional<Integer> version);

  TektonTask createAsTekton(TektonTask tektonTask, String team);

  TektonTask createAsTekton(TektonTask tektonTask);

  TektonTask applyAsTekton(TektonTask tektonTask, boolean replace, String team);

  TektonTask applyAsTekton(TektonTask tektonTask, boolean replace);

  void validateAsTekton(TektonTask tektonTask);

  List<ChangeLogVersion> changelog(String name, String team);
  
  List<ChangeLogVersion> changelog(String name);

  void delete(String name, String team);
}
