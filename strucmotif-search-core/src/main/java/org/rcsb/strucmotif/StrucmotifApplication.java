package org.rcsb.strucmotif;

import org.rcsb.strucmotif.domain.query.StructureContextBuilder;
import org.rcsb.strucmotif.domain.query.MotifContextBuilder;
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
public class StrucmotifApplication {
    static StructureContextBuilder structureContextBuilder;
    static MotifContextBuilder motifContextBuilder;

    /**
     * Default entry point.
     * @param args arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(StrucmotifApplication.class, args);
    }

    /**
     * Constructor.
     * @param structureContextBuilder injectable context builder (1 motif -> n structures)
     * @param motifContextBuilder injectable context builder (1 structure -> n motifs)
     */
    @Autowired
    public StrucmotifApplication(StructureContextBuilder structureContextBuilder, MotifContextBuilder motifContextBuilder) {
        StrucmotifApplication.structureContextBuilder = structureContextBuilder;
        StrucmotifApplication.motifContextBuilder = motifContextBuilder;
    }
}
