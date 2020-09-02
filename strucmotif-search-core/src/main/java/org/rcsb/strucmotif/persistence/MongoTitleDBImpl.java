package org.rcsb.strucmotif.persistence;

import com.mongodb.DBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.mongodb.client.model.Filters.eq;

@Service
public class MongoTitleDBImpl implements MongoTitleDB {
    private final MongoCollection<DBObject> titles;

    @Autowired
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
