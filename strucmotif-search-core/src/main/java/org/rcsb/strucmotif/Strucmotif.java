package org.rcsb.strucmotif;

import org.rcsb.strucmotif.core.MotifDefinitionRegistry;
import org.rcsb.strucmotif.domain.query.StructureContextBuilder;
import org.rcsb.strucmotif.domain.query.MotifContextBuilder;

/**
 * The entry point to perform strucmotif searches.
 */
public class Strucmotif {
    private final StructureContextBuilder structureContextBuilder;
    private final MotifContextBuilder motifContextBuilder;
    private final MotifDefinitionRegistry motifDefinitionRegistry;
    private static final Strucmotif INSTANCE = new Strucmotif();

    private Strucmotif() {
        StrucmotifApplication.main(new String[0]);
        this.structureContextBuilder = StrucmotifApplication.structureContextBuilder;
        this.motifContextBuilder = StrucmotifApplication.motifContextBuilder;
        this.motifDefinitionRegistry = StrucmotifApplication.motifDefinitionRegistry;
    }

    /**
     * Use a single motif to search for all structures that contain this motif.
     * @return a context builder that allows searching for similar structures
     */
    public static StructureContextBuilder searchForStructures() {
        return INSTANCE.structureContextBuilder;
    }

    /**
     * Use a single structure to detect all known motifs that occur within this structure.
     * @return a context builder that allows detecting motifs
     */
    public static MotifContextBuilder detectMotifs() {
        return INSTANCE.motifContextBuilder;
    }

    /**
     *
     * @return
     */
    public static MotifDefinitionRegistry getMotifDefinitionRegistry() {
        return INSTANCE.motifDefinitionRegistry;
    }
}
