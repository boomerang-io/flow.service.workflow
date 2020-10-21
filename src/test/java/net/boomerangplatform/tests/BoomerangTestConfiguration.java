package net.boomerangplatform.tests;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import com.mongodb.MongoClient;
import de.flapdoodle.embed.mongo.MongoImportExecutable;
import de.flapdoodle.embed.mongo.MongoImportProcess;
import de.flapdoodle.embed.mongo.MongoImportStarter;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongoImportConfig;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongoCmdOptionsBuilder;
import de.flapdoodle.embed.mongo.config.MongoImportConfigBuilder;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.distribution.GenericVersion;
import de.flapdoodle.embed.process.runtime.Network;

@Component
@Profile("embedded")
public class BoomerangTestConfiguration {

  private static final String BIND_IP = "localhost";
  private static final int PORT = 13345;
  private static final Pattern pattern = Pattern.compile("[.][^.]+$");

  private String database = "local";
  private MongodProcess mongod;
  private MongoClient mongo;

  @PreDestroy
  public void cleanup() {
    mongo.close();
    mongod.stop();
  }

  @PostConstruct
  public void initializeLocalMongoConnection() throws IOException {

    MongodStarter starter = MongodStarter.getDefaultInstance();
    IFeatureAwareVersion version = de.flapdoodle.embed.mongo.distribution.Versions
        .withFeatures(new GenericVersion("4.0.0"), Version.Main.PRODUCTION.getFeatures());
    IMongodConfig mongodConfig =
        new MongodConfigBuilder().version(version).withLaunchArgument("--replSet", "fancy")
            .cmdOptions(new MongoCmdOptionsBuilder().useNoJournal(false).build())
            .net(new Net(BIND_IP, PORT, Network.localhostIsIPv6())).build();

    MongodExecutable mongodExecutable = starter.prepare(mongodConfig);
    mongod = mongodExecutable.start();
    mongo = new MongoClient(BIND_IP, PORT);
  }

  public void loadSampleData() throws IOException {
    ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

    Resource[] resources = resourcePatternResolver.getResources("classpath:db/*.json");

    for (Resource jsonFile : resources) {
      String filepath = jsonFile.getFile().getAbsolutePath();

      Matcher matcher = pattern.matcher(jsonFile.getFilename());
      String collectionName = matcher.replaceFirst("");
      startMongoImport(database, collectionName, filepath, false, true, true);
    }
  }

  private MongoImportProcess startMongoImport(String dbName, String collection, String jsonFile,
      Boolean jsonArray, Boolean upsert, Boolean drop) throws IOException {
    IMongoImportConfig mongoImportConfig = new MongoImportConfigBuilder()
        .version(Version.Main.PRODUCTION).net(new Net(BIND_IP, PORT, Network.localhostIsIPv6()))
        .db(dbName).collection(collection).upsert(upsert).dropCollection(drop).jsonArray(jsonArray)
        .importFile(jsonFile).build();

    MongoImportExecutable mongoImportExecutable =
        MongoImportStarter.getDefaultInstance().prepare(mongoImportConfig);

    return mongoImportExecutable.start();
  }

}
