package org.rcsb.strucmotif.persistence;

import com.google.inject.Inject;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.conversions.Bson;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.pullAll;
import static com.mongodb.client.model.Updates.pushEach;

public class MongoUpdateStateManagerImpl implements UpdateStateManager {
    private static final String ARCHIVE_KEY = "archive";
    private static final String RESIDUE_KEY = "residue";
    private static final String INDEX_KEY = "index";
    private final MongoCollection<DBObject> state;

    @Inject
    public MongoUpdateStateManagerImpl(MongoClientHolder mongoClientHolder) {
        MongoDatabase database = mongoClientHolder.getDatabase();
        state = database.getCollection("state", DBObject.class);
    }

    @Override
    public Set<StructureIdentifier> selectArchiveEntries() {
        return select(ARCHIVE_KEY);
    }

    @Override
    public Set<StructureIdentifier> selectResidueDBEntries() {
        return select(RESIDUE_KEY);
    }

    @Override
    public Set<StructureIdentifier> selectInvertedIndexEntries() {
        return select(INDEX_KEY);
    }

    private Set<StructureIdentifier> select(String key) {
        return ((BasicDBList) state.find(eq("_id", key)).first().get("v")).stream()
                .map(String.class::cast)
                .map(StructureIdentifier::new)
                .collect(Collectors.toSet());
    }

    @Override
    public void insertArchiveEntries(Set<StructureIdentifier> additions) {
        insert(additions, ARCHIVE_KEY);
    }

    @Override
    public void insertResidueDBEntries(Set<StructureIdentifier> additions) {
        insert(additions, RESIDUE_KEY);
    }

    @Override
    public void insertInvertedIndexEntries(Set<StructureIdentifier> additions) {
        insert(additions, INDEX_KEY);
    }

    private void insert(Set<StructureIdentifier> additions, String key) {
        Bson filter = eq("_id", key);

        // ensure document exists
        if (state.countDocuments(filter) == 0) {
            state.insertOne(new BasicDBObject("_id", key));
        }

        List<String> update = additions.stream().map(StructureIdentifier::getPdbId).collect(Collectors.toList());
        state.findOneAndUpdate(filter, pushEach("v", update));
    }

    @Override
    public void deleteArchiveEntries(Set<StructureIdentifier> removals) {
        delete(removals, ARCHIVE_KEY);
    }

    @Override
    public void deleteResidueDBEntries(Set<StructureIdentifier> removals) {
        delete(removals, RESIDUE_KEY);
    }

    @Override
    public void deleteInvertedIndexEntries(Set<StructureIdentifier> removals) {
        delete(removals, INDEX_KEY);
    }

    private void delete(Set<StructureIdentifier> removals, String key) {
        state.findOneAndUpdate(eq("_id", key),
                pullAll("v", removals.stream().map(StructureIdentifier::getPdbId).collect(Collectors.toList())));
    }
}
