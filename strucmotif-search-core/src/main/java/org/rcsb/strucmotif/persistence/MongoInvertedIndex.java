package org.rcsb.strucmotif.persistence;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import com.mongodb.Function;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import org.bson.conversions.Bson;
import org.rcsb.strucmotif.domain.Pair;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;
import org.rcsb.strucmotif.domain.selection.IndexSelection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

import static com.mongodb.client.model.Filters.eq;

@Repository
public class MongoInvertedIndex implements InvertedIndex {
    private static final UpdateOptions UPSERT_OPTIONS = new UpdateOptions().upsert(true);
    private final MongoCollection<DBObject> invertedIndex;

    @Autowired
    public MongoInvertedIndex(MongoClientHolder mongoClientHolder) {
        MongoDatabase database = mongoClientHolder.getDatabase();
        invertedIndex = database.getCollection("index", DBObject.class);
    }

    @Override
    public void insert(ResiduePairDescriptor residuePairDescriptor, Map<StructureIdentifier, Collection<ResiduePairIdentifier>> residuePairOccurrences) {
        String descriptorKey = residuePairDescriptor.toString();
        Bson filter = eq("_id", descriptorKey);

        // ensure document exists
        if (invertedIndex.countDocuments(filter) == 0) {
            invertedIndex.insertOne(new BasicDBObject("_id", descriptorKey));
        }

        BasicDBObjectBuilder mapBuilder = new BasicDBObjectBuilder();
        for (Map.Entry<StructureIdentifier, Collection<ResiduePairIdentifier>> entry : residuePairOccurrences.entrySet()) {
            String structureKey = entry.getKey().getPdbId();
            Object[] values = entry.getValue()
                    .stream()
                    .map(this::createObjectArray)
                    .toArray();
            mapBuilder.add(structureKey, values);
        }

        // perform update for whole batch of entries for this descriptor
        BasicDBObject update = new BasicDBObject("$set", mapBuilder.get());
        invertedIndex.updateOne(filter, update, UPSERT_OPTIONS);
    }

    private Object createObjectArray(ResiduePairIdentifier targetIdentifier) {
        IndexSelection identifier1 = targetIdentifier.getIndexSelection1();
        int assemblyId1 = identifier1.getAssemblyId();
        int index1 = identifier1.getIndex();
        IndexSelection identifier2 = targetIdentifier.getIndexSelection2();
        int assemblyId2 = identifier2.getAssemblyId();
        int index2 = identifier2.getIndex();

        // let's implicitly assume that 2 values only describe indices with assemblyId = 1 being implied
        // I think this tradeoff is worth it to save significant amounts of disk space
        if (assemblyId1 == 1 && assemblyId2 == 1) {
            return new int[] { index1, index2 };
        } else {
            return new int[] { assemblyId1, index1, assemblyId2, index2 };
        }
    }

    @Override
    public Stream<Pair<StructureIdentifier, ResiduePairIdentifier[]>> select(ResiduePairDescriptor residuePairDescriptor) {
        String descriptorKey = residuePairDescriptor.toString();
        Bson filter = eq("_id", descriptorKey);

        DBObject result = invertedIndex.find(filter).first();
        if (result == null) {
            return Stream.empty();
        } else {
            // remove id field (only metadata) to omit downstream filter operation
            result.removeField("_id");
            // PSE can cause identifiers to flip - if so we need to flip them again to ensure correct overlap with other words
            return getResultStream(result, residuePairDescriptor.isFlipped() ?
                    this::createResiduePairIdentifierFlipped : this::createResiduePairIdentifierOriginal);
        }
    }

    @SuppressWarnings("Duplicates")
    private ResiduePairIdentifier createResiduePairIdentifierOriginal(BasicDBList line) {
        int seq1 = (int) line.get(0);
        int seq2 = (int) line.get(1);
        if (line.size() == 2) {
            IndexSelection indexSelector1 = new IndexSelection(seq1);
            IndexSelection indexSelector2 = new IndexSelection(seq2);
            return new ResiduePairIdentifier(indexSelector1, indexSelector2);
        } else {
            int assembly1 = (int) line.get(2);
            int assembly2 = (int) line.get(3);
            IndexSelection indexSelector1 = new IndexSelection(assembly1, seq1);
            IndexSelection indexSelector2 = new IndexSelection(assembly2, seq2);
            return new ResiduePairIdentifier(indexSelector1, indexSelector2);
        }
    }

    @SuppressWarnings("Duplicates")
    private ResiduePairIdentifier createResiduePairIdentifierFlipped(BasicDBList line) {
        int seq1 = (int) line.get(0);
        int seq2 = (int) line.get(1);
        if (line.size() == 2) {
            IndexSelection indexSelector1 = new IndexSelection(seq1);
            IndexSelection indexSelector2 = new IndexSelection(seq2);
            return new ResiduePairIdentifier(indexSelector2, indexSelector1);
        } else {
            int assembly1 = (int) line.get(2);
            int assembly2 = (int) line.get(3);
            IndexSelection indexSelector1 = new IndexSelection(assembly1, seq1);
            IndexSelection indexSelector2 = new IndexSelection(assembly2, seq2);
            return new ResiduePairIdentifier(indexSelector2, indexSelector1);
        }
    }

    private Stream<Pair<StructureIdentifier, ResiduePairIdentifier[]>> getResultStream(DBObject result, Function<BasicDBList, ResiduePairIdentifier> operation) {
        return result.keySet().stream()
                .map(key -> {
                    StructureIdentifier id = new StructureIdentifier(key);
                    BasicDBList list = (BasicDBList) result.get(key);
                    ResiduePairIdentifier[] value = new ResiduePairIdentifier[list.size()];
                    for (int i = 0; i < list.size(); i++) {
                        value[i] = operation.apply((BasicDBList) list.get(i));
                    }
                    return new Pair<>(id, value);
                });
    }

    @Override
    public void delete(ResiduePairDescriptor residuePairDescriptor, Collection<StructureIdentifier> structureIdentifiers) {
        String descriptorKey = residuePairDescriptor.toString();

        BasicDBObjectBuilder builder = new BasicDBObjectBuilder();
        for (StructureIdentifier id : structureIdentifiers) {
            builder.add(id.getPdbId(), 0);
        }

        // this operation is safe if requested id doesn't exist
        invertedIndex.updateOne(eq("_id", descriptorKey), new BasicDBObject("$unset", builder.get()));
    }
}
