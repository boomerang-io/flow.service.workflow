package io.boomerang.tekton;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import io.boomerang.model.AbstractParam;
import io.boomerang.model.ref.ChangeLog;
import io.boomerang.model.ref.ParamSpec;
import io.boomerang.model.ref.TaskTemplate;

public class TektonConverter {

  private TektonConverter() {

  }

  public static TektonTask convertTaskTemplateToTektonTask(TaskTemplate taskTemplate) {
    TektonTask tektonTask = new TektonTask();
    tektonTask.setApiVersion("tekton.dev/v1beta1");
    tektonTask.setKind("Task");

    Metadata metadata = new Metadata();
    metadata.setName(taskTemplate.getName());
    metadata.setLabels(new HashMap<String, String>());
    List<AbstractParam> configList = taskTemplate.getConfig();
    configList.forEach(c -> {
      c.setDescription(null);
      c.setDefaultValue(null);
    });
    Map<String, Object> annotations = metadata.getAnnotations();
    annotations.putAll(taskTemplate.getAnnotations());
    annotations.put("boomerang.io/icon", taskTemplate.getIcon());
    annotations.put("boomerang.io/params", configList);
    annotations.put("boomerang.io/category", taskTemplate.getCategory());
    annotations.put("boomerang.io/displayName", taskTemplate.getDisplayName());
    annotations.put("boomerang.io/version", taskTemplate.getVersion());
    annotations.put("boomerang.io/verified", taskTemplate.isVerified());
    tektonTask.setMetadata(metadata);

    Spec spec = new Spec();
    spec.setDescription(taskTemplate.getDescription());

    Step step = new Step();
    step.setName(taskTemplate.getName());
    step.setImage(taskTemplate.getSpec().getImage());
    step.setScript(taskTemplate.getSpec().getScript());
    step.setWorkingDir(taskTemplate.getSpec().getWorkingDir());
    step.setEnv(taskTemplate.getSpec().getEnvs());
    step.setCommand(taskTemplate.getSpec().getCommand());
    step.setArgs(taskTemplate.getSpec().getArguments());

    List<Step> steps = new LinkedList<>();
    steps.add(step);
    spec.setSteps(steps);

    // Assumes that on creation - the web client config is transformed to Params
    List<Param> params = new LinkedList<>();
    if (taskTemplate.getSpec().getParams() != null) {
      for (ParamSpec templateParam : taskTemplate.getSpec().getParams()) {
        Param param = new Param();
        param.setName(templateParam.getName());
        param.setDescription(templateParam.getDescription());
        if (templateParam.getDefaultValue() != null) {
          param.setDefaultValue(templateParam.getDefaultValue());
        }
        param.setType(templateParam.getType());
        params.add(param);
      }
    }
    spec.setParams(params);
    spec.setResults(taskTemplate.getSpec().getResults());
    tektonTask.setSpec(spec);

    return tektonTask;
  }

  /*
   * Converts a TektonTask to a Flow TaskTemplate.
   * 
   * Version and Verified need to be set outside of this method once its known if the task exists
   * and the requestor has access to the task.
   * 
   * TODO: figure out how Type is set
   */
  public static TaskTemplate convertTektonTaskToTaskTemplate(TektonTask task) {
    TaskTemplate taskTemplate = new TaskTemplate();

    Metadata metadata = task.getMetadata();
    taskTemplate.setName(metadata.getName());
    taskTemplate.setLabels(metadata.getLabels());

    List<AbstractParam> config = new LinkedList<>();
    taskTemplate.setConfig(config);
    List<AbstractParam> annotationParams = new LinkedList<>();
    if (metadata.getAnnotations() != null && !metadata.getAnnotations().isEmpty()) {
      Map<String, Object> annotations = metadata.getAnnotations();
      Object icon = annotations.get("boomerang.io/icon");
      if (icon != null) {
        taskTemplate.setIcon(icon.toString());
      }
      annotations.remove("boomerang.io/icon");
      Object category = annotations.get("boomerang.io/category");
      if (category != null) {
        taskTemplate.setCategory(category.toString());
      }
      annotations.remove("boomerang.io/category");
      // Check if description is set as an annotation. It will be overridden if the Spec has the
      // optional description
      Object description = annotations.get("description");
      if (description != null) {
        taskTemplate.setDescription(description.toString());
      }
      annotations.remove("description");
      Object displayName = annotations.get("boomerang.io/displayName");
      if (displayName != null) {
        taskTemplate.setDisplayName(displayName.toString());
      }
      annotations.remove("boomerang.io/displayName");
      Object version = annotations.get("boomerang.io/version");
      if (version != null) {
        taskTemplate.setVersion((Integer) version);
      }
      annotations.remove("boomerang.io/version");
      annotations.remove("boomerang.io/verified");
      Object abstractParams = annotations.get("boomerang.io/params");
      if (abstractParams != null) {
        annotationParams = (List<AbstractParam>) abstractParams;
      }
    }

    Spec spec = task.getSpec();
    if (spec.getDescription() != null && !spec.getDescription().isBlank()) {
      taskTemplate.setDescription(spec.getDescription());
    }
    Step step = spec.getSteps().get(0);
    if (step.getImage() != null && !step.getImage().isBlank()) {
      taskTemplate.getSpec().setImage(step.getImage());
    }
    if (step.getScript() != null && !step.getScript().isBlank()) {
      taskTemplate.getSpec().setScript(step.getScript());
    }
    if (step.getWorkingDir() != null && !step.getWorkingDir().isBlank()) {
      taskTemplate.getSpec().setWorkingDir(step.getWorkingDir());
    }
    if (step.getEnv() != null && !step.getEnv().isEmpty()) {
      taskTemplate.getSpec().setEnvs(step.getEnv());
    }
    if (step.getCommand() != null && !step.getCommand().isEmpty()) {
      taskTemplate.getSpec().setCommand(step.getCommand());
    }
    if (step.getArgs() != null && !step.getArgs().isEmpty()) {
      taskTemplate.getSpec().setArguments(step.getArgs());
    }

    if (spec.getParams() != null && !spec.getParams().isEmpty()) {
      List<ParamSpec> paramSpecs = new LinkedList<>();
      for (Param param : spec.getParams()) {
        ParamSpec paramSpec = new ParamSpec();
        paramSpec.setName(param.getName());
        paramSpec.setDescription(param.getDescription());
        paramSpec.setDefaultValue(param.getDefaultValue());
        paramSpec.setType(param.getType());
        paramSpecs.add(paramSpec);
        Optional<AbstractParam> aParam =
            annotationParams.stream().filter(c -> c.getKey().equals(param.getName())).findFirst();
        if (aParam.isPresent()) {
          aParam.get().setDefaultValue(null);
          aParam.get().setDescription(null);
          // Legacy - might not be needed with model deserialisation
          // if (defaultStr instanceof ArrayList<?>){
          // ArrayList<String> values = (ArrayList<String>) defaultStr;
          // StringBuilder sb = new StringBuilder();
          // for (String line : values) {
          // sb.append(line);
          // sb.append('\n');
          // newConfig.setDefaultValue(sb.toString());
          // }
          // }
          // TODO ensure type matches
          taskTemplate.getConfig().add(aParam.get());
        } else {
          AbstractParam newAbstractParam = new AbstractParam();
          newAbstractParam.setKey(param.getName());
          newAbstractParam.setLabel(param.getName());
          newAbstractParam.setType("text");
          newAbstractParam.setReadOnly(false);
          newAbstractParam.setPlaceholder("");
          // TODO ensure type matches
          taskTemplate.getConfig().add(newAbstractParam);
        }
      }
      taskTemplate.getSpec().setParams(paramSpecs);
    }

    if (spec.getResults() != null && !spec.getResults().isEmpty()) {
      spec.setResults(spec.getResults());
    }

    ChangeLog changelog = new ChangeLog();
    changelog.setDate(new Date());
    taskTemplate.setChangelog(changelog);
    return taskTemplate;
  }

}
