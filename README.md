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
Structural motif searching is available as part of the [RCSB Advanced Search](https://www.rcsb.org/search/advanced/strucmotif) and [RCSB Mol* plugin](https://www.rcsb.org/3d-view). [Help documentation is available](https://www.rcsb.org/docs/search-and-browse/advanced-search/structure-motif-search).

## Performance
Current benchmark times to search in `180,419` structures as of `7/26/21`, obtained on an instance with 6 cores and 64 
GB memory. All structure data is held in memory, inverted index data is read from an SSD.

| Motif | Assemblies | 'Paths' Time [ms] | 'Score' Time [ms] |
| --- | --- | --- | --- |
| [Serine Protease (HDS)](https://www.rcsb.org/search?request=%7B%22query%22%3A%7B%22type%22%3A%22group%22%2C%22logical_operator%22%3A%22and%22%2C%22nodes%22%3A%5B%7B%22type%22%3A%22terminal%22%2C%22service%22%3A%22strucmotif%22%2C%22parameters%22%3A%7B%22value%22%3A%7B%22entry_id%22%3A%224CHA%22%2C%22residue_ids%22%3A%5B%7B%22label_asym_id%22%3A%22B%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A42%7D%2C%7B%22label_asym_id%22%3A%22B%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A87%7D%2C%7B%22label_asym_id%22%3A%22C%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A47%7D%5D%7D%2C%22rmsd_cutoff%22%3A2%2C%22atom_pairing_scheme%22%3A%22ALL%22%2C%22exchanges%22%3A%5B%7B%22residue_id%22%3A%7B%22label_asym_id%22%3A%22B%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A42%7D%2C%22allowed%22%3A%5B%22HIS%22%5D%7D%2C%7B%22residue_id%22%3A%7B%22label_asym_id%22%3A%22B%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A87%7D%2C%22allowed%22%3A%5B%22ASP%22%5D%7D%2C%7B%22residue_id%22%3A%7B%22label_asym_id%22%3A%22C%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A47%7D%2C%22allowed%22%3A%5B%22SER%22%5D%7D%5D%7D%7D%5D%7D%2C%22return_type%22%3A%22assembly%22%2C%22request_options%22%3A%7B%22pager%22%3A%7B%22start%22%3A0%2C%22rows%22%3A25%7D%2C%22scoring_strategy%22%3A%22combined%22%2C%22sort%22%3A%5B%7B%22sort_by%22%3A%22score%22%2C%22direction%22%3A%22desc%22%7D%5D%7D%2C%22request_info%22%3A%7B%22src%22%3A%22ui%22%2C%22query_id%22%3A%2294392fddc4cac2e83939ea7b4a842f52%22%7D%7D) | 3,689 | 712 | 51 |
| [Aminopeptidase (KDDDE)](https://www.rcsb.org/search?request=%7B%22query%22%3A%7B%22type%22%3A%22group%22%2C%22logical_operator%22%3A%22and%22%2C%22nodes%22%3A%5B%7B%22type%22%3A%22terminal%22%2C%22service%22%3A%22strucmotif%22%2C%22parameters%22%3A%7B%22value%22%3A%7B%22entry_id%22%3A%221LAP%22%2C%22residue_ids%22%3A%5B%7B%22label_asym_id%22%3A%22A%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A250%7D%2C%7B%22label_asym_id%22%3A%22A%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A255%7D%2C%7B%22label_asym_id%22%3A%22A%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A273%7D%2C%7B%22label_asym_id%22%3A%22A%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A332%7D%2C%7B%22label_asym_id%22%3A%22A%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A334%7D%5D%7D%2C%22rmsd_cutoff%22%3A2%2C%22atom_pairing_scheme%22%3A%22ALL%22%2C%22exchanges%22%3A%5B%7B%22residue_id%22%3A%7B%22label_asym_id%22%3A%22A%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A250%7D%2C%22allowed%22%3A%5B%22LYS%22%5D%7D%2C%7B%22residue_id%22%3A%7B%22label_asym_id%22%3A%22A%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A255%7D%2C%22allowed%22%3A%5B%22ASP%22%5D%7D%2C%7B%22residue_id%22%3A%7B%22label_asym_id%22%3A%22A%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A273%7D%2C%22allowed%22%3A%5B%22ASP%22%5D%7D%2C%7B%22residue_id%22%3A%7B%22label_asym_id%22%3A%22A%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A332%7D%2C%22allowed%22%3A%5B%22ASP%22%5D%7D%2C%7B%22residue_id%22%3A%7B%22label_asym_id%22%3A%22A%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A334%7D%2C%22allowed%22%3A%5B%22GLU%22%5D%7D%5D%7D%7D%5D%7D%2C%22return_type%22%3A%22assembly%22%2C%22request_options%22%3A%7B%22pager%22%3A%7B%22start%22%3A0%2C%22rows%22%3A25%7D%2C%22scoring_strategy%22%3A%22combined%22%2C%22sort%22%3A%5B%7B%22sort_by%22%3A%22score%22%2C%22direction%22%3A%22desc%22%7D%5D%7D%2C%22request_info%22%3A%7B%22src%22%3A%22ui%22%2C%22query_id%22%3A%22f9ac5032bafa82602773f5b9c7809852%22%7D%7D) | 81 | 550 | 2 |
| [Zinc Fingers (CCH)](https://www.rcsb.org/search?request=%7B%22query%22%3A%7B%22type%22%3A%22group%22%2C%22logical_operator%22%3A%22and%22%2C%22nodes%22%3A%5B%7B%22type%22%3A%22terminal%22%2C%22service%22%3A%22strucmotif%22%2C%22parameters%22%3A%7B%22value%22%3A%7B%22entry_id%22%3A%221G2F%22%2C%22residue_ids%22%3A%5B%7B%22label_asym_id%22%3A%22F%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A7%7D%2C%7B%22label_asym_id%22%3A%22F%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A25%7D%2C%7B%22label_asym_id%22%3A%22F%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A29%7D%5D%7D%2C%22rmsd_cutoff%22%3A2%2C%22atom_pairing_scheme%22%3A%22ALL%22%2C%22exchanges%22%3A%5B%7B%22residue_id%22%3A%7B%22label_asym_id%22%3A%22F%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A7%7D%2C%22allowed%22%3A%5B%22CYS%22%5D%7D%2C%7B%22residue_id%22%3A%7B%22label_asym_id%22%3A%22F%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A25%7D%2C%22allowed%22%3A%5B%22HIS%22%5D%7D%2C%7B%22residue_id%22%3A%7B%22label_asym_id%22%3A%22F%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A29%7D%2C%22allowed%22%3A%5B%22HIS%22%5D%7D%5D%7D%7D%5D%7D%2C%22return_type%22%3A%22assembly%22%2C%22request_options%22%3A%7B%22pager%22%3A%7B%22start%22%3A0%2C%22rows%22%3A25%7D%2C%22scoring_strategy%22%3A%22combined%22%2C%22sort%22%3A%5B%7B%22sort_by%22%3A%22score%22%2C%22direction%22%3A%22desc%22%7D%5D%7D%2C%22request_info%22%3A%7B%22src%22%3A%22ui%22%2C%22query_id%22%3A%22a58d3ce5f6337c8a6bf6045ec903aba8%22%7D%7D) | 419 | 89 | 4 |
| [Enolase Superfamily (KDEEH)](https://www.rcsb.org/search?request=%7B%22query%22%3A%7B%22type%22%3A%22group%22%2C%22logical_operator%22%3A%22and%22%2C%22nodes%22%3A%5B%7B%22type%22%3A%22terminal%22%2C%22service%22%3A%22strucmotif%22%2C%22rmsd_cutoff%22%3A%222%22%2C%22atom_pairing_scheme%22%3A%22ALL%22%2C%22is_empty%22%3Afalse%2C%22parameters%22%3A%7B%22value%22%3A%7B%22entry_id%22%3A%222MNR%22%2C%22residue_ids%22%3A%5B%7B%22label_asym_id%22%3A%22A%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A162%7D%2C%7B%22label_asym_id%22%3A%22A%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A193%7D%2C%7B%22label_asym_id%22%3A%22A%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A219%7D%2C%7B%22label_asym_id%22%3A%22A%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A245%7D%2C%7B%22label_asym_id%22%3A%22A%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A295%7D%5D%7D%2C%22rmsd_cutoff%22%3A2%2C%22atom_pairing_scheme%22%3A%22ALL%22%2C%22exchanges%22%3A%5B%7B%22residue_id%22%3A%7B%22label_asym_id%22%3A%22A%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A162%7D%2C%22allowed%22%3A%5B%22LYS%22%5D%7D%2C%7B%22residue_id%22%3A%7B%22label_asym_id%22%3A%22A%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A193%7D%2C%22allowed%22%3A%5B%22ASP%22%5D%7D%2C%7B%22residue_id%22%3A%7B%22label_asym_id%22%3A%22A%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A219%7D%2C%22allowed%22%3A%5B%22GLU%22%5D%7D%2C%7B%22residue_id%22%3A%7B%22label_asym_id%22%3A%22A%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A245%7D%2C%22allowed%22%3A%5B%22GLU%22%5D%7D%2C%7B%22residue_id%22%3A%7B%22label_asym_id%22%3A%22A%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A295%7D%2C%22allowed%22%3A%5B%22HIS%22%5D%7D%5D%7D%2C%22label%22%3A%22strucmotif%22%7D%5D%7D%2C%22return_type%22%3A%22assembly%22%2C%22request_options%22%3A%7B%22pager%22%3A%7B%22start%22%3A0%2C%22rows%22%3A25%7D%2C%22scoring_strategy%22%3A%22combined%22%2C%22sort%22%3A%5B%7B%22sort_by%22%3A%22score%22%2C%22direction%22%3A%22desc%22%7D%5D%7D%2C%22request_info%22%3A%7B%22query_id%22%3A%22540fe9f7b4bb253ad79e0270c7f6bc50%22%7D%7D) | 173 | 421 | 2 |
| [Enolase Superfamily (KDEEH, exchanges)](https://www.rcsb.org/search?request=%7B%22query%22%3A%7B%22type%22%3A%22group%22%2C%22logical_operator%22%3A%22and%22%2C%22nodes%22%3A%5B%7B%22type%22%3A%22terminal%22%2C%22service%22%3A%22strucmotif%22%2C%22rmsd_cutoff%22%3A%222%22%2C%22atom_pairing_scheme%22%3A%22ALL%22%2C%22is_empty%22%3Afalse%2C%22parameters%22%3A%7B%22value%22%3A%7B%22entry_id%22%3A%222MNR%22%2C%22residue_ids%22%3A%5B%7B%22label_asym_id%22%3A%22A%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A162%7D%2C%7B%22label_asym_id%22%3A%22A%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A193%7D%2C%7B%22label_asym_id%22%3A%22A%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A219%7D%2C%7B%22label_asym_id%22%3A%22A%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A245%7D%2C%7B%22label_asym_id%22%3A%22A%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A295%7D%5D%7D%2C%22rmsd_cutoff%22%3A2%2C%22atom_pairing_scheme%22%3A%22ALL%22%2C%22exchanges%22%3A%5B%7B%22residue_id%22%3A%7B%22label_asym_id%22%3A%22A%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A162%7D%2C%22allowed%22%3A%5B%22LYS%22%2C%22HIS%22%5D%7D%2C%7B%22residue_id%22%3A%7B%22label_asym_id%22%3A%22A%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A193%7D%2C%22allowed%22%3A%5B%22ASP%22%5D%7D%2C%7B%22residue_id%22%3A%7B%22label_asym_id%22%3A%22A%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A219%7D%2C%22allowed%22%3A%5B%22GLU%22%5D%7D%2C%7B%22residue_id%22%3A%7B%22label_asym_id%22%3A%22A%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A245%7D%2C%22allowed%22%3A%5B%22GLU%22%2C%22ASP%22%2C%22ASN%22%5D%7D%2C%7B%22residue_id%22%3A%7B%22label_asym_id%22%3A%22A%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A295%7D%2C%22allowed%22%3A%5B%22HIS%22%2C%22LYS%22%5D%7D%5D%7D%2C%22label%22%3A%22strucmotif%22%7D%5D%7D%2C%22return_type%22%3A%22assembly%22%2C%22request_options%22%3A%7B%22pager%22%3A%7B%22start%22%3A0%2C%22rows%22%3A25%7D%2C%22scoring_strategy%22%3A%22combined%22%2C%22sort%22%3A%5B%7B%22sort_by%22%3A%22score%22%2C%22direction%22%3A%22desc%22%7D%5D%7D%2C%22request_info%22%3A%7B%22query_id%22%3A%22e2aaca70b01536d1e8eb6af1618494e2%22%7D%7D) | 179 | 1,238 | 5 |
| [RNA G-Quadruplex (GGGG)](https://www.rcsb.org/search?request=%7B%22query%22%3A%7B%22type%22%3A%22group%22%2C%22logical_operator%22%3A%22and%22%2C%22nodes%22%3A%5B%7B%22type%22%3A%22terminal%22%2C%22service%22%3A%22strucmotif%22%2C%22parameters%22%3A%7B%22value%22%3A%7B%22entry_id%22%3A%223IBK%22%2C%22residue_ids%22%3A%5B%7B%22label_asym_id%22%3A%22A%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A4%7D%2C%7B%22label_asym_id%22%3A%22A%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A10%7D%2C%7B%22label_asym_id%22%3A%22B%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A4%7D%2C%7B%22label_asym_id%22%3A%22B%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A10%7D%5D%7D%2C%22rmsd_cutoff%22%3A2%2C%22atom_pairing_scheme%22%3A%22ALL%22%2C%22exchanges%22%3A%5B%7B%22residue_id%22%3A%7B%22label_asym_id%22%3A%22A%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A4%7D%2C%22allowed%22%3A%5B%22G%22%5D%7D%2C%7B%22residue_id%22%3A%7B%22label_asym_id%22%3A%22A%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A10%7D%2C%22allowed%22%3A%5B%22G%22%5D%7D%2C%7B%22residue_id%22%3A%7B%22label_asym_id%22%3A%22B%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A4%7D%2C%22allowed%22%3A%5B%22G%22%5D%7D%2C%7B%22residue_id%22%3A%7B%22label_asym_id%22%3A%22B%22%2C%22struct_oper_id%22%3A%221%22%2C%22label_seq_id%22%3A10%7D%2C%22allowed%22%3A%5B%22G%22%5D%7D%5D%7D%7D%5D%7D%2C%22return_type%22%3A%22assembly%22%2C%22request_options%22%3A%7B%22pager%22%3A%7B%22start%22%3A0%2C%22rows%22%3A25%7D%2C%22scoring_strategy%22%3A%22combined%22%2C%22sort%22%3A%5B%7B%22sort_by%22%3A%22score%22%2C%22direction%22%3A%22desc%22%7D%5D%7D%2C%22request_info%22%3A%7B%22src%22%3A%22ui%22%2C%22query_id%22%3A%22491241b8c37e9f6f50b3140cd555c1bf%22%7D%7D) | 34 | 1,465 | 96 | 

Search for all assemblies that contain hits with an RMSD <2 Å.
'Paths' refers to the time spent on inverted index operations, which identify all candidate structures that contain the 
motif.
'Score' refers to the time spent on aligning candidate structures to the query and computing RMSD values.

## Features
- nucleotide support
- inter-chain & assembly support
- position-specific exchanges
- modified residues
- support for computed structure models, like from AlphaFold

## Getting started with a dependency
strucmotif-search is distributed by maven and supports Java 11+. To get started, append your `pom.xml` by:
```xml
<dependency>
  <groupId>org.rcsb</groupId>
  <artifactId>strucmotif-search</artifactId>
  <version>0.12.0</version>
</dependency>
```

## Getting started by cloning
An alternative way to use the library is cloning this repository and building the corresponding Maven modules.

## Search for structural motifs
The `MotifSearch` class provides a fluent API to process structural motif queries.

```java
class Demo {
    public static void main(String[] args) {
        // the entry point for all things motif search - #newQuery() starts building a new query
        MotifSearch.newQuery()
                // several ways can be used to define the query motif - e.g., specify an entry id
                .defineByPdbIdAndSelection("4CHA",
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

## Configuration
| Property     | Action | Default Value/Behavior |
| -----------  | ------ | ------- |
| `decimal-places-score` | Number of decimal places reported for scores | `2` |
| `decimal-places-matrix` | Number of decimal places reported in transformation matrices | `3` |
| `in-memory-strategy` | Preload structure data for increased performance? | `OFF` |
| `max-results` | Maximum number of results that will be returned | `10000` |
| `max-motif-size` | Maximum number of residues that may define a motif | `10` |
| `number-threads` | Number of worker threads | available processors |
| `root-path` | Path where data files are read from | `/opt/data/` |
| `undefined-assemblies` | Return hits without assembly information? | `false` |

Configure by placing your `application.properties` on the classpath.

## Index Structure Data and Run Updates
You will need to process your corpus of structure data before using the service. This will create an optized version of
all structure files and add them to an inverted index that allows efficient searching.

Details can be found in:
https://github.com/rcsb/strucmotif-search/blob/master/strucmotif-search-update/UPDATE.md

## Publication
Bittrich S, Burley SK, Rose AS (2020) Real-time structural motif searching in proteins using an inverted index strategy. PLoS Comput Biol 16(12): e1008502. https://doi.org/10.1371/journal.pcbi.1008502
