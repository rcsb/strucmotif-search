package org.rcsb.strucmotif.persistence;

import com.mongodb.DBObject;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;

import java.util.List;

/**
 * Access to protein titles (and potentially other entry-specific information).
 */
public interface MongoTitleDB {
    String selectTitle(String pdbId);

    void insertTitles(List<DBObject> titles);

    void deleteTitle(StructureIdentifier pdbId);
}
