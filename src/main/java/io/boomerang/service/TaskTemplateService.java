package io.boomerang.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort.Direction;
import io.boomerang.client.TaskTemplateResponsePage;
import io.boomerang.tekton.TektonTask;
import io.boomerang.v4.model.ref.ChangeLogVersion;
import io.boomerang.v4.model.ref.TaskTemplate;

public interface TaskTemplateService {

  TaskTemplate get(String name, Optional<Integer> version);

  TaskTemplateResponsePage query(Optional<Integer> queryLimit, Optional<Integer> queryPage,
      Optional<Direction> querySort, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus, Optional<List<String>> queryNames,
      Optional<List<String>> queryTeams);

  TaskTemplate create(TaskTemplate request, Optional<String> team);

  TaskTemplate apply(TaskTemplate request, boolean replace,
      Optional<String> teamId);

  TektonTask getAsTekton(String name, Optional<Integer> version);

  TektonTask createAsTekton(TektonTask tektonTask, Optional<String> teamId);

  TektonTask applyAsTekton(TektonTask tektonTask, boolean replace, Optional<String> teamId);

  void validateAsTekton(TektonTask tektonTask);

  List<ChangeLogVersion> changelog(String name);
}
