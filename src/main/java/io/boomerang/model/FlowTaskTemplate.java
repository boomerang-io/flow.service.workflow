package io.boomerang.model;

import org.springframework.beans.BeanUtils;
import io.boomerang.mongo.entity.FlowTaskTemplateEntity;

public class FlowTaskTemplate extends FlowTaskTemplateEntity {

  public FlowTaskTemplate() {

  }

  public FlowTaskTemplate(FlowTaskTemplateEntity entity) {
    BeanUtils.copyProperties(entity, this);
    if (this.getRevisions() != null) {
      this.setCurrentVersion(this.getRevisions().size());
    } else {
      this.setCurrentVersion(0);
    }
  }

  private long currentVersion;

  public long getCurrentVersion() {
    return currentVersion;
  }

  public void setCurrentVersion(long currentVersion) {
    this.currentVersion = currentVersion;
  }

}
