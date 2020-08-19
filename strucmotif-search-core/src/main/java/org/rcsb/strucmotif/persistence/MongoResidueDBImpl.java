package org.rcsb.strucmotif.persistence;

import com.google.inject.Inject;
import com.mongodb.BasicDBList;
import com.mongodb.DBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import java.util.List;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.eq;

public class MongoResidueDBImpl implements MongoResidueDB {
    private final MongoCollection<DBObject> residues;

    @Inject
    public MongoResidueDBImpl(MongoClientHolder mongoClientHolder) {
        MongoDatabase database = mongoClientHolder.getDatabase();
        residues = database.getCollection("components", DBObject.class);
    }

    @Override
    public BasicDBList selectResidue(String pdbId, int assemblyId, int index) {
        return (BasicDBList) residues.find(eq("_id", pdbId + ":" + assemblyId + ":" + index)).first().get("v");
    }

    @Override
    public void insertResidues(List<DBObject> components) {
        this.residues.insertMany(components);
    }

    @Override
    public void deleteResidues(String pdbId) {
        // pdbId is at start of all components to remove
        Pattern pattern = Pattern.compile("^" + pdbId);
        residues.deleteMany(Filters.regex("_id", pattern));
    }
}
