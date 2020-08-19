package org.rcsb.strucmotif.persistence;

import com.google.inject.Singleton;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

@Singleton
public class MongoClientHolderImpl implements MongoClientHolder {
    private final MongoDatabase database;

    public MongoClientHolderImpl() {
        MongoClient mongoClient = new MongoClient();
        this.database = mongoClient.getDatabase("motif");

        // register 'auto'-close of MongoDB connection
        Runtime.getRuntime().addShutdownHook(new Thread(mongoClient::close));
    }

    @Override
    public MongoDatabase getDatabase() {
        return database;
    }
}
