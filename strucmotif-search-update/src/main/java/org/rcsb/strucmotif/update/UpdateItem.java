package org.rcsb.strucmotif.update;

import java.net.URL;

/**
 * One piece of the update. Usually just references a PDB entry.<p>
 * Can also point to external resources or local files via a URL. In that case, you must make sure to provide a unique,
 * yet compact structureIdentifier.
 */
public class UpdateItem {
    private final String structureIdentifier;
    private final URL url;

    /**
     * Update by PDB-ID.
     * @param structureIdentifier a PDB entry ID
     */
    public UpdateItem(String structureIdentifier) {
        this(structureIdentifier, null);
    }

    /**
     * Update by identifier and external URL.
     * @param structureIdentifier the unique ID this item will have
     * @param url data source pointing to non-PDB structures, either by URL to some external resource or to a local file
     */
    public UpdateItem(String structureIdentifier, URL url) {
        this.structureIdentifier = structureIdentifier;
        this.url = url;
    }

    /**
     * Get the key with which this item will be registered.
     * @return a String, usually a PDB-ID or some compact identifier
     */
    public String getStructureIdentifier() {
        return structureIdentifier;
    }

    /**
     * Get the resource URL to load.
     * @return the URL to load, or null if referencing a PDB entry
     */
    public URL getUrl() {
        return url;
    }
}
