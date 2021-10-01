# Running Updates
Before searching, you need to add structure and inverted index data. Use the `MotifSearchUpdate` class to perform an
update.

## Update Structure and Inverted Index Data
Strucmotif-search comes with all logic needed to run weekly updates that mirror PDB releases. The client obtains the
current holdings and can perform incremental/delta loads that add the newly released structures.

### Loading Current RCSB PDB Holdings
```java
class Demo {
    public static void main(String[] args) {
        // perform a full load of all structures in RCSB PDB with default configuration
        MotifSearchUpdate.main(new String[] { "ADD", "full" });
    }
}
```

Supported operations are `ADD` and `REMOVE`. Either process all current PDB structures (`full`) or provide an array of
entry IDs you want to process (e.g., `"4HHB", "1MUW", "1EXR"`).

### Loading Non-Archived Structures
It's also possible to load and index non-archived structures such as computed structure models, e.g. from the AlphaFold
database or by pointing to local files.

```java
class Demo {
    public static void main(String[] args) {
        MotifSearchUpdate.main(new String[] { "ADD",
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

Usually, non-archived files also lack assembly information. In that case, set `undefined-assemblies` to `true` to allow
indexing and searching in the deposited coordinates. Hits will be identified by assembly-ID `0`.

Also, you likely want to ignore low-confidence predictions to avoid false-positives as well as to save storage and 
lower the processing time of queries. Use `residue-quality-strategy` and `residue-quality-cutoff` for that.

## Configuration
Several application properties are only relevant for the update part of the application. Changes to these parameters 
might require a full load.

| Property     | Action | Default Value/Behavior |
| -----------  | ------ | ------- |
| `cif-fetch-url` | URL template for (Binary)CIF download | RCSB PDB BinaryCIF |
| `data-source` | Path to local CIF archive | cif-fetch-url |
| `distance-cutoff` | Maximum distance between alpha carbons that will be indexed in Å | `15` |
| `download-tries` | Number of tries to download structure data during update | `1` |
| `inverted-index-gzip` | Gzip index files? | `false` |
| `number-threads` | Number of worker threads | available processors |
| `renumbered-coordinate-precision` | Coordinate precision of BinaryCIF files | `1` |
| `renumbered-gzip` | Gzip BinaryCIF files? | `true` |
| `residue-quality-cutoff` | Filter for residues with meaningful quality - combine with `residue-quality-strategy` | `70.0` |
| `residue-quality-strategy` | Filter for residues with meaningful quality - combine with `residue-quality-cutoff` | `NONE` |
| `root-path` | Path where data files will be written | `/opt/data/` |
| `undefined-assemblies` | Index structures without assembly information? | `false` |
| `update-chunk-size` | Writing to the inverted index is slow and therefore done in chunks | `400` |

Configure by placing your `application.properties` on the classpath.