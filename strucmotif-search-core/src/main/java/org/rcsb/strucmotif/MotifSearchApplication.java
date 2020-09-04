package org.rcsb.strucmotif;

import org.rcsb.strucmotif.domain.query.QueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

/**
 * TODO better way of doing this?
 */
@SpringBootApplication
@EntityScan("org.rcsb.strucmotif")
public class MotifSearchApplication implements CommandLineRunner {
    static QueryBuilder queryBuilder;

    public static void main(String[] args) {
        SpringApplication.run(MotifSearchApplication.class, args);
    }

    @Override
    public void run(String... args) {

    }

    @Autowired
    public MotifSearchApplication(QueryBuilder queryBuilder) {
        MotifSearchApplication.queryBuilder = queryBuilder;
    }
}
