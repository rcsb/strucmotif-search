package org.rcsb.strucmotif.io;

import org.rcsb.strucmotif.domain.Pair;
import org.rcsb.strucmotif.domain.structure.StructureInformation;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AssemblyInformationProviderImpl implements AssemblyInformationProvider {
    // maps from struct_oper_id to all assemblies this transformation is part of
    private final Map<String, Map<String, Set<String>>> reverseAssemblyInformation;

    public AssemblyInformationProviderImpl(StateRepository stateRepository) {
        this.reverseAssemblyInformation = loadAssemblyInformation(stateRepository);
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
                    return new Pair<>(structureIdentifier, reversed);
                })
                .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
    }

    @Override
    public Map<String, Set<String>> selectAssemblyMap(String structureIdentifier) {
        return reverseAssemblyInformation.get(structureIdentifier);
    }
}
