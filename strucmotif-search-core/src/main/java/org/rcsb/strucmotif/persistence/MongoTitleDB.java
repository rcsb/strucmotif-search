package org.rcsb.strucmotif.persistence;

import com.mongodb.DBObject;

import java.util.List;

/**
 * Access to protein titles (and potentially other entry-specific information).
 */
public interface MongoTitleDB {
    String selectTitle(String pdbId);

    void insertTitles(List<DBObject> titles);

    void deleteTitle(String pdbId);
}
