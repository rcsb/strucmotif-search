package org.rcsb.strucmotif.persistence;

import com.mongodb.BasicDBList;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import java.util.List;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.eq;

public class MongoResidueDBImpl implements ResidueDB {
    private final MongoClient mongoClient;
    private final MongoCollection<DBObject> titles;
    private final MongoCollection<DBObject> residues;

    public MongoResidueDBImpl() {
        this.mongoClient = new MongoClient();
        MongoDatabase database = mongoClient.getDatabase("motif");
        titles = database.getCollection("titles", DBObject.class);
        residues = database.getCollection("components", DBObject.class);

        // register 'auto'-close of MongoDB connection
        Runtime.getRuntime().addShutdownHook(new Thread(mongoClient::close));
    }

    @Override
    public String selectTitle(String pdbId) {
        return (String) titles.find(eq("_id", pdbId)).first().get("v");
    }

    @Override
    public BasicDBList selectResidue(String pdbId, int assemblyId, int index) {
        return (BasicDBList) residues.find(eq("_id", pdbId + ":" + assemblyId + ":" + index)).first().get("v");
    }

    @Override
    public void insertTitles(List<DBObject> titles) {
        this.titles.insertMany(titles);
    }

    @Override
    public void insertResidues(List<DBObject> components) {
        this.residues.insertMany(components);
    }

    @Override
    public void deleteTitle(String pdbId) {
        titles.deleteOne(eq("_id", pdbId));
    }

    @Override
    public void deleteResidues(String pdbId) {
        // pdbId is at start of all components to remove
        Pattern pattern = Pattern.compile("^" + pdbId);
        residues.deleteMany(Filters.regex("_id", pattern));
    }

    @Override
    public void close() {
        mongoClient.close();
    }
}
