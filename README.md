[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.rcsb/strucmotif-search/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.rcsb/strucmotif-search)
[![Build Status](https://travis-ci.com/rcsb/strucmotif-search.svg?branch=master)](https://travis-ci.com/rcsb/strucmotif-search)
[![Changelog](https://img.shields.io/badge/changelog--lightgrey.svg?style=flat)](https://github.com/rcsb/strucmotif-search/blob/master/CHANGELOG.md)

# Structural Motif Search
Biochemical and biological functions of proteins are the product of both the overall fold
of the polypeptide chain, and, typically, structural motifs made up of smaller numbers
of amino acids constituting a catalytic center or a binding site that may be remote from
one another in amino acid sequence. Detection of such structural motifs can provide valuable
insights into the function(s) of previously uncharacterized proteins.

![alt motifs](https://raw.githubusercontent.com/rcsb/strucmotif-search/master/motifs.png)

Technically, this remains an extremely challenging problem because of the size of the Protein
Data Bank (PDB) archive. We have developed a new approach that uses an inverted index strategy
capable of analyzing >180,000 PDB structures with unmatched speed. The efficiency of our 
inverted index method depends critically on identifying the small number of structures 
containing the query motif and ignoring most of the structures that are irrelevant. Our 
approach enables real-time retrieval and superposition of structural motifs, either extracted
from a reference structure or uploaded by the user.

## See it in action
Structural motif searching is available as part of the [RCSB Advanced Search](https://www.rcsb.org/search/advanced/strucmotif) and [RCSB Mol* plugin](https://www.rcsb.org/3d-view). [Help documentation is available](https://www.rcsb.org/docs/search/advanced-search/structural-motif-search).

## Performance
Current benchmark times to search in `180,419` structures as of `7/26/21`, 6 cores with 64 GB memory holding all
structure data in memory.

| Motif | Assemblies | 'Paths' Time [ms] | 'Score' Time [ms] |
| --- | --- | --- | --- |
| Serine Protease (HDS) | 4,830 | 776 | 316 |
| Aminopeptidase (KDDDE) | 81 | 563 | 32 |
| Zinc Fingers (CCH) | 446 | 90 | 25 |
| Enolase Superfamily (KDEEH) | 172 | 579 | 11 |
| Enolase Superfamily (KDEEH, exchanges) | 182 | 1,283 | 37 |
| RNA G-Quadruplex (GGGG) | 33 | 1,990 | 1,873 | 

Search for all assemblies that contain hits with an RMSD <1 Å.
'Paths' refers to the time spent on inverted index operations, which identify all candidate structures that contain the 
motif.
'Score' refers to the time spent on aligning candidate structures to the query and computing RMSD values.

## Features
- nucleotide support
- inter-chain & assembly support
- position-specific exchanges
- modified residues

## Getting started with a dependency
strucmotif-search is distributed by maven and supports Java 11+. To get started, append your `pom.xml` by:
```xml
<dependency>
  <groupId>org.rcsb</groupId>
  <artifactId>strucmotif-search</artifactId>
  <version>0.12.0</version>
</dependency>
```

### Search for structural motifs
The `MotifSearch` class provides a fluent API to process structural motif queries.

```java
class Demo {
    public static void main(String[] args) {
        // the entry point for all things motif search - #newQuery() starts building a new query
        MotifSearch.newQuery()
                // several ways can be used to define the query motif - e.g., specify an entry id
                .defineByPdbIdAndSelection("4cha",
                        // and a collection of sequence positions to extract residues
                        List.of(new LabelSelection("B", "1", 42), // HIS
                               new LabelSelection("B", "1", 87), // ASP
                               new LabelSelection("C", "1", 47))) // SER
                // parameters are considered mandatory arguments
                .buildParameters()
                // retrieve container with complete query
                .buildQuery()
                // execute query
                .run();
    }
}
```

### Update structure and inverted index data
Before searching, you need to add structure and inverted index data. Use the `MotifSearchUpdate` class to perform an 
update.

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

## Getting started by cloning
An alternative way to use the library is cloning this repository and building the corresponding Maven modules.

### Update structure and inverted index data
Like before, you will need run a local update to get results from a search. To do so, build the project and execute the 
packaged update jar:
```shell
java -jar -Xmx12G strucmotif-search-update/dist/strucmotif-update.jar ADD full
```

Specify the heap parameter `-Xmx` as roughly 75% of the available memory on your system for optimal performance.

### Controlling application properties during update
Application properties during the update process can be controlled by placing a file with the name 
`application.properties` in the directory from which the update will be executed. The file can be used to override 
default configurations.

```yaml
strucmotif.root-path=/Users/rcsb/strucmotif-data/
strucmotif.update-chunk-size=400
```

Use `root-path` to specify the directory to which structure and inverted index data will be written.

Set the `update-chunk-size` to a value that matches the `-Xmx` parameter. 400 works well with 12 GB of heap, 1,200 works
well with 24 GB. Decrease the chunk size if less memory is available, increase if more memory can be used. High values 
result in faster updates.

See the Configuration section for other parameters.

## Configuration
| Property     | Action | Default Value/Behavior |
| -----------  | ------ | ------- |
| `cif-fetch-url` | URL template for (Binary)CIF download | RCSB PDB BinaryCIF |
| `data-source` | Path to local CIF archive | cif-fetch-url |
| `decimal-places-score` | Number of decimal places reported for scores | `2` |
| `decimal-places-matrix` | Number of decimal places reported in transformation matrices | `3` |
| `distance-cutoff` | Maximum distance between alpha carbons that will be indexed in Å | `15` |
| `download-tries` | Number of tries to download structure data during update | `1` |
| `in-memory-strategy` | Either `OFF` or `HEAP` | `OFF` |
| `max-results` | Maximum number of results that will be returned | `10000` |
| `max-motif-size` | Maximum number of residues that may define a motif | `10` |
| `number-threads` | Number of worker threads | available processors |
| `renumbered-coordinate-precision` | Coordinate precision of BinaryCIF files | `1` |
| `renumbered-gzip` | Gzip BinaryCIF files? | `true` |
| `root-path` | Path where data files will be written | `/opt/data/` |
| `update-chunk-size` | Writing to the inverted index is slow and therefore done in chunks | `400` |

Configure by placing your `application.properties` on the classpath.

## Publication
Bittrich S, Burley SK, Rose AS (2020) Real-time structural motif searching in proteins using an inverted index strategy. PLoS Comput Biol 16(12): e1008502. https://doi.org/10.1371/journal.pcbi.1008502
