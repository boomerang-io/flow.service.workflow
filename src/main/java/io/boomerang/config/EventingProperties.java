package io.boomerang.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import io.nats.client.api.StorageType;

@ConfigurationProperties(prefix = "eventing")
public class EventingProperties {

  // Properties related to the subject
  public static class SubjectProperties {

    private String prefix;

    public String getPrefix() {
      return this.prefix;
    }
  }

  // Shared properties
  public static class SharedProperties {

    public static class LabelProperties {

      private String prefix;

      public String getPrefix() {
        return this.prefix;
      }
    }

    private LabelProperties label;

    public LabelProperties getLabel() {
      return this.label;
    }
  }

  // NATS server related properties
  public static class NatsProperties {

    public static class ServerProperties {

      private List<String> urls;

      private Duration reconnectWaitTime;

      private Integer reconnectMaxAttempts;

      public List<String> getUrls() {
        return this.urls;
      }

      public Duration getReconnectWaitTime() {
        return this.reconnectWaitTime;
      }

      public Integer getReconnectMaxAttempts() {
        return this.reconnectMaxAttempts;
      }
    }

    private ServerProperties server;

    public ServerProperties getServer() {
      return this.server;
    }
  }

  // NATS Jetstream related properties
  public static class JetstreamProperties {

    public static class StreamProperties {

      private String name;

      private StorageType storageType;

      private String subject;

      public String getName() {
        return this.name;
      }

      public StorageType getStorageType() {
        return this.storageType;
      }

      public String getSubject() {
        return this.subject;
      }
    }

    public static class ConsumerProperties {

      private String name;

      private Duration resubWaitTime;

      public String getName() {
        return this.name;
      }

      public Duration getResubWaitTime() {
        return this.resubWaitTime;
      }
    }

    private StreamProperties stream;

    private ConsumerProperties consumer;

    public StreamProperties getStream() {
      return this.stream;
    }

    public ConsumerProperties getConsumer() {
      return this.consumer;
    }
  }

  private Boolean enabled;

  private SubjectProperties subject;

  private SharedProperties shared;

  private NatsProperties nats;

  private JetstreamProperties jetstream;


  public Boolean getEnabled() {
    return this.enabled;
  }

  public Boolean isEnabled() {
    return this.enabled;
  }

  public SubjectProperties getSubject() {
    return this.subject;
  }

  public SharedProperties getShared() {
    return this.shared;
  }

  public NatsProperties getNats() {
    return this.nats;
  }

  public JetstreamProperties getJetstream() {
    return this.jetstream;
  }
}
