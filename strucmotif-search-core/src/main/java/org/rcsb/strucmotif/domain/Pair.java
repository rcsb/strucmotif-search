package org.rcsb.strucmotif.domain;

/**
 * Defines a relation between 2 objects.
 * @param <F> the first type
 * @param <S> the second type
 */
public record Pair<F, S>(F first, S second) {
    /**
     * The first element.
     * @return the wrapped object
     */
    public F getFirst() {
        return first;
    }

    /**
     * The second element.
     * @return the wrapped object
     */
    public S getSecond() {
        return second;
    }

    @Override
    public String toString() {
        return first + " - " + second;
    }
}
