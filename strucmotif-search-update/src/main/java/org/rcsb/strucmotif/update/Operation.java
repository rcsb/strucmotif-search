package org.rcsb.strucmotif.update;

import java.util.Arrays;
import java.util.NoSuchElementException;

public enum Operation {
    ADD,
    REMOVE;

    public static Operation resolve(String s) {
        String uc = s.toUpperCase();
        return Arrays.stream(Operation.values())
                .filter(e -> e.name().equals(uc))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Unrecognized Operation: " + s + " - options are: " + Arrays.toString(Operation.values())));
    }
}
