package org.rcsb.strucmotif.update.extractor;

import java.util.List;

/**
 * Finds an appropriate key extractor.
 */
public class KeyExtractorFactory {
    private final List<KeyExtractor> nonGeneric;
    private final GenericKeyExtractor generic;
    private static final KeyExtractorFactory INSTANCE = new KeyExtractorFactory();

    private KeyExtractorFactory() {
        nonGeneric = List.of(new AlphaFoldKeyExtractor(), new ModelArchiveKeyExtractor());
        generic = new GenericKeyExtractor();
    }

    /**
     * Determine the key extractor for this resource.
     * @param resource the resource
     * @return a {@link KeyExtractor} that supports this namespace, the generic implementation if none matches
     */
    public static KeyExtractor getKeyExtractor(String resource) {
        String name = resource.contains("/") ? resource.substring(resource.lastIndexOf("/") + 1) : resource;
        String upperCase = name.toUpperCase();
        for (KeyExtractor keyExtractor : INSTANCE.nonGeneric) {
            if (upperCase.startsWith(keyExtractor.getNameSpace())) {
                return keyExtractor;
            }
        }
        return INSTANCE.generic;
    }

    /**
     * Short-cut to extract keys.
     * @param resource the resource
     * @return the extracted key
     */
    public static String getKey(String resource) {
        return getKeyExtractor(resource).getKey(resource);
    }
}
