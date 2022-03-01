package org.rcsb.strucmotif.core;

import org.rcsb.strucmotif.domain.AssamSearchContext;
import org.rcsb.strucmotif.domain.SpriteSearchContext;
import org.rcsb.strucmotif.domain.result.AssamHit;
import org.rcsb.strucmotif.domain.result.AssamMotifSearchResult;
import org.rcsb.strucmotif.domain.result.SpriteHit;
import org.rcsb.strucmotif.domain.result.SpriteMotifSearchResult;

import java.util.function.Consumer;

/**
 * Performs motif search queries and returns the corresponding result object.
 */
public interface MotifSearchRuntime {
    void performSearch(AssamSearchContext context);

    void performSearch(AssamSearchContext context, Consumer<AssamHit> consumer);

    void performSearch(SpriteSearchContext context);

    void performSearch(SpriteSearchContext context, Consumer<SpriteHit> consumer);
}
