package org.rcsb.strucmotif.domain.align;

/**
 * The result of an alignment.
 */
public record AlignmentResult(float[] transformation, float rmsd) {}
