package org.rcsb.strucmotif.io;

import org.rcsb.strucmotif.domain.structure.StructureInformation;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
public class StructureIndexProviderImpl implements StructureIndexProvider {
    private final Deque<Integer> reuse;
    private int next;
    private final Map<Integer, String> forward;

    public StructureIndexProviderImpl(StateRepository stateRepository) {
        this.reuse = new ArrayDeque<>();
        this.forward = new HashMap<>();

        // determine the largest known id
        int max = Integer.MIN_VALUE;
        for (StructureInformation structureInformation : stateRepository.selectKnown()) {
            int index = structureInformation.getStructureIndex();
            forward.put(index, structureInformation.getStructureIdentifier());
            if (index > max) {
                max = index;
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
    }

    @Override
    public String selectStructureIdentifier(int structureIndex) {
        return forward.get(structureIndex);
    }

    /**
     * Maps between structure identifier and index. Note that this is here for convenience and not necessarily fast.
     * @param structureIdentifier the String
     * @return the int
     */
    public int selectStructureIndex(String structureIdentifier) {
        for (Map.Entry<Integer, String> entry : forward.entrySet()) {
            if (Objects.equals(structureIdentifier, entry.getValue())) {
                return entry.getKey();
            }
        }
        throw new NoSuchElementException("Didn't find index for identifier '" + structureIdentifier + "'");
    }

    @Override
    public synchronized int nextStructureIndex() {
        // while there are indices to reuse then assign one of those first
        if (!reuse.isEmpty()) {
            return reuse.removeFirst();
        }

        return next++;
    }
}
