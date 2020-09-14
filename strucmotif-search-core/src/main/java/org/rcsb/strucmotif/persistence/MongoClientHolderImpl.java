package org.rcsb.strucmotif.persistence;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class MongoClientHolderImpl implements MongoClientHolder {
    private static final Logger logger = LoggerFactory.getLogger(MongoClientHolderImpl.class);
    private final MongoDatabase database;

    @Autowired
    public MongoClientHolderImpl(MotifSearchConfig motifSearchConfig) {
        MongoClient mongoClient;
        String uri = motifSearchConfig.getDbConnectionUri();
        if (uri != null && !uri.isBlank() && !uri.contains("localhost") && !uri.contains("127.0.0.1")) {
            logger.info("Connecting to remote MongoDB: {}", uri.replaceAll(":[^:]*@", ":<hidden>@"));
            mongoClient = new MongoClient(new MongoClientURI(uri));
        } else {
            logger.info("Connecting to local MongoDB");
            mongoClient = new MongoClient();
        }
        this.database = mongoClient.getDatabase("motif");

        // register 'auto'-close of MongoDB connection
        Runtime.getRuntime().addShutdownHook(new Thread(mongoClient::close));
    }

    @Override
    public MongoDatabase getDatabase() {
        return database;
    }
}
