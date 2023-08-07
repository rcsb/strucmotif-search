package org.rcsb.strucmotif.domain.align;

/**
 * The result of an alignment.
 * @param transformation the transformation determined by the alignment
 * @param rootMeanSquareDeviation the alignment score
 */
public record AlignmentResult(float[] transformation, float rootMeanSquareDeviation) {}
