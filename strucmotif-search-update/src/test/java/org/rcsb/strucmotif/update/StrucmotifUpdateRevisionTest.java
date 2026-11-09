package org.rcsb.strucmotif.update;

import org.junit.jupiter.api.Test;
import org.rcsb.cif.CifIO;
import org.rcsb.cif.schema.StandardSchemata;
import org.rcsb.cif.schema.mm.PdbxAuditRevisionHistory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * Which revision gets recorded for an entry. Delta updates compare it to rcsb_accession_info, which reports the
 * 'Structure model' revision, recording anything else makes the entry look modified on every run.
 */
class StrucmotifUpdateRevisionTest {
    /**
     * 21VV: the last row is 'EM metadata' 2.2, the coordinates are at 1.3 (rcsb_accession_info: 1.3).
     */
    @Test
    void whenEmMetadataIsAheadOfStructureModel_thenStructureModelRevisionReported() throws IOException {
        assertArrayEquals(new int[] { 1, 3 }, revision("""
                1  'Structure model' 1 0
                2  'EM metadata'     1 0
                3  'Primary map'     1 0
                4  'Structure model' 1 1
                5  'EM metadata'     2 0
                6  'Structure model' 1 2
                7  'EM metadata'     2 1
                8  'Structure model' 1 3
                9  'EM metadata'     2 2
                """));
    }

    /**
     * 10ZM: the last row is 'EM metadata' 1.3, the coordinates are at 2.0 (rcsb_accession_info: 2.0).
     */
    @Test
    void whenEmMetadataIsBehindStructureModel_thenStructureModelRevisionReported() throws IOException {
        assertArrayEquals(new int[] { 2, 0 }, revision("""
                1  'Structure model' 1 0
                2  'EM metadata'     1 0
                3  'Half map'        1 0
                4  'Structure model' 1 2
                5  'EM metadata'     1 2
                6  'Structure model' 2 0
                7  'EM metadata'     1 3
                """));
    }

    @Test
    void whenOnlyStructureModelRows_thenLatestReported() throws IOException {
        // computed structure models, e.g. AF_AFI1KTD4F1
        assertArrayEquals(new int[] { 6, 0 }, revision("""
                1 'Structure model' 1 0
                2 'Structure model' 5 0
                3 'Structure model' 6 0
                """));
    }

    @Test
    void whenNoDataContentType_thenLastRowReported() throws IOException {
        PdbxAuditRevisionHistory history = parse("""
                loop_
                _pdbx_audit_revision_history.ordinal
                _pdbx_audit_revision_history.major_revision
                _pdbx_audit_revision_history.minor_revision
                1 1 0
                2 1 4
                """);
        assertArrayEquals(new int[] { 1, 4 }, StrucmotifUpdate.latestStructureModelRevision(history));
    }

    private static int[] revision(String rows) throws IOException {
        return StrucmotifUpdate.latestStructureModelRevision(parse("""
                loop_
                _pdbx_audit_revision_history.ordinal
                _pdbx_audit_revision_history.data_content_type
                _pdbx_audit_revision_history.major_revision
                _pdbx_audit_revision_history.minor_revision
                """ + rows));
    }

    private static PdbxAuditRevisionHistory parse(String loop) throws IOException {
        String cif = "data_TEST\n" + loop + "#\n";
        return CifIO.readFromInputStream(new ByteArrayInputStream(cif.getBytes(StandardCharsets.UTF_8)))
                .as(StandardSchemata.MMCIF)
                .getFirstBlock()
                .getPdbxAuditRevisionHistory();
    }
}
