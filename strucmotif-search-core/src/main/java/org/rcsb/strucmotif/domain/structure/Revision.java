package org.rcsb.strucmotif.domain.structure;

import org.rcsb.cif.schema.mm.MmCifFile;
import org.rcsb.cif.schema.mm.PdbxAuditRevisionHistory;

import java.util.Objects;

/**
 * The version of a PDB entry.
 */
public class Revision {
    private final int major;
    private final int minor;

    /**
     * Construct a version tag.
     * @param major major version
     * @param minor minor version
     */
    public Revision(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    /**
     * Parse revision information from a file.
     * @param mmCifFile the source file
     */
    public Revision(MmCifFile mmCifFile) {
        PdbxAuditRevisionHistory pdbxAuditRevisionHistory = mmCifFile.getFirstBlock().getPdbxAuditRevisionHistory();
        int last = pdbxAuditRevisionHistory.getRowCount() - 1;
        this.major = pdbxAuditRevisionHistory.getMajorRevision().get(last);
        this.minor = pdbxAuditRevisionHistory.getMinorRevision().get(last);
    }

    /**
     * The major version component.
     * @return an int
     */
    public int getMajor() {
        return major;
    }

    /**
     * The minor version component.
     * @return an int
     */
    public int getMinor() {
        return minor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Revision revision = (Revision) o;
        return major == revision.major &&
                minor == revision.minor;
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor);
    }
}

