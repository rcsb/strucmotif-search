package org.rcsb.strucmotif.update;

import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * The possible operations during a strucmotif update ('ADD' structures, 'REMOVE' structures, 'RECOVER').
 */
public enum Operation {
    /**
     * Add structures.
     */
    ADD,
    /**
     * Remove structures.
     */
    REMOVE,
    /**
     * Try to recover - this is used when the JVM dies while manipulating the inverted index.
     */
    RECOVER;

    /**
     * Map from string to Operation enum.
     * @param s the name
     * @return the corresponding enum entry
     */
    public static Operation resolve(String s) {
        String uc = s.toUpperCase();
        return Arrays.stream(Operation.values())
                .filter(e -> e.name().equals(uc))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Unrecognized Operation: " + s + " - options are: " + Arrays.toString(Operation.values())));
    }
}
