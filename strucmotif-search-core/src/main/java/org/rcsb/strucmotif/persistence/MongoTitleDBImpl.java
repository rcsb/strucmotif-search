package org.rcsb.strucmotif.persistence;

import com.google.inject.Inject;
import com.mongodb.DBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;

import java.util.List;

import static com.mongodb.client.model.Filters.eq;

public class MongoTitleDBImpl implements MongoTitleDB {
    private final MongoCollection<DBObject> titles;

    @Inject
    public MongoTitleDBImpl(MongoClientHolder mongoClientHolder) {
        MongoDatabase database = mongoClientHolder.getDatabase();
        titles = database.getCollection("titles", DBObject.class);
    }

    @Override
    public String selectTitle(String pdbId) {
        return (String) titles.find(eq("_id", pdbId)).first().get("v");
    }


    @Override
    public void insertTitles(List<DBObject> titles) {
        this.titles.insertMany(titles);
    }

    @Override
    public void deleteTitle(StructureIdentifier pdbId) {
        titles.deleteOne(eq("_id", pdbId.getPdbId()));
    }
}
