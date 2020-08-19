package org.rcsb.strucmotif.persistence;

import com.mongodb.client.MongoDatabase;

/**
 * Handles MongoDB connection. Supports injection, tries to gracefully close DB connection on termination.
 */
public interface MongoClientHolder {
    MongoDatabase getDatabase();
}
