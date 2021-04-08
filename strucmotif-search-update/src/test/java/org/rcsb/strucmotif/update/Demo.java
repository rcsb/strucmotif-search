package org.rcsb.strucmotif.update;

class Demo {
    public static void main(String[] args) {
        // perform a full load of all structures in RCSB PDB with default configuration
        MotifSearchUpdate.main(new String[] { "ADD", "full" });
    }
}