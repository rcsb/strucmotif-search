package org.rcsb.strucmotif.persistence;

import com.google.inject.Inject;
import com.mongodb.DBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;

import java.util.List;
import java.util.Set;

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
    public Set<StructureIdentifier> selectArchiveEntries() {
        return null;
    }

    @Override
    public Set<StructureIdentifier> selectResidueDBEntries() {
        return null;
    }

    @Override
    public Set<StructureIdentifier> selectInvertedIndexEntries() {
        return null;
    }

    @Override
    public void insertArchiveEntries(Set<StructureIdentifier> additions) {

    }

    @Override
    public void insertResidueDBEntries(Set<StructureIdentifier> additions) {

    }

    @Override
    public void insertInvertedIndexEntries(Set<StructureIdentifier> additions) {

    }

    @Override
    public void deleteArchiveEntries(Set<StructureIdentifier> removals) {

    }

    @Override
    public void deleteResidueDBEntries(Set<StructureIdentifier> removals) {

    }

    @Override
    public void deleteInvertedIndexEntries(Set<StructureIdentifier> removals) {

    }
}
