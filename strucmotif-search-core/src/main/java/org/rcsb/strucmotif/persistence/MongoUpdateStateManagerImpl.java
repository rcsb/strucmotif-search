package org.rcsb.strucmotif.persistence;

import com.google.inject.Inject;
import com.mongodb.DBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;

import java.util.List;

public class MongoUpdateStateManagerImpl implements UpdateStateManager {
    private final MongoCollection<DBObject> archiveState;
    private final MongoCollection<DBObject> residueState;
    private final MongoCollection<DBObject> indexState;

    @Inject
    public MongoUpdateStateManagerImpl(MongoClientHolder mongoClientHolder) {
        MongoDatabase database = mongoClientHolder.getDatabase();
        archiveState = database.getCollection("archive_state", DBObject.class);
        residueState = database.getCollection("residue_state", DBObject.class);
        indexState = database.getCollection("index_state", DBObject.class);
    }

    @Override
    public List<StructureIdentifier> getArchiveEntries() {
        return null;
    }

    @Override
    public List<StructureIdentifier> getResidueDBEntries() {
        return null;
    }

    @Override
    public List<StructureIdentifier> getInvertedIndexEntries() {
        return null;
    }

    @Override
    public void insertArchiveEntries(List<StructureIdentifier> additions) {

    }

    @Override
    public void insertResidueDBEntries(List<StructureIdentifier> additions) {

    }

    @Override
    public void insertInvertedIndexEntries(List<StructureIdentifier> additions) {

    }

    @Override
    public void removeArchiveEntries(List<StructureIdentifier> removals) {

    }

    @Override
    public void removeResidueDBEntries(List<StructureIdentifier> removals) {

    }

    @Override
    public void removeInvertedIndexEntries(List<StructureIdentifier> removals) {

    }
}
