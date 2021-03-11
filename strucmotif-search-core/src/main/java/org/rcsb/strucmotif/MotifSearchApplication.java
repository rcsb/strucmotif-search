package org.rcsb.strucmotif;

import org.rcsb.strucmotif.domain.query.QueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;

/**
 * The entry point of the strucmotif-search application.
 */
@SpringBootApplication(exclude = { MongoAutoConfiguration.class, MongoDataAutoConfiguration.class })
@EntityScan("org.rcsb.strucmotif")
public class MotifSearchApplication {
    static QueryBuilder queryBuilder;

    /**
     * Default entry point.
     * @param args arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(MotifSearchApplication.class, args);
    }

    @Autowired
    public MotifSearchApplication(QueryBuilder queryBuilder) {
        MotifSearchApplication.queryBuilder = queryBuilder;
    }
}
