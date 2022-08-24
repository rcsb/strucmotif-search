package org.rcsb.strucmotif.io;

/**
 * Allows writing data to FFindex-managed files without compromising production data. Call the providing function to
 * commit all data to the production files. This is rather slow and should only happen from time to time.
 */
public interface Committable {
    /**
     * Process all temporary files and make them available in queries.
     */
    void commit();
}
