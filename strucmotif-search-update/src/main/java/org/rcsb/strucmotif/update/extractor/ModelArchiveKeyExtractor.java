package org.rcsb.strucmotif.update.extractor;

/**
 * Handles files from the ModelArchive.
 */
public class ModelArchiveKeyExtractor implements KeyExtractor {
    private static final String NAMESPACE = "MA-";

    @Override
    public String getNameSpace() {
        return NAMESPACE;
    }

    @Override
    public String getKey(String resource) {
//        /path/to/ma-9z55z.cif -> MA-9Z55Z
        String name = resource.contains("/") ? resource.substring(resource.lastIndexOf("/") + 1) : resource;
        return name.replaceAll("(?i)\\.b?cif(?:\\.gz)?", "").toUpperCase();
    }
}
