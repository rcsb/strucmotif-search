package org.rcsb.strucmotif.persistence;

import com.google.inject.Singleton;
import com.mongodb.BasicDBList;
import com.mongodb.DBObject;

import java.util.List;

/**
 * The MongoResidueDB holds coordinates for all residues in the archive. This allows fast access to certain groups by
 * pdbId, assemblyId, and residue index.
 */
@Singleton
public interface MongoResidueDB {
    BasicDBList selectResidue(String pdbId, int assemblyId, int index);

    void insertResidues(List<DBObject> components);

    void deleteResidues(String pdbId);
}
