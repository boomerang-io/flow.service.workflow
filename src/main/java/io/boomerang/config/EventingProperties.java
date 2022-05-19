package io.boomerang.config;

import java.time.Duration;
import java.util.List;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import io.nats.client.api.StorageType;

@Configuration
@ConfigurationProperties(prefix = "eventing")
public class EventingProperties {

  // Shared properties
  public static class SharedProperties {

    public static class LabelProperties {

      @NotBlank
      private String prefix;

      public String getPrefix() {
        return this.prefix;
      }

      public void setPrefix(String prefix) {
        this.prefix = prefix;
      }
    }

    private LabelProperties label;

    public LabelProperties getLabel() {
      return this.label;
    }

    public void setLabel(LabelProperties label) {
      this.label = label;
    }
  }

  // NATS server related properties
  public static class NatsProperties {

    public static class ServerProperties {

      @Size(min = 1)
      private List<String> urls;

      private Duration reconnectWaitTime;

      @Min(-1)
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

      public void setUrls(List<String> urls) {
        this.urls = urls;
      }

      public void setReconnectWaitTime(Duration reconnectWaitTime) {
        this.reconnectWaitTime = reconnectWaitTime;
      }

      public void setReconnectMaxAttempts(Integer reconnectMaxAttempts) {
        this.reconnectMaxAttempts = reconnectMaxAttempts;
      }
    }

    private ServerProperties server;

    public ServerProperties getServer() {
      return this.server;
    }

    public void setServer(ServerProperties server) {
      this.server = server;
    }
  }

  // NATS Jetstream related properties
  public static class JetstreamProperties {

    public static class StreamProperties {

      @NotBlank
      private String name;

      @NotBlank
      @Pattern(regexp = "^((file)|(memory))$")
      private StorageType storageType;

      @NotBlank
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

      public void setName(String name) {
        this.name = name;
      }

      public void setStorageType(StorageType storageType) {
        this.storageType = storageType;
      }

      public void setSubject(String subject) {
        this.subject = subject;
      }
    }

    public static class ConsumerProperties {

      @NotBlank
      private String name;

      private Duration resubWaitTime;

      public String getName() {
        return this.name;
      }

      public Duration getResubWaitTime() {
        return this.resubWaitTime;
      }

      public void setName(String name) {
        this.name = name;
      }

      public void setResubWaitTime(Duration resubWaitTime) {
        this.resubWaitTime = resubWaitTime;
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

    public void setStream(StreamProperties stream) {
      this.stream = stream;
    }

    public void setConsumer(ConsumerProperties consumer) {
      this.consumer = consumer;
    }

  }

  private Boolean enabled;

  private SharedProperties shared;

  private NatsProperties nats;

  private JetstreamProperties jetstream;

  public Boolean getEnabled() {
    return this.enabled;
  }

  public Boolean isEnabled() {
    return this.enabled;
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

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public void setShared(SharedProperties shared) {
    this.shared = shared;
  }

  public void setNats(NatsProperties nats) {
    this.nats = nats;
  }

  public void setJetstream(JetstreamProperties jetstream) {
    this.jetstream = jetstream;
  }
}
