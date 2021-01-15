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
capable of analyzing >170,000 PDB structures with unmatched speed. The efficiency of our 
inverted index method depends critically on identifying the small number of structures 
containing the query motif and ignoring most of the structures that are irrelevant. Our 
approach enables real-time retrieval and superposition of structural motifs, either extracted
from a reference structure or uploaded by the user.

## Performance
Current benchmark times to search in `160,467` structures as of `2/17/20`.

| Motif | Hits | Time | Units |
| --- | --- | --- | --- |
| Serine Protease (HDS) | 3,498 | 0.92 | s/op |
| Aminopeptidase (KDDDE) | 350 | 0.46 | s/op |
| Zinc Fingers (CCH) | 1,056 | 0.13 | s/op |
| Enolase Superfamily (KDEEH) | 288 | 0.36 | s/op |
| Enolase Superfamily (KDEEH, exchanges) | 308 | 0.87 | s/op |
| RNA G-Quadruplex (GGGG) | 84 | 1.10 | s/op | 

## Features
- nucleotide support
- inter-chain & assembly support
- position-specific exchanges
- modified residues

## Getting started
strucmotif-search is distributed by maven and supports Java 11+. To get started, append your `pom.xml` by:
```xml
<dependency>
  <groupId>org.rcsb</groupId>
  <artifactId>strucmotif-search</artifactId>
  <version>0.9.9</version>
</dependency>
```

## Example
```java
class Demo {
    public static void main(String[] args) {
        // the entry point for all things motif search - #newQuery() starts building a new query
        MotifSearch.newQuery()
                // several ways can be used to define the query motif - e.g., specify an entry id
                .defineByPdbIdAndSelection("4cha",
                        // and a collection of sequence positions to extract residues
                        Set.of(new LabelSelection("B", "1", 42), // HIS
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

## Configuration
| Property     | Action | Default Value/Behavior |
| -----------  | ------ | ------- |
| `cif-fetch-url` | URL template for (Binary)CIF download | RCSB PDB BinaryCIF |
| `data-source` | Path to local CIF archive | cif-fetch-url |
| `decimal-places-score` | Number of decimal places reported for scores | `2` |
| `decimal-places-matrix` | Number of decimal places reported in transformation matrices | `3` |
| `distance-cutoff` | Maximum distance between alpha carbons that will be indexed in Å | `15` |
| `max-results` | Maximum number of results that will be returned | `10000` |
| `max-motif-size` | Maximum number of residues that may define a motif | `10` |
| `number-threads` | Number of worker threads | available processors |
| `renumbered-coordinate-precision` | Coordinate precision of BinaryCIF files | `1` |
| `renumbered-gzip` | Gzip BinaryCIF files? | `true` |
| `root-path` | Path where data files will be written | `/opt/data/` |
| `update-chunk-size` | Writing to the inverted index is slow and therefore done in chunks | `400` |

Configure by placing your `application.properties` on the classpath.

## Availability
Structural motif searching is available as part of the RCSB advanced search: coming soon

## Publication
Bittrich S, Burley SK, Rose AS (2020) Real-time structural motif searching in proteins using an inverted index strategy. PLoS Comput Biol 16(12): e1008502. https://doi.org/10.1371/journal.pcbi.1008502
