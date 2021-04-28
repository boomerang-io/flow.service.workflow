package net.boomerangplatform.service.tekton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import net.boomerangplatform.model.ParamType;
import net.boomerangplatform.model.tekton.Annotations;
import net.boomerangplatform.model.tekton.Labels;
import net.boomerangplatform.model.tekton.Metadata;
import net.boomerangplatform.model.tekton.Param;
import net.boomerangplatform.model.tekton.Spec;
import net.boomerangplatform.model.tekton.Step;
import net.boomerangplatform.model.tekton.TektonTask;
import net.boomerangplatform.mongo.entity.FlowTaskTemplateEntity;
import net.boomerangplatform.mongo.model.ChangeLog;
import net.boomerangplatform.mongo.model.FlowTaskTemplateStatus;
import net.boomerangplatform.mongo.model.Revision;
import net.boomerangplatform.mongo.model.TaskTemplateConfig;

public class TetkonConverter {
  
  private TetkonConverter() {
    
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
      
      String commandStr = revision.getCommand();
      if (commandStr != null && !commandStr.isBlank()) {
        List<String> commandArray = Arrays.asList(commandStr.split(" ", -1));
        step.setCommand(commandArray);
      }
      step.setArgs(revision.getArguments());

      step.setName(task.getName());
      
      List<Step> steps = new LinkedList<>();

      steps.add(step);
      spec.setSteps(steps);
      
      List<Param> params = new LinkedList<>();
      List<TaskTemplateConfig> configList = revision.getConfig();
    
      Annotations annotations = new Annotations();
      Map<String, Object> annotationFields = annotations.otherFields();
      annotationFields.put("boomerang.io/icon", task.getIcon());
      annotationFields.put("boomerang.io/params", configList);
  
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
    Revision newRevision = TetkonConverter.convertSpecToRevision(task);
    revisions.add(newRevision);
    newRevision.setVersion(1);
    taskTemplate.setRevisions(revisions);
    taskTemplate.setStatus(FlowTaskTemplateStatus.active);
    taskTemplate.setVerified(false);
    taskTemplate.setCategory("community");
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
    
    if (step.getCommand() != null) {
      revision.setCommand(StringUtils.join(step.getCommand(), " "));
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
          newConfig.setDefaultValue(defaultStr.toString());
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
          newConfig.setLabel((String) extraPrams.get("type"));
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
