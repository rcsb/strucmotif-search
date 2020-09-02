package org.rcsb.strucmotif.persistence;

import com.google.inject.Singleton;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import org.rcsb.strucmotif.MotifSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class MongoClientHolderImpl implements MongoClientHolder {
    private static final Logger logger = LoggerFactory.getLogger(MongoClientHolderImpl.class);
    private final MongoDatabase database;

    public MongoClientHolderImpl() {
        MongoClient mongoClient;
        String uri = MotifSearch.DB_CONNECTION_URI;
        logger.info("Acquiring MongoClient - URI: {}", uri);
        if (uri != null && !uri.isBlank()) {
            mongoClient = new MongoClient(new MongoClientURI(MotifSearch.DB_CONNECTION_URI));
        } else {
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
