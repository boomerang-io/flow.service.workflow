package net.boomerangplatform.model.tekton;

import java.util.List;

public class Spec{
  
    private List<Param> params;
    private List<Step> steps;
    public List<Step> getSteps() {
      return steps;
    }
    public void setSteps(List<Step> steps) {
      this.steps = steps;
    }
    public List<Param> getParams() {
      return params;
    }
    public void setParams(List<Param> params) {
      this.params = params;
    }
}
