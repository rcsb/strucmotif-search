package org.rcsb.strucmotif.update;

import com.google.gson.Gson;
import org.rcsb.strucmotif.MotifSearch;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.io.read.RenumberedReader;
import org.rcsb.strucmotif.io.write.StructureWriter;
import org.rcsb.strucmotif.persistence.InvertedIndex;
import org.rcsb.strucmotif.persistence.UpdateStateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class MotifSearchUpdate {
    private static final Logger logger = LoggerFactory.getLogger(MotifSearchUpdate.class);
    private static final String TASK_NAME = MotifSearchUpdate.class.getSimpleName();
    private static final String RCSB_ENTRY_LIST = "http://www.rcsb.org/pdb/json/getCurrent";

    public enum Context {
        ARCHIVE,
        RESIDUE,
        INDEX
    }

    public static void main(String[] args) throws IOException {
        StructureWriter renumberedWriter = MotifSearch.getInstance(StructureWriter.class);
        RenumberedReader renumberedReader = MotifSearch.getInstance(RenumberedReader.class);
        InvertedIndex motifLookup = MotifSearch.getInstance(InvertedIndex.class);
        MongoResidueDB residueDB = MotifSearch.getInstance(MongoResidueDB.class);
        MongoTitleDB titleDB = MotifSearch.getInstance(MongoTitleDB.class);
        UpdateStateManager updateStateManager = MotifSearch.getInstance(UpdateStateManager.class);

        Set<StructureIdentifier> all = getAllIdentifiers();

        // perform all steps that add structures to archive, database, or lookup
        for (Context context : Context.values()) {
            Set<StructureIdentifier> known = getKnownIdentifiers(context, updateStateManager);
            Set<StructureIdentifier> ids = getDeltaPlusIdentifiers(all, known);
                logger.info("[{}] Incremental mode - {} entries ({})",
                        TASK_NAME,
                        ids.size(),
                        ids);

            switch (context) {
                case ARCHIVE:
                    new AddStructuresToArchiveTask(ids, renumberedWriter, updateStateManager);
                    break;
                case RESIDUE:
                    if (!MotifSearch.NO_DB) {
                        new AddStructuresToStructureDBTask(ids, renumberedReader, residueDB, titleDB, updateStateManager);
                    }
                    break;
                case INDEX:
                    new AddStructuresToInvertedIndexTask(ids, motifLookup, renumberedReader, updateStateManager);
                    break;
            }
        }

        // perform all steps that remove structures from archive, database, or lookup
        for (Context context : Context.values()) {
            Set<StructureIdentifier> known = getKnownIdentifiers(context, updateStateManager);
            Set<StructureIdentifier> ids = getDeltaMinusIdentifiers(all, known);
            logger.info("[{}] Incremental mode - {} entries ({})",
                    TASK_NAME,
                    ids.size(),
                    ids);

            switch (context) {
                case ARCHIVE:
                    new DeleteStructuresFromArchiveTask(ids, updateStateManager);
                    break;
                case RESIDUE:
                    if (!MotifSearch.NO_DB) {
                        new DeleteStructuresFromStructureDBTask(ids, residueDB, titleDB, updateStateManager);
                    }
                    break;
                case INDEX:
                    new DeleteStructuresFromInvertedIndexTask(ids, motifLookup, updateStateManager);
                    break;
            }
        }
    }

    /**
     * Determine all IDs that need to be added to the archive.
     * @param requested the requested update
     * @param known the registered identifiers
     * @return array of IDs that need to be processed for the given context
     */
    public static Set<StructureIdentifier> getDeltaPlusIdentifiers(Set<StructureIdentifier> requested, Set<StructureIdentifier> known) {
        if (known.isEmpty()) {
            logger.warn("[{}] No existing data - starting from scratch",
                    TASK_NAME);
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
    public static Set<StructureIdentifier> getDeltaMinusIdentifiers(Set<StructureIdentifier> requested, Set<StructureIdentifier> known) {
        if (known.isEmpty()) {
            logger.warn("[{}] No existing data - no need for cleanup of obsolete entries",
                    TASK_NAME);
            return Collections.emptySet();
        } else {
            return known.stream()
                    .filter(id -> !requested.contains(id))
                    .collect(Collectors.toSet());
        }
    }

    public static Set<StructureIdentifier> getKnownIdentifiers(Context context, UpdateStateManager updateStateManager) {
        if (context == Context.ARCHIVE) {
            return updateStateManager.selectArchiveEntries();
        } else if (context == Context.INDEX) {
            return updateStateManager.selectInvertedIndexEntries();
        } else if (context == Context.RESIDUE) {
            return updateStateManager.selectResidueDBEntries();
        } else {
            throw new UnsupportedOperationException("context " + context + " not supported");
        }
    }

    public static Set<StructureIdentifier> getAllIdentifiers() throws IOException {
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
}
