package org.rcsb.strucmotif.persistence;

import com.mongodb.BasicDBList;
import com.mongodb.DBObject;

import java.util.List;

public interface ResidueDB {
    String selectTitle(String pdbId);

    BasicDBList selectResidue(String pdbId, int assemblyId, int index);

    void insertTitles(List<DBObject> titles);

    void insertResidues(List<DBObject> components);

    void deleteTitle(String pdbId);

    void deleteResidues(String pdbId);

    void close();
}
