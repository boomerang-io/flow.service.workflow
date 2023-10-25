package io.boomerang.model.tekton;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.fabric8.kubernetes.api.model.PodSecurityContext;

public class Step {

  private String name;
  private String image;
  private String script;
  private String workingDir;
  private List<Env> env;
  private List<String> command;
  private List<String> args;
  @JsonInclude(Include.NON_NULL)
  private PodSecurityContext securityContext;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getImage() {
    return image;
  }

  public void setImage(String image) {
    this.image = image;
  }

  public List<String> getArgs() {
    return args;
  }

  public void setArgs(List<String> args) {
    this.args = args;
  }

  public List<String> getCommand() {
    return command;
  }

  public void setCommand(List<String> command) {
    this.command = command;
  }

  public String getScript() {
    return script;
  }

  public void setScript(String script) {
    this.script = script;
  }

  public List<Env> getEnv() {
    return env;
  }

  public void setEnv(List<Env> env) {
    this.env = env;
  }

  public String getWorkingDir() {
    return workingDir;
  }

  public void setWorkingDir(String workingDir) {
    this.workingDir = workingDir;
  }

  public PodSecurityContext getSecurityContext() {
    return securityContext;
  }

  public void setSecurityContext(PodSecurityContext securityContext) {
    this.securityContext = securityContext;
  }
}
