package io.boomerang.tests;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.util.StringUtils;
import com.mongodb.ConnectionString;
import com.mongodb.WriteConcern;
import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.Defaults;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.config.RuntimeConfig;
import de.flapdoodle.embed.process.config.io.ProcessOutput;
import de.flapdoodle.embed.process.config.store.DownloadConfig;
import de.flapdoodle.embed.process.config.store.HttpProxyFactory;

@Configurable
public class MongoConfig implements InitializingBean, DisposableBean {

  private static String MONGO_IP = "localhost";

  private static final String MONGO_DB = "test_db";

  @Value("${test.mongo.log.enable:false}")
  private boolean enableMongoLogs;

  private MongodExecutable executable;
  private int mongoPort;

  @Override
  public void afterPropertiesSet() throws Exception {
    String proxyHost = System.getProperty("http.proxyHost");
    String proxyPort = System.getProperty("http.proxyPort");

    MongodStarter starter = null;
    ProcessOutput processOutput = enableMongoLogs ? ProcessOutput.getDefaultInstance("mongo")
        : ProcessOutput.getDefaultInstanceSilent();
    Command command = Command.MongoD;
    if (StringUtils.hasText(proxyHost) && StringUtils.hasText(proxyPort)) {

      final DownloadConfig downloadConfig = Defaults.downloadConfigFor(command)
          .proxyFactory(new HttpProxyFactory(proxyHost, Integer.parseInt(proxyPort))).build();

      RuntimeConfig runtimeConfig =
          Defaults.runtimeConfigFor(command).processOutput(processOutput).artifactStore(
              Defaults.extractedArtifactStoreFor(command).withDownloadConfig(downloadConfig))
              .build();

      starter = MongodStarter.getInstance(runtimeConfig);
    } else {
      RuntimeConfig runtimeConfig =
          Defaults.runtimeConfigFor(command).processOutput(processOutput).build();
      starter = MongodStarter.getInstance(runtimeConfig);
    }
    if (starter != null) {

      MongodConfig mongodConfig = MongodConfig.builder().version(Version.Main.PRODUCTION).build();
      executable = starter.prepare(mongodConfig);
      MongodProcess mongodProcess = executable.start();

      this.mongoPort = mongodProcess.getConfig().net().getPort();
    }
  }


  @Bean
  public MongoDatabaseFactory factory() {
    // also possible to connect to a remote or real MongoDB instance
    return new SimpleMongoClientDatabaseFactory(
        new ConnectionString("mongodb://" + MONGO_IP + ":" + mongoPort + "/" + MONGO_DB));
  }


  @Bean
  public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDbFactory) {
    MongoTemplate template = new MongoTemplate(mongoDbFactory);
    template.setWriteConcern(WriteConcern.ACKNOWLEDGED);
    return template;
  }

  @Override
  public void destroy() throws Exception {

  }
}
