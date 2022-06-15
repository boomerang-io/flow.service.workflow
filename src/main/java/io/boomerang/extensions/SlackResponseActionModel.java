package io.boomerang.extensions;

import com.slack.api.model.view.View;

public class SlackResponseActionModel {
  
  private String response_action;
  private View view;
  
  public SlackResponseActionModel(String response_action, View view) {
    super();
    this.response_action = response_action;
    this.view = view;
  }
  public String getResponse_action() {
    return response_action;
  }
  public void setResponse_action(String response_action) {
    this.response_action = response_action;
  }
  public View getView() {
    return view;
  }
  public void setView(View view) {
    this.view = view;
  }
}
