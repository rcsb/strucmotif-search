package org.rcsb.strucmotif.update;

class Demo {
    public static void main(String[] args) {
        // perform a full load of all structures in RCSB PDB with default configuration
        // use 'full_csm' to also fetch all computed structure models integrated into RCSB.org (~200k experimentally-determined structures, ~1M CSMs)
        StrucmotifUpdate.main(new String[] { "ADD", "full" });

        // load some arbitrary (external) structure data like AlphaFold models
        StrucmotifUpdate.main(new String[] {
                "ADD",
                "AF-Q76EI6-F1,https://alphafold.ebi.ac.uk/files/AF-Q76EI6-F1-model_v4.cif",
                "ma-bak-cepc-0886,https://www.modelarchive.org/api/projects/ma-bak-cepc-0886?type=basic__model_file_name",
        });
    }
}