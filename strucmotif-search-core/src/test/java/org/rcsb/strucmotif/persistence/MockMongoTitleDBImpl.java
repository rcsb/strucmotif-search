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

@Singleton
public class MockMongoTitleDBImpl implements MongoTitleDB {
    private static final Logger logger = LoggerFactory.getLogger(MockMongoTitleDBImpl.class);
    private final Map<String, String> titles;
//    private final MongoCollection<DBObject> titles2;

    public MockMongoTitleDBImpl() throws IOException {
        logger.debug("Mocking title-DB");

        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("mongo/titles.csv")) {
            this.titles = new BufferedReader(new InputStreamReader(Objects.requireNonNull(inputStream)))
                    .lines()
                    .map(line -> new Pair<>(line.substring(0, 4), line.substring(4)))
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
    public void insertTitles(List<DBObject> titles) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteTitle(String pdbId) {
        throw new UnsupportedOperationException();
    }
}
