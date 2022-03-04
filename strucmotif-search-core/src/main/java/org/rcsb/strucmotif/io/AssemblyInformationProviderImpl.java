package org.rcsb.strucmotif.io;

import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.domain.Pair;
import org.rcsb.strucmotif.domain.structure.StructureInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Wraps the {@link StateRepository} to provide assembly information for individual structures.
 */
@Service
public class AssemblyInformationProviderImpl implements AssemblyInformationProvider {
    private static final Map<String, Set<String>> UNDEFINED_ASSEMBLIES = Collections.emptyMap();
    // maps from struct_oper_id to all assemblies this transformation is part of
    private final Map<String, Map<String, Set<String>>> reverseAssemblyInformation;
    private final StrucmotifConfig strucmotifConfig;

    /**
     * Construct a assembly info provider.
     * @param stateRepository the state provider
     * @param strucmotifConfig the config
     */
    @Autowired
    public AssemblyInformationProviderImpl(StateRepository stateRepository, StrucmotifConfig strucmotifConfig) {
        this.reverseAssemblyInformation = loadAssemblyInformation(stateRepository);
        this.strucmotifConfig = strucmotifConfig;
    }

    private Map<String, Map<String, Set<String>>> loadAssemblyInformation(StateRepository stateRepository) {
        Collection<StructureInformation> data = stateRepository.selectKnown();
        return data.stream()
                .map(s -> {
                    String structureIdentifier = s.getStructureIdentifier();
                    Map<String, String[]> assemblyInformation = s.getAssemblyInformation();
                    Map<String, Set<String>> reversed = new HashMap<>();
                    for (Map.Entry<String, String[]> partial : assemblyInformation.entrySet()) {
                        String assemblyId = partial.getKey();
                        for (String structOperId : partial.getValue()) {
                            Set<String> mappedAssemblyIds = reversed.computeIfAbsent(structOperId, e -> new HashSet<>());
                            mappedAssemblyIds.add(assemblyId);
                        }
                    }

                    // compact representation by not storing empty maps explicitly
                    if (reversed.isEmpty()) {
                        return null;
                    }
                    return new Pair<>(structureIdentifier, reversed);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
    }

    @Override
    public Map<String, Set<String>> selectAssemblyMap(String structureIdentifier) {
        return reverseAssemblyInformation.getOrDefault(structureIdentifier, UNDEFINED_ASSEMBLIES);
    }

    @Override
    public String getUndefinedAssemblyIdentifier() {
        return strucmotifConfig.getUndefinedAssemblyIdentifier();
    }
}
