package org.rcsb.strucmotif.benchmark.memory;

import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.io.StructureReader;
import org.rcsb.strucmotif.io.StructureReaderImpl;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.URL;

public class Helper {
    public static void main(String[] args) throws IOException {
        StructureReader structureReader = new StructureReaderImpl();
        InputStream inputStream = new URL("https://models.rcsb.org/1acj.bcif").openStream();

        com.sun.management.ThreadMXBean b = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        long selfId = Thread.currentThread().getId();
        long memoryBefore = b.getThreadAllocatedBytes(selfId);

        Structure structure = structureReader.readFromInputStream(inputStream);
        // base:
        // sparse: 7152888

        long memoryAfter = b.getThreadAllocatedBytes(selfId);
        System.out.println(memoryAfter - memoryBefore);
    }
}
