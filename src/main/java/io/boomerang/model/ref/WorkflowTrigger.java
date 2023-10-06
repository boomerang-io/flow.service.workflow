package io.boomerang.model.ref;

public class WorkflowTrigger {

  private Trigger manual;
  private Trigger scheduler;
  private Trigger webhook;
  private TriggerEvent event;
  
  public WorkflowTrigger() {
    webhook = new Trigger();
    event = new TriggerEvent();
    manual = new Trigger();
    scheduler = new Trigger();
    
    manual.setEnable(Boolean.TRUE);
  }
  
  public Trigger getManual() {
    return manual;
  }
  public void setManual(Trigger manual) {
    this.manual = manual;
  }
  public Trigger getScheduler() {
    return scheduler;
  }
  public void setScheduler(Trigger scheduler) {
    this.scheduler = scheduler;
  }
  public Trigger getWebhook() {
    return webhook;
  }
  public void setWebhook(Trigger webhook) {
    this.webhook = webhook;
  }
  public TriggerEvent getEvent() {
    return event;
  }
  public void setEvent(TriggerEvent event) {
    this.event = event;
  }
}
