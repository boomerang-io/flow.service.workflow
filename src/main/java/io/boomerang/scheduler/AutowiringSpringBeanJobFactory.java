package io.boomerang.scheduler;

import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

public class AutowiringSpringBeanJobFactory extends SpringBeanJobFactory {

  private AutowireCapableBeanFactory autowireCapableBeanFactory;

  @Override
  public void setApplicationContext(final ApplicationContext context) {
    autowireCapableBeanFactory = context.getAutowireCapableBeanFactory();
  }

  @Override
  protected Object createJobInstance(final TriggerFiredBundle bundle) throws Exception {
    final Object job = super.createJobInstance(bundle);
    autowireCapableBeanFactory.autowireBean(job);
    return job;
  }

}
