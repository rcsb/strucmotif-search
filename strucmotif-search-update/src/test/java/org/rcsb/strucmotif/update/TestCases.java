package org.rcsb.strucmotif.update;

import java.io.InputStream;

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
    AF_A0A0R0FWM3("AF-A0A0R0FWM3-F1-model_v1.cif"), // helix, fragmented
    AF_Q8SY76("af-q8sy76-f1-model_v1.cif"); // helix

    private final String filename;

    TestCases(String name) {
        this.filename = name;
    }

    public String getFilename() {
        return filename;
    }

    public InputStream getInputStream() {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(this.filename);
    }
}
