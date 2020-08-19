package org.rcsb.strucmotif.persistence;

import com.google.inject.Inject;
import com.mongodb.DBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.rcsb.strucmotif.domain.Pair;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class MongoInvertedIndexImpl implements InvertedIndex {
    private final MongoCollection<DBObject> invertedIndex;

    @Inject
    public MongoInvertedIndexImpl(MongoClientHolder mongoClientHolder) {
        MongoDatabase database = mongoClientHolder.getDatabase();
        invertedIndex = database.getCollection("ii", DBObject.class);
    }

    @Override
    public void insert(ResiduePairDescriptor residuePairDescriptor, Map<String, List<ResiduePairIdentifier>> residuePairOccurrences) {
        throw new UnsupportedOperationException("impl");
    }

    @Override
    public Stream<Pair<StructureIdentifier, ResiduePairIdentifier[]>> select(ResiduePairDescriptor residuePairDescriptor) {
        throw new UnsupportedOperationException("impl");
    }

    @Override
    public void delete(ResiduePairDescriptor residuePairDescriptor, List<String> structureIdentifiers) {
        throw new UnsupportedOperationException("impl");
    }

    @Override
    public void createDirectories() {
        // nothing to do
    }
}
