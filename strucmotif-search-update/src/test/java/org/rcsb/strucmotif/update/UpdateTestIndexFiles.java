package org.rcsb.strucmotif.update;

import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.core.ThreadPool;
import org.rcsb.strucmotif.core.ThreadPoolImpl;
import org.rcsb.strucmotif.io.InvertedIndex;
import org.rcsb.strucmotif.io.InvertedIndexImpl;
import org.rcsb.strucmotif.io.ResidueTypeResolver;
import org.rcsb.strucmotif.io.ResidueTypeResolverImpl;
import org.rcsb.strucmotif.io.StateRepository;
import org.rcsb.strucmotif.io.StateRepositoryImpl;
import org.rcsb.strucmotif.io.StructureDataProvider;
import org.rcsb.strucmotif.io.StructureDataProviderImpl;
import org.rcsb.strucmotif.io.StructureIndexProvider;
import org.rcsb.strucmotif.io.StructureIndexProviderImpl;
import org.rcsb.strucmotif.io.StructureReader;
import org.rcsb.strucmotif.io.StructureReaderImpl;
import org.rcsb.strucmotif.io.StructureWriter;
import org.rcsb.strucmotif.io.StructureWriterImpl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Update the test resources in the core-module.
 */
public class UpdateTestIndexFiles {
    /**
     * Update index files used for testing.
     * @param args absolute path to test resource directory of core module
     * @throws Exception update failed
     */
    public static void main(String[] args) throws Exception {
        Path knownSource = Paths.get(args[0], "known.list");
        List<String> cmd = new BufferedReader(new InputStreamReader(Files.newInputStream(knownSource)))
                .lines()
                .map(l -> l.split(",")[0])
                .collect(Collectors.toList());
        cmd.add(0, "ADD");

        Path indexSource = Paths.get(args[0], "index");
        List<Path> indexFiles = Files.list(indexSource)
                .filter(p -> p.getFileName().toString().contains(".colf"))
                .collect(Collectors.toList());

        Path renumberedSource = Paths.get(args[0], "renum");

        Path updatePath = Files.createTempDirectory("strucmotif-test-resources");
        StrucmotifConfig strucmotifConfig = new StrucmotifConfig();
        strucmotifConfig.setRootPath(updatePath.toString());

        StateRepository stateRepository = new StateRepositoryImpl(strucmotifConfig);
        ResidueTypeResolver residueTypeResolver = new ResidueTypeResolverImpl(strucmotifConfig);
        StructureReader structureReader = new StructureReaderImpl(residueTypeResolver);
        StructureWriter structureWriter = new StructureWriterImpl(residueTypeResolver, strucmotifConfig);
        StructureDataProvider structureDataProvider = new StructureDataProviderImpl(structureReader, structureWriter, strucmotifConfig);
        InvertedIndex invertedIndex = new InvertedIndexImpl(strucmotifConfig);
        ThreadPool threadPool = new ThreadPoolImpl(strucmotifConfig);
        StructureIndexProvider structureIndexProvider = new StructureIndexProviderImpl(stateRepository);
        StrucmotifUpdate strucmotifUpdate = new StrucmotifUpdate(stateRepository, structureDataProvider, invertedIndex, strucmotifConfig, threadPool, structureIndexProvider);
        strucmotifUpdate.run(cmd.toArray(String[]::new));

        Files.move(updatePath.resolve("known.list"), knownSource, StandardCopyOption.REPLACE_EXISTING);
        for (Path p : indexFiles) {
            String bin = p.getFileName().toString().substring(0, 2);
            Files.move(updatePath.resolve("index").resolve(bin).resolve(p.getFileName()), p, StandardCopyOption.REPLACE_EXISTING);
        }
        for (Path p : Files.list(updatePath.resolve("renumbered")).collect(Collectors.toList())) {
            Files.move(p, renumberedSource.resolve(p.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
