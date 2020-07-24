package net.boomerangplatform.mongo.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type",
    defaultImpl = WorkflowCustomLink.class)
@JsonSubTypes({@JsonSubTypes.Type(value = WorkflowCustomLink.class, name = "custom"),
    @JsonSubTypes.Type(value = WorkflowDecisionLink.class, name = "decision")})
public abstract class WorkflowLink {

  private String id;
  private String selected;
  private String source;
  private String sourcePort;
  private String target;
  private String targetPort;

  private List<Object> points;

  private LinkExtras extras;
  private List<LinkLabel> labels;

  private Integer width;
  private String color;
  private Integer curvyness;

  public WorkflowLink() {

  }

  public String getSelected() {
    return selected;
  }

  public void setSelected(String selected) {
    this.selected = selected;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getSourcePort() {
    return sourcePort;
  }

  public void setSourcePort(String sourcePort) {
    this.sourcePort = sourcePort;
  }

  public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  public String getTargetPort() {
    return targetPort;
  }

  public void setTargetPort(String targetPort) {
    this.targetPort = targetPort;
  }

  public List<Object> getPoints() {
    return points;
  }

  public void setPoints(List<Object> points) {
    this.points = points;
  }

  public LinkExtras getExtras() {
    return extras;
  }

  public void setExtras(LinkExtras extras) {
    this.extras = extras;
  }

  public List<LinkLabel> getLabels() {
    return labels;
  }

  public void setLabels(List<LinkLabel> labels) {
    this.labels = labels;
  }

  public Integer getWidth() {
    return width;
  }

  public void setWidth(Integer width) {
    this.width = width;
  }

  public String getColor() {
    return color;
  }

  public void setColor(String color) {
    this.color = color;
  }

  public Integer getCurvyness() {
    return curvyness;
  }

  public void setCurvyness(Integer curvyness) {
    this.curvyness = curvyness;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }
}
