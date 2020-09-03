package org.rcsb.strucmotif.update;

import com.google.gson.Gson;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

public class MotifSearchUpdateCommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(MotifSearchUpdateCommandLineRunner.class);
    private static final String RCSB_ENTRY_LIST = "http://www.rcsb.org/pdb/json/getCurrent";

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Too few arguments");
            System.out.println("Usage: java -Xmx12G -jar update.jar context operation ...");
            System.out.println("Valid context values: " + Arrays.toString(Context.values()));
            System.out.println("Valid operation values: " + Arrays.toString(Operation.values()));
            System.out.println("Optionally: list of entry ids - (no argument performs null operation, use single argument 'full' for complete update)");
            System.out.println("This call is idempotent - if you want to update entries you have to explicitly remove them first");
            System.out.println("Example: java -Xmx12G -jar update.jar ARCHIVE ADD 1acj 1exr 4hhb");
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

        // TODO acquire
        MotifSearchUpdate update = null;

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
                update.handleAddOperation(context, identifiers);
                break;
            case REMOVE:
                update.handleDeleteOperation(context, identifiers);
                break;
        }
    }

    public static Collection<StructureIdentifier> getAllIdentifiers() throws IOException {
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
}
