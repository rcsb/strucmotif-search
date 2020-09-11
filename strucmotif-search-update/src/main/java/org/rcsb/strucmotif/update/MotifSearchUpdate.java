package org.rcsb.strucmotif.update;

import com.google.gson.Gson;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.persistence.StateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

@SpringBootApplication(exclude = { MongoAutoConfiguration.class, MongoDataAutoConfiguration.class })
@ComponentScan({"org.rcsb.strucmotif"})
@EntityScan("org.rcsb.strucmotif")
public class MotifSearchUpdate implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(MotifSearchUpdate.class);
    private static final String RCSB_ENTRY_LIST = "http://www.rcsb.org/pdb/json/getCurrent";

    public static void main(String[] args) {
        SpringApplication.run(MotifSearchUpdate.class, args);
    }

    public void run(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Too few arguments");
            System.out.println("Usage: java -Xmx12G -jar update.jar context operation ...");
            System.out.println("Valid context values: " + Arrays.toString(Context.values()));
            System.out.println("Valid operation values: " + Arrays.toString(Operation.values()));
            System.out.println("Optionally: list of entry ids - (no argument performs null operation, use single argument 'full' for complete update)");
            System.out.println("If you want to update entries you have to explicitly remove them first");
            System.out.println("Example: java -Xmx12G -jar update.jar BCIF ADD 1acj 1exr 4hhb");
            return;
        }

        Context context = Context.resolve(args[0]);
        Operation operation = Operation.resolve(args[1]);
        String[] ids = new String[args.length - 2];
        Collection<StructureIdentifier> identifiers;
        System.arraycopy(args, 2, ids, 0, ids.length);
        if (ids.length == 1 && ids[0].equalsIgnoreCase("full")) {
            identifiers = getAllIdentifiers();
        } else {
            identifiers = Arrays.stream(ids).map(StructureIdentifier::new).collect(Collectors.toSet());
        }

        logger.info("Starting update - Context: {}, Operation: {}, {} ids ({})",
                context,
                operation,
                identifiers.size(),
                identifiers.stream()
                        .limit(5)
                        .map(id -> "\"" + id.getPdbId() + "\"")
                        .collect(Collectors.joining(", ", "[", "]")));

        switch (operation) {
            case ADD:
                handleAddOperation(context, identifiers);
                break;
            case REMOVE:
                handleDeleteOperation(context, identifiers);
                break;
        }
    }

    public Collection<StructureIdentifier> getAllIdentifiers() throws IOException {
        logger.info("Retrieving current entry list from {}", RCSB_ENTRY_LIST);
        GetCurrentResponse response;
        try (InputStream inputStream = new URL(RCSB_ENTRY_LIST).openStream()) {
            try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream)) {
                response = new Gson().fromJson(inputStreamReader, GetCurrentResponse.class);
            }
        }
        return Arrays.stream(response.getIdList())
                .map(String::toLowerCase)
                .map(StructureIdentifier::new)
                .collect(Collectors.toSet());
    }

    @SuppressWarnings("unused")
    static class GetCurrentResponse {
        private int resultCount;
        private String[] idList;

        int getResultCount() {
            return resultCount;
        }

        void setResultCount(int resultCount) {
            this.resultCount = resultCount;
        }

        String[] getIdList() {
            return idList;
        }

        void setIdList(String[] idList) {
            this.idList = idList;
        }
    }

    private final StateRepository stateRepository;
    private final AddStructuresToArchiveTask addStructuresToArchiveTask;
    private final AddStructuresToInvertedIndexTask addStructuresToInvertedIndexTask;
    private final AddStructuresToStructureRepositoryTask addStructuresToStructureRepositoryTask;
    private final DeleteStructuresFromArchiveTask deleteStructuresFromArchiveTask;
    private final DeleteStructuresFromInvertedIndexTask deleteStructuresFromInvertedIndexTask;
    private final DeleteStructuresFromStructureRepositoryTask deleteStructuresFromStructureRepositoryTask;

    @Autowired
    public MotifSearchUpdate(StateRepository stateRepository, AddStructuresToArchiveTask addStructuresToArchiveTask, AddStructuresToInvertedIndexTask addStructuresToInvertedIndexTask, AddStructuresToStructureRepositoryTask addStructuresToStructureRepositoryTask, DeleteStructuresFromArchiveTask deleteStructuresFromArchiveTask, DeleteStructuresFromInvertedIndexTask deleteStructuresFromInvertedIndexTask, DeleteStructuresFromStructureRepositoryTask deleteStructuresFromStructureRepositoryTask) {
        this.stateRepository = stateRepository;
        this.addStructuresToArchiveTask = addStructuresToArchiveTask;
        this.addStructuresToInvertedIndexTask = addStructuresToInvertedIndexTask;
        this.addStructuresToStructureRepositoryTask = addStructuresToStructureRepositoryTask;
        this.deleteStructuresFromArchiveTask = deleteStructuresFromArchiveTask;
        this.deleteStructuresFromInvertedIndexTask = deleteStructuresFromInvertedIndexTask;
        this.deleteStructuresFromStructureRepositoryTask = deleteStructuresFromStructureRepositoryTask;
    }

    public void handleAddOperation(Context context, Collection<StructureIdentifier> requested) {
        Collection<StructureIdentifier> known = getKnownIdentifiers(context);
        Collection<StructureIdentifier> delta = getDeltaPlusIdentifiers(requested, known);
        switch (context) {
            case BCIF:
                addStructuresToArchiveTask.execute(delta);
                break;
            case STRUCTURES:
                addStructuresToStructureRepositoryTask.execute(delta);
                break;
            case INDEX:
                addStructuresToInvertedIndexTask.execute(delta);
                break;
            case ALL:
                addStructuresToArchiveTask.execute(delta);
                addStructuresToStructureRepositoryTask.execute(delta);
                addStructuresToInvertedIndexTask.execute(delta);
                break;
        }
    }

    public void handleDeleteOperation(Context context, Collection<StructureIdentifier> requested) {
        Collection<StructureIdentifier> known = getKnownIdentifiers(context);
        Collection<StructureIdentifier> delta = getDeltaMinusIdentifiers(requested, known);
        switch (context) {
            case BCIF:
                deleteStructuresFromArchiveTask.execute(delta);
                break;
            case STRUCTURES:
                deleteStructuresFromStructureRepositoryTask.execute(delta);
                break;
            case INDEX:
                deleteStructuresFromInvertedIndexTask.execute(delta);
                break;
            case ALL:
                deleteStructuresFromArchiveTask.execute(delta);
                deleteStructuresFromStructureRepositoryTask.execute(delta);
                deleteStructuresFromInvertedIndexTask.execute(delta);
                break;
        }
    }

    /**
     * Determine all IDs that need to be added to the archive.
     * @param requested the requested update
     * @param known the registered identifiers
     * @return array of IDs that need to be processed for the given context
     */
    public Collection<StructureIdentifier> getDeltaPlusIdentifiers(Collection<StructureIdentifier> requested, Collection<StructureIdentifier> known) {
        if (known.isEmpty()) {
            logger.warn("No existing data - starting from scratch");
            return requested;
        } else {
            return requested.stream()
                    .filter(id -> !known.contains(id))
                    .collect(Collectors.toSet());
        }
    }

    /**
     * Determine all IDs that need to be removed from the archive.
     * @param requested the requested update
     * @param known the registered identifiers
     * @return array of IDs that need to be remove for the given context
     */
    public Collection<StructureIdentifier> getDeltaMinusIdentifiers(Collection<StructureIdentifier> requested, Collection<StructureIdentifier> known) {
        if (known.isEmpty()) {
            logger.warn("No existing data - no need for cleanup of obsolete entries");
            return Collections.emptySet();
        } else {
            return known.stream()
                    .filter(requested::contains)
                    .collect(Collectors.toSet());
        }
    }

    public Collection<StructureIdentifier> getKnownIdentifiers(Context context) {
        if (context == Context.BCIF) {
            return stateRepository.selectKnown();
        } else if (context == Context.INDEX) {
            return stateRepository.selectIndexed();
        } else if (context == Context.STRUCTURES) {
            return stateRepository.selectSupported();
        } else {
            throw new UnsupportedOperationException("Context " + context + " not supported");
        }
    }
}
