package io.boomerang.model.ref;

import java.util.Date;

public class ChangeLog {
  private String author;
  private String reason;
  private Date date;

  public ChangeLog() {
  }

  public ChangeLog(String reason) {
    super();
    this.reason = reason;
    this.date = new Date();
  }

  public ChangeLog(String author, String reason) {
    super();
    this.author = author;
    this.reason = reason;
    this.date = new Date();
  }

  @Override
  public String toString() {
    return "ChangeLog [author=" + author + ", reason=" + reason + ", date=" + date + "]";
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }
}
