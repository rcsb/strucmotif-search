package org.rcsb.strucmotif.update;

class Demo {
    public static void main(String[] args) {
        // perform a full load of all structures in RCSB PDB with default configuration
//        MotifSearchUpdate.main(new String[] { "ADD", "full" });

        // load some arbitrary, external structure data like AlphaFold models
        MotifSearchUpdate.main(new String[] {
                "ADD",
                "https://alphafold.ebi.ac.uk/files/AF-Q76EI6-F1-model_v1.cif",
                "https://alphafold.ebi.ac.uk/files/AF-Q8W3K0-F1-model_v1.cif",
                "https://alphafold.ebi.ac.uk/files/AF-Q5VSL9-F1-model_v1.cif",
                "https://alphafold.ebi.ac.uk/files/AF-P26646-F1-model_v1.cif"
        });
    }
}