# Running Updates
Before searching, you need to add structure and inverted index data. Use the `StrucmotifUpdate` class to perform an
update.

## Update Structure and Inverted Index Data
Strucmotif-search comes with all logic needed to run weekly updates that mirror PDB releases. The client obtains the
current holdings and can perform incremental/delta loads that add the newly released structures.

### Loading Current RCSB PDB Holdings
```java
class Demo {
    public static void main(String[] args) {
        // perform a full load of all structures in RCSB PDB with default configuration
        StrucmotifUpdate.main(new String[] { "ADD", "full" });
    }
}
```

Supported operations are `ADD` and `REMOVE`. Either process all current PDB structures (`full`) or provide an array of
entry IDs you want to process (e.g., `"4HHB", "1MUW", "1EXR"`). Use `full_csm` to index all experimental PDB structures
as well as all computed structure models integrated into rcsb.org (˜1 million AlphaFold DB structures).

### Loading Non-Archived Structures
It's also possible to load and index non-archived structures such as computed structure models, e.g. from the AlphaFold
database or by pointing to local files.

```java
class Demo {
    public static void main(String[] args) {
        StrucmotifUpdate.main(new String[] { "ADD",
                // provide a unique identifier if pointing to external files
                // written files and entries in the inverted index are identified by this key
                "AF-P07477-F1,https://alphafold.ebi.ac.uk/files/AF-P07477-F1-model_v1.cif",
                "AF-Q8NHM4-F1,https://alphafold.ebi.ac.uk/files/AF-Q8NHM4-F1-model_v1.cif",
                "MA-9Z55Z,https://www.modelarchive.org/api/projects/ma-9z55z?type=basic__model_file_name",
                "MA-AGHOX,https://www.modelarchive.org/api/projects/ma-aghox?type=basic__model_file_name"
        });
    }
}
```

You likely want to ignore low-confidence predictions to avoid false-positives as well as to save storage and lower the 
processing time of queries. Use `residue-quality-strategy` and `residue-quality-cutoff` for that.

### Loading from Directory
It's also possible to index whole directories. The directory will be walked and all CIF/BinaryCIF files will be 
processed. The library will try to extract reasonable keys from the provided file names, this process is based on 
resource-specific prefixes (`AF-` and `MA-` for AlphaFold and ModelArchive, respectively).
```java
class Demo {
    public static void main(String[] args) {
        StrucmotifUpdate.main(new String[] { "ADD", "path", "/opt/data/pdb/" });
    }
}
```

## Configuration
Several application properties are only relevant for the update part of the application. Changes to these parameters 
might require a full load.

| Property                     | Action                                                                                | Default Value/Behavior         |
|------------------------------|---------------------------------------------------------------------------------------|--------------------------------|
| `ambiguous-monomer-strategy` | How to resolve ASX and GLX?                                                           | `TYPE`                         |
| `ccd-url`                    | URL to the chemical component dictionary                                              | wwPDB                          |
| `cif-fetch-url`              | URL template for (Binary)CIF download                                                 | RCSB PDB BinaryCIF             |
| `commit-interval`            | How many entries to process before committing to index                                | `250,000`                      |
| `data-source`                | Path to local CIF archive                                                             | cif-fetch-url                  |
| `distance-cutoff`            | Maximum distance between alpha carbons that will be indexed in Å                      | `20`                           |
| `download-tries`             | Number of tries to download structure data during update                              | `3`                            |
| `missing-assembly-strategy`  | How to handle missing assembly information                                            | `WARN`                         |
| `missing-revision-strategy`  | How to handle missing revision history                                                | `WARN`                         |
| `modified-residue-strategy`  | How to resolve the parent of modified residues?                                       | `INTERNAL`                     |
| `residue-pair-strategy`      | How to index transformed residues?                                                    | `RESIDUES_IN_CONTACT`          |
| `residue-quality-cutoff`     | Filter for residues with meaningful quality - combine with `residue-quality-strategy` | `70.0`                         |
| `residue-quality-strategy`   | Filter for residues with meaningful quality - combine with `residue-quality-cutoff`   | `qa_metric_local_above_cutoff` |
| `root-path`                  | Path where data files will be written                                                 | `/opt/data/`                   |
| `support-d-amino-acids`      | Map D-amino acids to their L-counterpart                                              | `true`                         |

Configure by placing your `application.properties` on the classpath. All properties specific to this project must be
prefixed with `strucmotif.`.