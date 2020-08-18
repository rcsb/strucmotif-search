[![Changelog](https://img.shields.io/badge/changelog--lightgrey.svg?style=flat)](https://github.com/rcsb/strucmotif-search/blob/master/CHANGELOG.md)

# Structural Motif Search
Structural motifs are small sets of amino acids in spatial proximity that constitute e.g. active sites or 
structure-defining patterns. This implementation traverses the whole PDB archive (>160k structures) and aims at 
returning all highly similar occurrences of the query motif within a second.

## Getting started
strucmotif-search is distributed by maven and supports Java 11+. To get started, append your `pom.xml` by:
```xml
<dependency>
  <groupId>org.rcsb</groupId>
  <artifactId>strucmotif-search</artifactId>
  <version>...</version>
</dependency>
```

Be sure to have a MongoDB server running, e.g.:
```
mongod --dbpath=/opt/data/mongodb
```

## Example
```java
class Demo {
    public static void main(String[] args) {
        MotifSearchResult simple = MotifSearch.newQuery()
            .defineByPdbIdAndSelection("4cha",
                Set.of(new LabelSelection("B", 1, 42), // HIS
                       new LabelSelection("B", 1, 87), // ASP
                       new LabelSelection("C", 1, 47))) // SER
            // parameters are considered mandatory arguments - we use defaults
            .buildParameters()
            // several additional arguments can be provided to customize the query further
            .buildQuery()
            // execute query
            .run()
            .getHits()
            // a collection of hits is returned
            .forEach(System.out::println);
    }
}
```

## Performance
Current benchmark times to search in `160,467` structure as of `2/17/20`.

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
- can be run locally on an ordinary desktop system

## Limitations
- default maximum distance of 2 backbone atoms is 20 A
- default tolerance value might not find all relevant matches
- no support for alpha carbon traces
- data should be stored on SSD (~6 GB for archive, ~65 GB for lookup table, ~40 GB for residue-DB)

## Implementation
An inverted indexing strategy is employed to find all similar motif occurrences in a search space of >160k structures.
All occurrences of amino acids & nucleotides pairs are described by a `ResiduePairDescriptor` that captures:
- label of residue 1: e.g. histidine => `H`
- label of residue 2: e.g. aspartic acid => `D`
- distance between backbones (`CA` for amino acids): e.g. 6.456 Å => `6`
- distance between side-chains (`CB` for amino acids): e.g. 8.693 Å => `9`
- angle between vectors defined by backbone and side-chain coordinates: e.g. 90.5˚ => `5` 

This allows composing compact yet informative descriptors such as `HD-6-9-5` for all residue pairs in the search space.
Sequence positions where certain residue pairs occur can be addressed by a `ResiduePairIdentifier` that summarizes:
- pdbId
- id of assembly generation operation
- combination of sequence indices which correspond to this particular `ResiduePairDescriptor`

A search can be performed by fragmenting it into `ResiduePairDescriptor` instances, looking up all 
occurrences of these `ResiduePairDescriptor`, and finding combinations which are compatible to the query. Subsequently, 
this allows aligning the query and each candidate motif and score by RMSD.