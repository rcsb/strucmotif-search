package org.rcsb.strucmotif.persistence;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.conversions.Bson;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.pullAll;
import static com.mongodb.client.model.Updates.pushEach;

@Service
public class MongoUpdateStateManager implements UpdateStateManager {
    // all 'known' entry identifiers, this is a superset of 'structures' as e.g. alpha carbon traces will fail processing
    private static final String REGISTERED_KEY = "registered";
    // all 'healthy' structures that can appear as search results
    private static final String STRUCTURES_KEY = "structures";
    // all structures registered in the inverted index (this should be equal to 'structures')
    private static final String INDEX_KEY = "index";
    private final MongoCollection<DBObject> state;

    @Autowired
    public MongoUpdateStateManager(MongoClientHolder mongoClientHolder) {
        MongoDatabase database = mongoClientHolder.getDatabase();
        state = database.getCollection("state", DBObject.class);
    }

    @Override
    public Collection<StructureIdentifier> selectArchiveEntries() {
        return select(REGISTERED_KEY);
    }

    @Override
    public Collection<StructureIdentifier> selectResidueDBEntries() {
        return select(STRUCTURES_KEY);
    }

    @Override
    public Collection<StructureIdentifier> selectInvertedIndexEntries() {
        return select(INDEX_KEY);
    }

    private Collection<StructureIdentifier> select(String key) {
        return ((BasicDBList) state.find(eq("_id", key)).first().get("v")).stream()
                .map(String.class::cast)
                .map(StructureIdentifier::new)
                .collect(Collectors.toSet());
    }

    @Override
    public void insertArchiveEntries(Collection<StructureIdentifier> additions) {
        insert(additions, REGISTERED_KEY);
    }

    @Override
    public void insertResidueDBEntries(Collection<StructureIdentifier> additions) {
        insert(additions, STRUCTURES_KEY);
    }

    @Override
    public void insertInvertedIndexEntries(Collection<StructureIdentifier> additions) {
        insert(additions, INDEX_KEY);
    }

    private void insert(Collection<StructureIdentifier> additions, String key) {
        Bson filter = eq("_id", key);

        // ensure document exists
        if (state.countDocuments(filter) == 0) {
            state.insertOne(new BasicDBObject("_id", key));
        }

        List<String> update = additions.stream().map(StructureIdentifier::getPdbId).collect(Collectors.toList());
        state.findOneAndUpdate(filter, pushEach("v", update));
    }

    @Override
    public void deleteArchiveEntries(Collection<StructureIdentifier> removals) {
        delete(removals, REGISTERED_KEY);
    }

    @Override
    public void deleteResidueDBEntries(Collection<StructureIdentifier> removals) {
        delete(removals, STRUCTURES_KEY);
    }

    @Override
    public void deleteInvertedIndexEntries(Collection<StructureIdentifier> removals) {
        delete(removals, INDEX_KEY);
    }

    private void delete(Collection<StructureIdentifier> removals, String key) {
        state.findOneAndUpdate(eq("_id", key),
                pullAll("v", removals.stream().map(StructureIdentifier::getPdbId).collect(Collectors.toList())));
    }
}
