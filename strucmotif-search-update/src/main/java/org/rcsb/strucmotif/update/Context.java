package org.rcsb.strucmotif.update;

import java.util.Arrays;
import java.util.NoSuchElementException;

public enum Context {
    ARCHIVE,
    RESIDUE,
    INDEX;

    public static Context resolve(String s) {
        String uc = s.toUpperCase();
        return Arrays.stream(Context.values())
                .filter(e -> e.name().equals(uc))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Unrecognized Context: " + s + " - options are: " + Arrays.toString(Context.values())));
    }
}
