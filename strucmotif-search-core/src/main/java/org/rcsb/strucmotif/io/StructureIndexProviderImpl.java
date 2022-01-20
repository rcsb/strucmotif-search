package org.rcsb.strucmotif.io;

import org.rcsb.strucmotif.domain.query.StructureDeterminationMethodology;
import org.rcsb.strucmotif.domain.structure.StructureInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
public class StructureIndexProviderImpl implements StructureIndexProvider {
    private static final Logger logger = LoggerFactory.getLogger(StructureIndexProviderImpl.class);
    private final Deque<Integer> reuse;
    private int next;
    private final Map<Integer, String> forward;
    private final Map<String, Integer> backward;
    private final Set<Integer> experimental;
    private final Set<Integer> computational;

    public StructureIndexProviderImpl(StateRepository stateRepository) {
        this.reuse = new ArrayDeque<>();
        this.forward = new HashMap<>();
        this.backward = new HashMap<>();
        this.experimental = new HashSet<>();
        this.computational = new HashSet<>();

        // determine the largest known id
        int max = -1; // let's start at 0, negative values are perfectly fine too (and will happen when counter overflows)
        for (StructureInformation structureInformation : stateRepository.selectKnown()) {
            int structureIndex = structureInformation.getStructureIndex();
            String structureIdentifier = structureInformation.getStructureIdentifier();
            forward.put(structureIndex, structureIdentifier);
            backward.put(structureIdentifier, structureIndex);

            // keep track whether indices are PDB or model
            if (StructureDeterminationMethodology.EXPERIMENTAL.test(structureIdentifier)) {
                experimental.add(structureIndex);
            } else {
                computational.add(structureIndex);
            }

            if (structureIndex > max) {
                max = structureIndex;
            }
        }

        // next is position after max
        this.next = max + 1;

        // determine all positions that can be safely reused
        for (int i = 0; i < next; i++) {
            if (forward.containsKey(i)) {
                continue;
            }

            reuse.add(i);
        }

        if (forward.size() != backward.size()) {
            throw new IllegalStateException("Mappings are not bidirectional: " + forward.size() + " identifiers, " + backward.size() + " indices");
        }
        if (containsKey(null)) {
            throw new IllegalStateException("Mappings contain 'null' key");
        }

        logger.info("{} mappings ({} PDB entries, {} computed structure models)",
                forward.size(),
                experimental.size(),
                computational.size());
        logger.info("{} keys will be reused, after that the next index will be {}",
                reuse.size(),
                next);
    }

    @Override
    public String selectStructureIdentifier(int structureIndex) {
        if (!containsKey(structureIndex)) {
            throw new IllegalStateException("No value for " + structureIndex + " - perform a 'RECOVER' update to remove lingering structures from index");
        }

        return forward.get(structureIndex);
    }

    @Override
    public int selectStructureIndex(String structureIdentifier) {
        return backward.get(structureIdentifier);
    }

    @Override
    public int selectOrMintStructureIndex(String structureIdentifier) {
        if (backward.containsKey(structureIdentifier)) {
            return backward.get(structureIdentifier);
        }

        return nextStructureIndex();
    }

    @Override
    public synchronized int nextStructureIndex() {
        // while there are indices to reuse then assign one of those first
        if (!reuse.isEmpty()) {
            return reuse.removeFirst();
        }

        return next++;
    }

    @Override
    public boolean containsKey(String structureIdentifier) {
        return backward.containsKey(structureIdentifier);
    }

    @Override
    public boolean containsKey(int structureIndex) {
        return forward.containsKey(structureIndex);
    }

    @Override
    public Set<Integer> selectBySearchSpace(StructureDeterminationMethodology structureDeterminationMethodology) {
        switch (structureDeterminationMethodology) {
            case EXPERIMENTAL:
                return experimental;
            case COMPUTATIONAL:
                return computational;
            case ALL:
                return forward.keySet();
            default:
                throw new UnsupportedOperationException(structureDeterminationMethodology + " isn't handled");
        }
    }
}
