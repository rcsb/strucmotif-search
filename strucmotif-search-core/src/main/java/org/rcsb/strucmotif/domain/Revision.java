package org.rcsb.strucmotif.domain;

import java.util.Objects;

/**
 * The version of a PDB entry.
 */
public class Revision {
    private final int major;
    private final int minor;

    public Revision(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    public int getMajor() {
        return major;
    }

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
