package net.boomerangplatform.misc;

import static org.junit.Assert.assertEquals;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import net.boomerangplatform.model.tekton.Annotations;
import net.boomerangplatform.model.tekton.Labels;
import net.boomerangplatform.model.tekton.Metadata;
import net.boomerangplatform.model.tekton.Param;
import net.boomerangplatform.model.tekton.Spec;
import net.boomerangplatform.model.tekton.Step;
import net.boomerangplatform.model.tekton.TektonTask;
import net.boomerangplatform.mongo.entity.FlowTaskTemplateEntity;
import net.boomerangplatform.service.tekton.TetkonConverter;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

public class TektonImportExportTests {
  
  @Test
  public void testYamlImport() throws IOException {
    TektonTask task = loadTektonTask();
    assertEquals("tekton.dev/v1beta1", task.getApiVersion());
    assertEquals("Task", task.getKind());
    Metadata metadata = task.getMetadata();
    testMetadata(metadata);
    testSpec(task);
  }
  
  @Test 
  public void testYamlExport() throws IOException {
    FlowTaskTemplateEntity flowTaskTemplate = loadFlowTemplate();
    TektonTask task = TetkonConverter.convertFlowTaskToTekton(flowTaskTemplate, Optional.empty());
    logObjectASYaml(task);
    assertEquals("Task", task.getKind());
  }
  
  @Test
  public void testYamlConversion() throws IOException {
    TektonTask task = loadTektonTask();
    FlowTaskTemplateEntity entity = TetkonConverter.convertTektonTaskToNewFlowTask(task);
    assertEquals("example-task-name", entity.getName());
    logObjectASJson(entity);
  }
  
  private void logObjectASJson(Object o) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    System.out.println(mapper.writer().withDefaultPrettyPrinter().writeValueAsString(o));
  }
  
  private void logObjectASYaml(Object o) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES).disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
    System.out.println(mapper.writer().writeValueAsString(o));
  }
  
  private FlowTaskTemplateEntity loadFlowTemplate()
      throws IOException, JsonProcessingException, JsonMappingException {
    File resource = new ClassPathResource("yaml/import.json").getFile();
    String yamlString = new String(Files.readAllBytes(resource.toPath()));
    ObjectMapper mapper = new ObjectMapper();
    
    FlowTaskTemplateEntity task = mapper.readValue(yamlString, FlowTaskTemplateEntity.class);
    return task;
  }
  
  private TektonTask loadTektonTask()
      throws IOException, JsonProcessingException, JsonMappingException {
    File resource = new ClassPathResource("yaml/import.yaml").getFile();
    String yamlString = new String(Files.readAllBytes(resource.toPath()));
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    
    TektonTask task = mapper.readValue(yamlString, TektonTask.class);
    return task;
  }
  
  private void testMetadata(Metadata metadata) {
    assertEquals("example-task-name", metadata.getName());
    
    Labels labels = metadata.getLabels();
    assertEquals("value", labels.otherFields().get("key"));
    
    Annotations annotations = metadata.getAnnotations();
    assertEquals(4, annotations.otherFields().size());
    
    Map<String, Object> mappings = annotations.otherFields();

    assertEquals("fix", mappings.get("boomerang.io/icon"));
    assertEquals("Worker", mappings.get("boomerang.io/category"));
    assertEquals(1, mappings.get("boomerang.io/revision"));
    assertEquals("cool task", mappings.get("description"));
  }
  
  private void testSpec(TektonTask task) {
    Spec spec = task.getSpec();
    List<Param> params = spec.getParams();
    assertEquals(1, params.size());
    
    Param param = params.get(0);
    assertEquals("pathToDockerFile", param.getName());
    assertEquals("string", param.getType());
    assertEquals("The path to the dockerfile to build", param.getDescription());
    
    List<Step> steps = spec.getSteps();
    assertEquals(1, steps.size());
    Step step = steps.get(0);
    assertEquals("ubuntu", step.getImage());
    assertEquals("ubuntu-example", step.getName());
    assertEquals("entrypoint", step.getCommand().get(0));
    testArguments(step);
  }

  private void testArguments(Step step) {
    List<String> args = step.getArgs();
    assertEquals("ubuntu-build-example", args.get(0));
    assertEquals("SECRETS-example.md", args.get(1));
  }

}
