Strucmotif-search Changelog
=============

Unreleased
-------------
### Breaking changes
- switch to Java 17

strucmotif-search 0.18.1
-------------
### Added
- `full` update is now based on RCSB PDB Search API and supports `full_csm`

### Bug fix
- better handling of memory-mapped regions -- don't crash JVM by exceeding `/proc/sys/vm/max_map_count`

### Breaking changes
- removed `rcsb-entry-holdings-url` property -- parsing of that is RCSB-specific anyway

strucmotif-search 0.17.0
-------------
### Added
- drop the "many small data files" approach and move to concatenated data file and index table for access

### Breaking changes
- switch format of data files to FFindex-based files
- remove config options that determine gzipping of structure & index files -- now structures are always gzipped and index files are always uncompressed
- replace `Collection` type in `StateRepository` with `Set` impl to avoid performance penalties when passing other collection impls
- `StructureDataProvider#deleteRenumbered(String)` to `StructureDataProvider#deleteRenumbered(Set)`
- `StructureWriter` doesn't write to file-system anymore, now returns a `byte[]`
- use `ByteBuffer` when performing bucket-specific operations

strucmotif-search 0.16.4
-------------
### Bug fix
- make sure that `InvertedIndex` impl honors `number-threads` parameter

strucmotif-search 0.16.3
-------------
### Bug fix
- rework Overlap detection for better performance

strucmotif-search 0.16.2
-------------
### Bug fix
- under some circumstances duplicated hits were reported

strucmotif-search 0.16.1
-------------
### Bug fix
- store `label_seq_id` as short[]

strucmotif-search 0.16.0
-------------
### Added
- support for modified residues
- support for pyrrolysine, selenocysteine, inosinic acid & 2'-deoxyuridine-5'-monophosphate
- support for D-amino acids
- faster updates by writing index updates as temporary files and merging only in configurable intervals

### Bug fix
- add 2'-Deoxythymidine-5'-monophosphate as `DT`
- fix behavior for ambiguous residue pair descriptors (like `DD-4-5-4`)

### Breaking changes
- don't index `T` anymore
- internal codes of residue types rearranged

strucmotif-search 0.15.2
-------------
### Added
- more parameter name tweaking

strucmotif-search 0.15.1
-------------
### Added
- add overloaded `contentTypes` method

strucmotif-search 0.15.0
-------------
### Breaking changes
- rename `StructureDeterminationMethodology` to `ContentType`, now use `builder.contentTypes(ContentType.EXPERIMENTAL, ContentType.COMPUTATIONAL)` to specify structures to consider

strucmotif-search 0.14.1
-------------
### Bug fix
- dependency updates

strucmotif-search 0.14.0
-------------
### Added
- add motif definitions, registry & search support (`MotifSearch...`)
- make default assembly identifier of residue graph configurable

### Breaking changes
- refactored for consistent naming:
  - top-level class: `MotifSearch` -> `Strucmotif`
  - entry point refactored from `MotifSearch#newQuery` -> `Strucmotif#searchForStructures`
  - corresponding query, parameter, result & hit objects are now more abstract to allow motif search support (`Strucmotif#detectMotifs`)
- `Motifs` replaced by more flexible `MotifDefinitionRegistry`
- query: `whitelist` & `blacklist` renamed to `allowedStructures` & `excludedStructures`

strucmotif-search 0.13.6
-------------
### Added
- add impl & extension to backend enum

strucmotif-search 0.13.5
-------------
### Bug fix
- fix build

strucmotif-search 0.13.4
-------------
### Bug fix
- properly detect files to load by extension

strucmotif-search 0.13.3
-------------
### Added
- some update classes are now public

strucmotif-search 0.13.2
-------------
### Added
- make `undefined-assembly-identifier` configurable

strucmotif-search 0.13.1
-------------
### Added
- make `undefined-assemblies` flag a parameter during query execution rather than tying it to the runtime (`undefined-assemblies` still must be set to true during indexing)

strucmotif-search 0.13.0
-------------
### Breaking changes
- change index keys from `String` to `int` - this allows more efficient storage of non-PDB identifiers

### Added
- support hits without assembly information (`strucmotif.undefined-assemblies` must be true)
- optionally, index only residues with 'good quality' (e.g., based on B-factor values)
- make `StructureWriterImpl` configurable
- support indexing of non-archived structures by reading from local file or URL (this allows to index e.g. AlphaFold data and search therein)
- filter by StructureDeterminationMethodology (`EXPERIMENTAL`, `COMPUTATIONAL`, or `ALL`) to restrict overall search space of queries
- add support for [colfer](https://github.com/pascaldekloe/colfer) backend to store inverted index data
- add support to index whole directories

strucmotif-search 0.12.9
-------------
### Bug fix
- Update log4j dependency

strucmotif-search 0.12.8
-------------
### Bug fix
- Update log4j dependency

strucmotif-search 0.12.7
-------------
### General
- Downgrade some dependencies to address staging errors

strucmotif-search 0.12.6
-------------
### General
- Adds some docs

strucmotif-search 0.12.5
-------------
### General
- Update other dependency

strucmotif-search 0.12.4
-------------
### Bug fix
- Update log4j dependency

strucmotif-search 0.12.3
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