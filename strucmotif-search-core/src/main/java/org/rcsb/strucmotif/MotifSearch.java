package org.rcsb.strucmotif;

import org.rcsb.strucmotif.domain.query.AssamContextBuilder;
import org.rcsb.strucmotif.domain.query.SpriteContextBuilder;

/**
 * The entry point to perform motif searches.
 */
public class MotifSearch {
    private final AssamContextBuilder assamQueryBuilder;
    private final SpriteContextBuilder spriteQueryBuilder;
    private static final MotifSearch INSTANCE = new MotifSearch();

    private MotifSearch() {
        MotifSearchApplication.main(new String[0]);
        this.assamQueryBuilder = MotifSearchApplication.assamQueryBuilder;
        this.spriteQueryBuilder = MotifSearchApplication.spriteQueryBuilder;
    }

    /**
     * Start a new query using the {@link AssamContextBuilder}.
     * @return a fresh builder instance
     */
    public static AssamContextBuilder assam() {
        return INSTANCE.assamQueryBuilder;
    }

    public static SpriteContextBuilder sprite() {
        return INSTANCE.spriteQueryBuilder;
    }
}
