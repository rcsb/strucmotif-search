package org.rcsb.strucmotif.persistence;

import com.google.inject.Singleton;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import org.rcsb.strucmotif.MotifSearch;

@Singleton
public class MongoClientHolderImpl implements MongoClientHolder {
    private final MongoDatabase database;

    public MongoClientHolderImpl() {
        MongoClient mongoClient;
        if (MotifSearch.DB_CONNECTION_URI != null) {
            MongoClientURI uri = new MongoClientURI(MotifSearch.DB_CONNECTION_URI);
            mongoClient = new MongoClient(uri);
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
