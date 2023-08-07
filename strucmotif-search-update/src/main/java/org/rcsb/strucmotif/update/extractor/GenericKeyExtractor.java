package org.rcsb.strucmotif.update.extractor;

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
        return name.replaceAll("(?i)\\.b?cif(?:\\.gz)?", "").toUpperCase();
    }
}
