package io.boomerang.config;

import java.time.Duration;
import java.util.Arrays;
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

  // Generic Jetstream stream properties
  public static class GenericStreamProperties {

    @NotBlank
    private String name;

    @NotBlank
    @Pattern(regexp = "^((file)|(memory))$")
    private StorageType storageType;

    @Size(min = 1)
    private String[] subjects;

    public String getName() {
      return this.name;
    }

    public StorageType getStorageType() {
      return this.storageType;
    }

    public String[] getSubjects() {
      return this.subjects;
    }

    public void setName(String name) {
      this.name = name;
    }

    public void setStorageType(StorageType storageType) {
      this.storageType = storageType;
    }

    public void setSubjects(String[] subjects) {
      this.subjects = subjects;
    }
  }

  // Generic Jetstream consumer properties
  public static class GenericConsumerProperties {

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
      private String[] urls;

      private Duration reconnectWaitTime;

      @Min(-1)
      private Integer reconnectMaxAttempts;

      public String[] getUrls() {
        return this.urls;
      }

      public Duration getReconnectWaitTime() {
        return this.reconnectWaitTime;
      }

      public Integer getReconnectMaxAttempts() {
        return this.reconnectMaxAttempts;
      }

      public void setUrls(String[] urls) {
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

    // NATS Jetstream input events properties
    public static class InputEventsProperties {

      private GenericStreamProperties stream;

      private GenericConsumerProperties consumer;

      public GenericStreamProperties getStream() {
        return this.stream;
      }

      public GenericConsumerProperties getConsumer() {
        return this.consumer;
      }

      public void setStream(GenericStreamProperties stream) {
        this.stream = stream;
      }

      public void setConsumer(GenericConsumerProperties consumer) {
        this.consumer = consumer;
      }
    }

    // NATS Jetstream output events properties
    public static class OutputEventsProperties {

      // Output events subject pattern properties
      public static class SubjectPatternProperties {

        private List<String> workflowStatus;

        private List<String> taskStatus;

        public List<String> getWorkflowStatus() {
          return this.workflowStatus;
        }

        public void setWorkflowStatus(String workflowStatus) {
          this.workflowStatus = Arrays.asList(workflowStatus.split("\\."));
        }

        public List<String> getTaskStatus() {
          return this.taskStatus;
        }

        public void setTaskStatus(String taskStatus) {
          this.taskStatus = Arrays.asList(taskStatus.split("\\."));
        }
      }

      private GenericStreamProperties stream;

      private SubjectPatternProperties subjectPattern;

      public GenericStreamProperties getStream() {
        return this.stream;
      }

      public void setStream(GenericStreamProperties stream) {
        this.stream = stream;
      }

      public SubjectPatternProperties getSubjectPattern() {
        return this.subjectPattern;
      }

      public void setSubjectPattern(SubjectPatternProperties subjectPattern) {
        this.subjectPattern = subjectPattern;
      }
    }

    private InputEventsProperties inputEvents;

    private OutputEventsProperties outputEvents;

    public InputEventsProperties getInputEvents() {
      return this.inputEvents;
    }

    public void setInputEvents(InputEventsProperties inputEvents) {
      this.inputEvents = inputEvents;
    }

    public OutputEventsProperties getOutputEvents() {
      return this.outputEvents;
    }

    public void setOutputEvents(OutputEventsProperties outputEvents) {
      this.outputEvents = outputEvents;
    }
  }

  private SharedProperties shared;

  private NatsProperties nats;

  private JetstreamProperties jetstream;

  public SharedProperties getShared() {
    return this.shared;
  }

  public NatsProperties getNats() {
    return this.nats;
  }

  public JetstreamProperties getJetstream() {
    return this.jetstream;
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
