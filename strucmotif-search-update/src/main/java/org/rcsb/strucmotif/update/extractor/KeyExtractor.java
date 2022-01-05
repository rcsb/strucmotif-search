package org.rcsb.strucmotif.update.extractor;

import java.nio.file.Path;

/**
 * Converts {@link Path} instances to a (hopefully) unique key.
 */
public interface KeyExtractor {
    /**
     * The expected namespace prefix used by this data-source.
     * @return a String which can be empty
     */
    String getNameSpace();

    /**
     * Extract the key of this path or URL. The input will be processed in a case-insensitive manner, the output will be
     * upper-case.
     * @param resource the path/URL to process
     * @return a key
     */
    String getKey(String resource);
}
