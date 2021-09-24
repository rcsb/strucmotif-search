package org.rcsb.strucmotif.update;

class Demo {
    public static void main(String[] args) {
        // perform a full load of all structures in RCSB PDB with default configuration
        MotifSearchUpdate.main(new String[] { "ADD", "full" });

        // load some arbitrary (external) structure data like AlphaFold models
        MotifSearchUpdate.main(new String[] {
                "ADD",
                "AF-Q76EI6-F1,https://alphafold.ebi.ac.uk/files/AF-Q76EI6-F1-model_v1.cif",
                "AF-Q8W3K0-F1,https://alphafold.ebi.ac.uk/files/AF-Q8W3K0-F1-model_v1.cif",
                "AF-Q9CPN9-F1,https://alphafold.ebi.ac.uk/files/AF-Q9CPN9-F1-model_v1.cif",
                "AF-P07477-F1,https://alphafold.ebi.ac.uk/files/AF-P07477-F1-model_v1.cif",
                "AF-Q8NHM4-F1,https://alphafold.ebi.ac.uk/files/AF-Q8NHM4-F1-model_v1.cif",
                "MA-9Z55Z,file:///Users/sebastian/Downloads/ma-9z55z.cif",
                "MA-AGHOX,file:///Users/sebastian/Downloads/ma-aghox.cif"
        });
    }
}