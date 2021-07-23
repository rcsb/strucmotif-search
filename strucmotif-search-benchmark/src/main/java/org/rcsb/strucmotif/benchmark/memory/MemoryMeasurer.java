package org.rcsb.strucmotif.benchmark.memory;

import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.io.StructureReader;
import org.rcsb.strucmotif.io.StructureReaderImpl;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.URL;

public class MemoryMeasurer {
    public static void main(String[] args) throws IOException {
        StructureReader structureReader = new StructureReaderImpl();
        InputStream inputStream = new URL("https://models.rcsb.org/1acj.bcif").openStream();

        com.sun.management.ThreadMXBean b = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        long selfId = Thread.currentThread().getId();
        long memoryBefore = b.getThreadAllocatedBytes(selfId);

        Structure structure = structureReader.readFromInputStream(inputStream);
//         nop: 1124472
//         base: 7165768
//         sparse: 7152888
//         sparse + short coords: 7129672

        long memoryAfter = b.getThreadAllocatedBytes(selfId);
        System.out.println(structure.getStructureIdentifier());
        System.out.println(memoryAfter - memoryBefore);
    }
}
