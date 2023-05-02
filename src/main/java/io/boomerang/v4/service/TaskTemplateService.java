package io.boomerang.v4.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.ResponseEntity;
import io.boomerang.tekton.TektonTask;
import io.boomerang.v4.client.TaskTemplateResponsePage;
import io.boomerang.v4.model.ref.TaskTemplate;

public interface TaskTemplateService {

  ResponseEntity<TaskTemplate> get(String name, Optional<Integer> version);

  TaskTemplateResponsePage query(Optional<Integer> queryLimit, Optional<Integer> queryPage,
      Optional<Direction> querySort, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus, Optional<List<String>> queryNames,
      Optional<List<String>> queryTeams);

  ResponseEntity<TaskTemplate> create(TaskTemplate request, Optional<String> team);

  ResponseEntity<TaskTemplate> apply(TaskTemplate request, boolean replace,
      Optional<String> teamId);

  void enable(String name);

  void disable(String name);

  TektonTask getAsTekton(String name, Optional<Integer> version);

  TektonTask createAsTekton(TektonTask tektonTask, Optional<String> teamId);

  TektonTask applyAsTekton(TektonTask tektonTask, boolean replace, Optional<String> teamId);

  void validateAsTekton(TektonTask tektonTask);
}
