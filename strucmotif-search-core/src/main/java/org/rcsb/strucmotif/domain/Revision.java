package org.rcsb.strucmotif.domain;

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
