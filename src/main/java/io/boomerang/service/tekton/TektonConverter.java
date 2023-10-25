package io.boomerang.service.tekton;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.util.Strings;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import io.boomerang.model.ParamType;
import io.boomerang.model.Result;
import io.boomerang.model.controller.TaskEnvVar;
import io.boomerang.model.controller.TaskResult;
import io.boomerang.model.tekton.Annotations;
import io.boomerang.model.tekton.Env;
import io.boomerang.model.tekton.Labels;
import io.boomerang.model.tekton.Metadata;
import io.boomerang.model.tekton.Param;
import io.boomerang.model.tekton.Spec;
import io.boomerang.model.tekton.Step;
import io.boomerang.model.tekton.TektonTask;
import io.boomerang.mongo.entity.FlowTaskTemplateEntity;
import io.boomerang.mongo.model.ChangeLog;
import io.boomerang.mongo.model.FlowTaskTemplateStatus;
import io.boomerang.mongo.model.Revision;
import io.boomerang.mongo.model.TaskTemplateConfig;
import io.fabric8.kubernetes.api.model.PodSecurityContext;

public class TektonConverter {
  
  private TektonConverter() {
    
  }
  
  public static TektonTask convertFlowTaskToTekton(FlowTaskTemplateEntity task, Optional<Integer> revisionNumber) {
    TektonTask newTask = new TektonTask();
    newTask.setApiVersion("tekton.dev/v1beta1");
    newTask.setKind("Task");
    
    
    
    Metadata metadata = new  Metadata();
    metadata.setName(task.getName());
    metadata.setLabels(new Labels());
    newTask.setMetadata(metadata);
    
    Spec spec = new Spec();
    newTask.setSpec(spec);
    
    Revision revision = null;
    List<Revision> revisions = task.getRevisions();
    
    if (revisions != null) {
      if (revisionNumber.isEmpty()) {
        revision = revisions.stream()
            .sorted(Comparator.comparingInt(Revision::getVersion).reversed() ).findFirst().orElse(null);
      }
      else {
        revision = revisions.stream().filter(c -> c.getVersion().equals(revisionNumber.get())).findFirst().orElse(null);
      }
    
    }

    if (revision != null) {
      Step step = new Step();
      
      step.setImage(revision.getImage());
      step.setScript(revision.getScript());
      step.setWorkingDir(revision.getWorkingDir());
      
      List<Env> envList = new LinkedList<>();
      if (revision.getEnvs() != null) {
  
        for (TaskEnvVar taskEnv :revision.getEnvs()) {
          Env env = new Env();
          env.setName(taskEnv.getName());
          env.setValue(taskEnv.getValue());
          
          envList.add(env);
        }
      }
      step.setEnv(envList);
      step.setCommand(revision.getCommand());
      
      step.setArgs(revision.getArguments());

      step.setName(task.getName());
      
      String sc = revision.getSecurityContext();
      if (Strings.isNotBlank(sc)) {
        try {
          ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
          PodSecurityContext podSecurityContext = mapper.readValue(sc, PodSecurityContext.class);
          step.setSecurityContext(podSecurityContext);
          
        } catch (JsonMappingException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        } catch (JsonProcessingException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
      
      List<Step> steps = new LinkedList<>();

      steps.add(step);
      spec.setSteps(steps);
      
      if (Strings.isNotBlank(revision.getServiceAccountName())) {
        spec.setServiceAccountName(revision.getServiceAccountName());
      }
      
      List<Param> params = new LinkedList<>();
      List<TaskTemplateConfig> configList = revision.getConfig();
    
      Annotations annotations = new Annotations();
      Map<String, Object> annotationFields = annotations.otherFields();
      annotationFields.put("boomerang.io/icon", task.getIcon());
      annotationFields.put("boomerang.io/params", configList);
      annotationFields.put("boomerang.io/category", task.getCategory());
  
      metadata.setAnnotations(annotations);
      
      if (configList != null) {
        for (TaskTemplateConfig config : configList) {
          Param param = new Param();
          param.setName(config.getKey());
          param.otherFields().put("default", config.getDefaultValue());
          
          if ("textarea".equals(config.getType())) {
            param.setType(ParamType.array);
          } else {
            param.setType(ParamType.string);
          }
          
          param.setDescription(config.getDescription());
          
          params.add(param);
        }
      }
      
      for (TaskTemplateConfig config : configList) {
        config.setDescription(null);
        config.setDefaultValue(null);
      }
      
      List<Result> results = new LinkedList<>();
      List<TaskResult> resultsList = revision.getResults();
      if (resultsList != null) {
        for (TaskResult result : resultsList) {
          Result r = new Result();
          r.setName(result.getName());
          r.setDescription(result.getDescription());
          results.add(r);
        }
      }
    
      spec.setResults(results);
      
      revision.getConfig();
      spec.setDescription(task.getDescription());
      spec.setParams(params);
    }
    return newTask;
  }

  public static FlowTaskTemplateEntity convertTektonTaskToNewFlowTask(TektonTask task) {
    FlowTaskTemplateEntity taskTemplate = new FlowTaskTemplateEntity();
    
    Metadata metadata = task.getMetadata();
    
    taskTemplate.setName(metadata.getName());
    getAnnotations(taskTemplate, metadata);
    
    List<Revision> revisions  = new LinkedList<>();
    Revision newRevision = TektonConverter.convertSpecToRevision(task);
    revisions.add(newRevision);
    newRevision.setVersion(1);
    taskTemplate.setRevisions(revisions);
    taskTemplate.setStatus(FlowTaskTemplateStatus.active);
    taskTemplate.setVerified(false);
    

    taskTemplate.setDescription(task.getSpec().getDescription());
    
    
    Date createdDate = new Date();
    taskTemplate.setCreatedDate(createdDate);
    taskTemplate.setLastModified(createdDate);
    taskTemplate.setNodetype("templateTask");
    return taskTemplate;
  }

  private static void getAnnotations(FlowTaskTemplateEntity taskTemplate, Metadata metadata) {
    Annotations annotations = metadata.getAnnotations();

    Object icon = annotations.otherFields().get("boomerang.io/icon");
    Object category = annotations.otherFields().get("boomerang.io/category");
    Object description = annotations.otherFields().get("description");
    
    if (icon != null) {
      taskTemplate.setIcon(icon.toString());
    }
    
    if (category != null) {
      taskTemplate.setCategory(category.toString());
    }
    
    if (description != null) {
      taskTemplate.setDescription(description.toString());
    }
  }
  
  @SuppressWarnings("unchecked")
  private static Revision convertSpecToRevision(TektonTask task) {
    List<Map<String, Object>> params  = extractBoomerangParams(task);

    Spec spec = task.getSpec();
    Step step = spec.getSteps().get(0);
    
    Revision revision = new Revision();
    
    revision.setImage(step.getImage());
    revision.setArguments(step.getArgs());
    revision.setScript(step.getScript());
    revision.setWorkingDir(step.getWorkingDir());
    if (step.getSecurityContext() != null) {
      try {
        ObjectMapper mapper =
          new ObjectMapper(new YAMLFactory().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
              .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
        revision.setSecurityContext(mapper.writer().writeValueAsString(step.getSecurityContext()));
      } catch (JsonProcessingException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    revision.setServiceAccountName(spec.getServiceAccountName());
    
    List<TaskResult> taskResults = new LinkedList<>();
    
    if (spec.getResults() != null) {
      for (Result taskResult :spec.getResults() ) {
        TaskResult result = new TaskResult();
        result.setName(taskResult.getName());
        result.setDescription(taskResult.getDescription());
        taskResults.add(result);
      }
      revision.setResults(taskResults);
    }
    
    
    
    
    if (step.getCommand() != null) {
      revision.setCommand(step.getCommand());
    }
    
    if (step.getEnv() != null) {
      List<Env> envs = step.getEnv();
      List<TaskEnvVar> taskList = new LinkedList<>();
      for (Env env : envs) {
        TaskEnvVar taskEnvVar = new TaskEnvVar();
        taskEnvVar.setName(env.getName());
        taskEnvVar.setValue(env.getValue());
        taskList.add(taskEnvVar);
      }
      revision.setEnvs(taskList);
      
    }
    List<TaskTemplateConfig> config = new LinkedList<>();
    
    if (spec.getParams() != null) {
      for (Param param : spec.getParams()) {
        
        String key = param.getName();
        
        Map<String, Object> extraPrams = params.stream().filter(element -> key.equals(element.get("key")) ).findFirst().orElse(new HashMap<>());
        
        TaskTemplateConfig newConfig = new TaskTemplateConfig();
        newConfig.setKey(key);
        newConfig.setLabel(key);
        
        newConfig.setDescription(param.getDescription());
      
        ParamType type = param.getType();
        Object defaultStr = param.otherFields().get("default");
        if (type == ParamType.string) {
          if (defaultStr != null) {
            newConfig.setDefaultValue(defaultStr.toString());
          }
          
          newConfig.setType("text");
          
        } else if (defaultStr instanceof ArrayList<?>){
          ArrayList<String> values = (ArrayList<String>) defaultStr;
          StringBuilder sb = new StringBuilder();
          for (String line : values) {
            sb.append(line);
            sb.append('\n');
            newConfig.setDefaultValue(sb.toString());
          }
        }
        
     
        newConfig.setReadOnly(false);
        newConfig.setPlaceholder("");
        
        
        if (extraPrams.containsKey("placeholder")) {
          newConfig.setPlaceholder((String) extraPrams.get("placeholder"));
        }
        if (extraPrams.containsKey("readOnly")) {
          newConfig.setReadOnly((Boolean) extraPrams.get("readOnly"));
        }
        if (extraPrams.containsKey("label")) {
          newConfig.setLabel((String) extraPrams.get("label"));
        }
        if (extraPrams.containsKey("type")) {
          newConfig.setType((String) extraPrams.get("type"));
        }
        
        
        config.add(newConfig);
      }
      revision.setConfig(config);
      
      ChangeLog changeLog = new ChangeLog();
      changeLog.setDate(new Date());
      
      revision.setChangelog(changeLog);
    }
   
    return revision;
  }

  @SuppressWarnings("unchecked")
  private static  List<Map<String, Object>> extractBoomerangParams(TektonTask task) {
    List<Map<String, Object>> paramList = new LinkedList<>();
    if (task.getMetadata() != null) {
      Metadata metdata = task.getMetadata();
      if (metdata.getAnnotations() != null) {
        Annotations annotations = metdata.getAnnotations();
        paramList = (List<Map<String, Object>>) annotations.otherFields().get("boomerang.io/params");
      }
    }
    return paramList;
  }
  
}
