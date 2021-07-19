package org.rcsb.strucmotif2.domain;

/**
 * Defines a relation between 2 objects.
 * @param <F> the first type
 * @param <S> the second type
 */
public class Pair<F, S> {
    private final F first;
    private final S second;

    /**
     * Construct a pair.
     * @param first 1st element
     * @param second 2nd element
     */
    public Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }

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
