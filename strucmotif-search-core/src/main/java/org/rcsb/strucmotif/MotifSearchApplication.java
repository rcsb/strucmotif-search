package org.rcsb.strucmotif;

import org.rcsb.strucmotif.domain.query.AssamContextBuilder;
import org.rcsb.strucmotif.domain.query.SpriteContextBuilder;
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
    static AssamContextBuilder assamQueryBuilder;
    static SpriteContextBuilder spriteQueryBuilder;

    /**
     * Default entry point.
     * @param args arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(MotifSearchApplication.class, args);
    }

    /**
     * Constructor.
     * @param assamQueryBuilder injectable query builder (1 motif -> n structures)
     * @param spriteQueryBuilder injectable query builder (1 structure -> n motifs)
     */
    @Autowired
    public MotifSearchApplication(AssamContextBuilder assamQueryBuilder, SpriteContextBuilder spriteQueryBuilder) {
        MotifSearchApplication.assamQueryBuilder = assamQueryBuilder;
        MotifSearchApplication.spriteQueryBuilder = spriteQueryBuilder;
    }
}
