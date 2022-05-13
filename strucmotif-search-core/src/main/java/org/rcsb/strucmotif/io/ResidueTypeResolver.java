package org.rcsb.strucmotif.io;

import org.rcsb.strucmotif.domain.structure.ResidueType;

/**
 * Maps from generic three-letter-codes to the {@link ResidueType} enum. Depending on the selected strategy, this will
 * completely ignore non-canonical components or map them to something known via their parent.
 */
public interface ResidueTypeResolver {
    /**
     * Map a three-letter-code to a ResidueType.
     * @param threeLetterCode the input
     * @return a ResidueType, might be the unknown component
     */
    ResidueType selectResidueType(String threeLetterCode);
}
