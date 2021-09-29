Strucmotif-search Changelog
=============

Unreleased
-------------
### Bug fix
- compute correct transformation & RMSD for transformed residues

strucmotif-search 0.12.2
-------------
### Bug fix
- throw `IllegalQueryDefinitionException` if requested chain/residue isn't found, part 2

strucmotif-search 0.12.1
-------------
### Bug fix
- throw `IllegalQueryDefinitionException` if requested chain/residue isn't found

strucmotif-search 0.12.0
-------------
### Breaking changes
- all of them - inverted index and structure files are incompatible to previous versions
- reintroduce IndexSelection and build index upon on them
- remove geometric scores - compute RMSD for all hits - remove associated parameters
- replace Set/Collection with List in places where order (e.g. of residue) matters
- a whole lotta internal changes

strucmotif-search 0.11.2
-------------
### Bug fix
- fix multiplication of matrices

strucmotif-search 0.11.1
-------------
### Added
- dependency updates

strucmotif-search 0.11.0
-------------
### Added
- write results to file & custom consumers

strucmotif-search 0.10.1
-------------
### Bug fix
- correct order of residues in transformed hits

strucmotif-search 0.10.0
-------------
### Added
- download-tries config parameter for update routine

strucmotif-search 0.9.15
-------------
### Bug fix
- fix timing issues for mkdir in StructureDataProviderImpl & FileSystemInvertedIndex
- properly handle blank lines in state.list & known.list

strucmotif-search 0.9.14
-------------
### Bug fix
- avoid IOException when missing permissions for default path (FileSystemInvertedIndex)

strucmotif-search 0.9.13
-------------
### Bug fix
- avoid IOException when missing permissions for default path (StructureDataProviderImpl)

strucmotif-search 0.9.12
-------------
### Bug fix
- throw IllegalQueryDefinitionException when detected during result stage

strucmotif-search 0.9.11
-------------
### General
- order result residues wrt query

strucmotif-search 0.9.10
-------------
### General
- support queries from transformed coordinates

strucmotif-search 0.9.9
-------------
### General
- geom scores normalized [0,1] - high scores are better

strucmotif-search 0.9.8
-------------
### General
- dedicated IllegalQueryDefinitionException when no residue pairs can be detected
- no longer throws exception for empty result sets (rather return empty collection)

strucmotif-search 0.9.7
-------------
### General
- filtering by score: now less or equal instead of strictly less

strucmotif-search 0.9.6
-------------
### General
- better logging (use unique identifier for log statements)
- strict interpretation of cutoff/limit values (0 was de facto ignored)

strucmotif-search 0.9.4
-------------
### Bug fixes
- don't report hits without assembly information

strucmotif-search 0.9.3
-------------
### Bug fixes
- StructureIdentifiers are case-insensitive (wrt equals/hashCode)

strucmotif-search 0.9.2
-------------
### General
- report assembly information of hit

### Bug fixes
- remove dependency on legacy endpoint

strucmotif-search 0.9.1
-------------
### General
- will now perform automatic recover operation during update if necessary
- marking entries only as dirty when index is getting updated

strucmotif-search 0.9.0
-------------
### Bug fixes
- fix tests
- rename bcif-fetch-url to cif-fetch-url

strucmotif-search 0.8.4
-------------
### Bug fixes
- fix state parsing

strucmotif-search 0.8.3
-------------
### Bug fixes
- allow missing dirty.list if empty

strucmotif-search 0.8.2
-------------
### General
- number-threads now affects update routine

strucmotif-search 0.8.1
-------------
### General
- let's consider this the first public release

strucmotif-search 0.0.1
-------------
### General
- this is the implementation described in the paper