package org.rcsb.strucmotif.update.extractor;

/**
 * Handles files from the AlphaFoldDB.
 */
public class AlphaFoldKeyExtractor implements KeyExtractor {
    private static final String NAMESPACE = "AF-";

    @Override
    public String getNameSpace() {
        return NAMESPACE;
    }

    @Override
    public String getKey(String resource) {
        // https://alphafold.ebi.ac.uk/files/AF-Q76EI6-F1-model_v1.cif -> AF-Q76EI6-F1
        String name = resource.contains("/") ? resource.substring(resource.lastIndexOf("/")) : resource;
        return name.replaceAll("(?i)-model_v\\d+\\.b?cif(?:\\.gz)?", "").toUpperCase();
    }
}
