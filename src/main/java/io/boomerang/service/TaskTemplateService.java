package io.boomerang.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort.Direction;
import io.boomerang.client.TaskTemplateResponsePage;
import io.boomerang.model.ref.ChangeLogVersion;
import io.boomerang.model.ref.TaskTemplate;
import io.boomerang.tekton.TektonTask;

public interface TaskTemplateService {

  TaskTemplate get(String name, Optional<Integer> version, Optional<String> team);

  TaskTemplateResponsePage query(Optional<Integer> queryLimit, Optional<Integer> queryPage,
      Optional<Direction> querySort, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus, Optional<List<String>> queryNames,
      Optional<String> queryTeam);
  
  TaskTemplate create(TaskTemplate request, Optional<String> team);

  TaskTemplate apply(TaskTemplate request, boolean replace,
      Optional<String> team);

  TektonTask getAsTekton(String name, Optional<Integer> version,
      Optional<String> team);

  TektonTask createAsTekton(TektonTask tektonTask, Optional<String> team);

  TektonTask applyAsTekton(TektonTask tektonTask, boolean replace, Optional<String> team);

  void validateAsTekton(TektonTask tektonTask);

  List<ChangeLogVersion> changelog(String name);
}
