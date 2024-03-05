package io.boomerang.audit;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.boomerang.security.model.Token;

@JsonInclude(Include.NON_NULL)
public class AuditEvent {

  private AuditType type;
  private Date date = new Date();
  private AuditActor actor;
  
  public AuditEvent() {
    // TODO Auto-generated constructor stub
  }
  
  public AuditEvent(AuditType type, Token token) {
    this.type = type;
    this.actor = new AuditActor(token);
  }

  public AuditType getType() {
    return type;
  }
  public void setType(AuditType type) {
    this.type = type;
  }
  public Date getDate() {
    return date;
  }
  public void setDate(Date date) {
    this.date = date;
  }
  public AuditActor getActor() {
    return actor;
  }
  public void setActor(AuditActor actor) {
    this.actor = actor;
  }
}
