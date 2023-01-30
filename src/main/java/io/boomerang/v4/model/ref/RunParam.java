package io.boomerang.v4.model.ref;

public class RunParam {
  
  private String name;
  private Object value;
  
  protected RunParam() {
  }

  public RunParam(String name, Object value) {
    this.name = name;
    this.value = value;
  }

  @Override
  public String toString() {
    return "RunParam [name=" + name + ", value=" + value + "]";
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Object getValue() {
    return value;
  }

  public void setValue(Object value) {
    this.value = value;
  }
}
