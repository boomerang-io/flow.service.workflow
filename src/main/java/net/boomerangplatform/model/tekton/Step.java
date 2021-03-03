package net.boomerangplatform.model.tekton; 
import java.util.List;

public class Step {
  
    private String name;
    private String image;
    private List<String> command;
    private List<String> args;
    
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
}
