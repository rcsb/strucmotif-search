package org.rcsb.strucmotif.persistence;

import com.google.inject.Singleton;
import com.mongodb.BasicDBList;
import com.mongodb.DBObject;
import org.rcsb.strucmotif.domain.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Use locally stored data that we expect to use for tests.
 * The uncommented lines can be used to create the mock data.
 */
@Singleton
public class MockMongoResidueDBImpl extends MongoResidueDBImpl {
    // TODO recreate mock data
    private static final Logger logger = LoggerFactory.getLogger(MockMotifLookupImpl.class);
    private final Map<String, String> titles;
    private final Map<String, BasicDBList> components;
//    private final MongoCollection<DBObject> titles2;
//    private final MongoCollection<DBObject> components2;

    public MockMongoResidueDBImpl() throws IOException {
        logger.debug("Mocking residue-DB");

//        MongoClient mongoClient = new MongoClient();
//        MongoDatabase database = mongoClient.getDatabase("motif");
//        titles2 = database.getCollection("titles", DBObject.class);
//        components2 = database.getCollection("components", DBObject.class);

        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("mongo/titles.csv")) {
            this.titles = new BufferedReader(new InputStreamReader(Objects.requireNonNull(inputStream)))
                    .lines()
                    .map(line -> new Pair<>(line.substring(0, 4), line.substring(4)))
                    .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
        }

        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("mongo/components.csv")) {
            this.components = new BufferedReader(new InputStreamReader(Objects.requireNonNull(inputStream)))
                    .lines()
                    .map(line -> line.replace("]", ""))
                    .map(line -> line.split("\\["))
                    .map(split -> new Pair<>(split[0], handleArray(split[1])))
                    .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
        }
    }

    private BasicDBList handleArray(String raw) {
        String[] split = raw.split(", ");
        BasicDBList list = new BasicDBList();
        list.add(split[0]);
        list.add(Integer.parseInt(split[1]));
        list.add(split[2]);
        for (int i = 3; i < split.length; i = i + 4) {
            list.add(split[i]);
            list.add(Integer.parseInt(split[i + 1]));
            list.add(Integer.parseInt(split[i + 2]));
            list.add(Integer.parseInt(split[i + 3]));
        }
        return list;
    }

    @Override
    public String selectTitle(String pdbId) {
//        System.out.println(pdbId + titles2.find(eq("_id", pdbId)).first().get("v"));
        return titles.get(pdbId);
    }

    @Override
    public BasicDBList selectResidue(String pdbId, int assemblyId, int index) {
//        if(components.get(pdbId + ":" + assemblyId + ":" + index) == null) {
//            System.out.println(pdbId + ":" + assemblyId + ":" + index + components2.find(eq("_id", pdbId + ":" + assemblyId + ":" + index)).first().get("v"));
//        }
        return components.get(pdbId + ":" + assemblyId + ":" + index);
    }

    @Override
    public void insertTitles(List<DBObject> titles) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void insertResidues(List<DBObject> components) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteTitle(String pdbId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteResidues(String pdbId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        // not needed
    }
}
