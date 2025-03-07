package org.rcsb.strucmotif.update;

import org.rcsb.strucmotif.io.StructureDataProvider;

import java.net.URL;

/**
 * One piece of the update. Usually just references a PDB entry.<p>
 * Can also point to external resources or local files via a URL. In that case, you must make sure to provide a unique
 * structureIdentifier.
 */
public class UpdateItem {
    private final String structureIdentifier;
    private final URL url;
    private final int modelIdentifier;

    /**
     * Update by PDB-ID.
     * @param structureIdentifier a PDB entry ID
     */
    public UpdateItem(String structureIdentifier) {
        this(structureIdentifier, null, StructureDataProvider.DEFAULT_MODEL_IDENTIFIER);
    }

    /**
     * Update a specific model of a PDB-ID.
     * @param structureIdentifier a PDB entry ID
     */
    public UpdateItem(String structureIdentifier, int modelIdentifier) {
        this(structureIdentifier, null, modelIdentifier);
    }

    /**
     * Update by identifier and external URL.
     * @param structureIdentifier the unique ID this item will have
     * @param url data source pointing to non-PDB structures, either by URL to some external resource or to a local file
     */
    public UpdateItem(String structureIdentifier, URL url) {
        this(structureIdentifier, url, StructureDataProvider.DEFAULT_MODEL_IDENTIFIER);
    }

    /**
     * Update by identifier and external URL, while being model-aware.
     * @param structureIdentifier the unique ID this item will have
     * @param url data source pointing to non-PDB structures, either by URL to some external resource or to a local file
     * @param modelIdentifier int value of the model to index
     */
    public UpdateItem(String structureIdentifier, URL url, int modelIdentifier) {
        this.structureIdentifier = structureIdentifier;
        this.url = url;
        this.modelIdentifier = modelIdentifier;
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

    /**
     * Which model to index?
     * @return an int (usually, 1)
     */
    public int getModelIdentifier() {
        return modelIdentifier;
    }
}
