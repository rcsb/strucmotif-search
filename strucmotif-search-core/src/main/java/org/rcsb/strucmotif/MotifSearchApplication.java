package org.rcsb.strucmotif;

import org.rcsb.strucmotif.domain.query.QueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

/**
 * TODO better way of doing this?
 */
@SpringBootApplication
@EntityScan("org.rcsb.strucmotif")
public class MotifSearchApplication {
    static QueryBuilder queryBuilder;

    public static void main(String[] args) {
        SpringApplication.run(MotifSearchApplication.class, args);
    }

    @Autowired
    public MotifSearchApplication(QueryBuilder queryBuilder) {
        MotifSearchApplication.queryBuilder = queryBuilder;
    }
}
