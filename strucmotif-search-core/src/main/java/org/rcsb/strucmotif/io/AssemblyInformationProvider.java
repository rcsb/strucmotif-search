package org.rcsb.strucmotif.io;

import java.util.Map;
import java.util.Set;

public interface AssemblyInformationProvider {
    /**
     * Provides assembly information.
     * @param structureIdentifier the structure of interest
     * @return a map with operator expressions as key and a collection of all corresponding assembly ids as value
     */
    Map<String, Set<String>> selectAssemblyMap(String structureIdentifier);

    /**
     * If dealing with undefined assemblies: What's their identifier?
     * @return access to the `undefined-assembly-identifier` config property
     */
    String getUndefinedAssemblyIdentifier();
}
