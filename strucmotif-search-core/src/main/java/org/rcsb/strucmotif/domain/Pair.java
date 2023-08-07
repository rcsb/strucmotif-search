package org.rcsb.strucmotif.domain;

/**
 * Defines a relation between 2 objects.
 * @param first 1st element
 * @param second 2nd element
 * @param <F> the first type
 * @param <S> the second type
 */
public record Pair<F, S>(F first, S second) {
    @Override
    public String toString() {
        return first + " - " + second;
    }
}
