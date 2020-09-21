package org.rcsb.strucmotif.domain;

/**
 * Defines a relation between 2 objects.
 * @param <F> the first type
 * @param <S> the second type
 */
public class Pair<F, S> {
    private final F first;
    private final S second;

    public Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    public F getFirst() {
        return first;
    }

    public S getSecond() {
        return second;
    }

    @Override
    public String toString() {
        return first + " - " + second;
    }
}
