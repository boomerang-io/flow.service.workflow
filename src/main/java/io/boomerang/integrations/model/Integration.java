package io.boomerang.integrations.model;

import org.springframework.data.annotation.Id;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.boomerang.integrations.model.enums.IntegrationStatus;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class Integration {
  @Id
  private String id;

  private String name;
  
  private String link;
  
  private String icon;
  
  private IntegrationStatus status = IntegrationStatus.unlinked;
  
  private String description;

  private String instructions;

  private String ref;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getLink() {
    return link;
  }

  public void setLink(String link) {
    this.link = link;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public IntegrationStatus getStatus() {
    return status;
  }

  public void setStatus(IntegrationStatus status) {
    this.status = status;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getInstructions() {
    return instructions;
  }

  public void setInstructions(String instructions) {
    this.instructions = instructions;
  }

  public String getRef() {
    return ref;
  }

  public void setRef(String ref) {
    this.ref = ref;
  }  
}
