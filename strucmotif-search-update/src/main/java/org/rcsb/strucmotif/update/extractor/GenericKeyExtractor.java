package org.rcsb.strucmotif.update.extractor;

import org.rcsb.strucmotif.domain.structure.EntryIds;

import java.util.Locale;

/**
 * Fallback strategy of extracting keys: use file name.
 */
public class GenericKeyExtractor implements KeyExtractor {
    private static final String NAMESPACE = "";

    /**
     * Default constructor.
     */
    public GenericKeyExtractor() {
    }

    @Override
    public String getNameSpace() {
        return NAMESPACE;
    }

    @Override
    public String getKey(String resource) {
        // /path/to/1abc.cif -> 1ABC
        String name = resource.contains("/") ? resource.substring(resource.lastIndexOf("/") + 1) : resource;
        String key = name.replaceAll("(?i)\\.b?cif(?:\\.gz)?", "");
        // legacy and CSM keys are conventionally upper-case, but extended PDB IDs must stay lower-case
        String lowerCase = key.toLowerCase(Locale.ROOT);
        return EntryIds.isExtendedPdbId(lowerCase) ? lowerCase : key.toUpperCase();
    }
}
