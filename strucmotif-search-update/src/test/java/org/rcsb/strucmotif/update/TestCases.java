package org.rcsb.strucmotif.update;

import java.io.InputStream;
import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * Some small structures and their expected properties when processed by the update routine.
 */
enum TestCases {
    PDB_2RLL("2RLL.cif.gz"),
    PDB_3ULN("3uln.cif.gz"),
    PDB_3UM4("3UM4.cif"),
    PDB_4TUT("4TUT.bcif.gz"),
    PDB_5XES("5xes.bcif"),
    PDB_6FCE("6fce.cif"),
    AF_A0A0R0FWM3("af_afa0a0r0fwm3f1.bcif"), // helix, fragmented
    AF_Q8SY76("af_afq8sy76f1.bcif"); // helix

    private final String filename;
    private final String key;

    TestCases(String name) {
        this.filename = name;
        this.key = filename.split("\\.")[0].replace("-model_v1", "");
    }

    public String getFilename() {
        return filename;
    }

    public String getKey() {
        return key;
    }

    public InputStream getInputStream() {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(this.filename);
    }

    public static InputStream getInputStream(String key) {
        return Arrays.stream(TestCases.values())
                .filter(t -> t.getKey().equalsIgnoreCase(key))
                .findFirst()
                .map(TestCases::getInputStream)
                .orElseThrow(() -> new NoSuchElementException("Couldn't find resource for case '" + key + "'"));
    }

    public String getExpression() {
        if (filename.equalsIgnoreCase(key)) {
            return key;
        } else {
            return key + ",https://rcsb.org"; // any URL works here
        }
    }
}
