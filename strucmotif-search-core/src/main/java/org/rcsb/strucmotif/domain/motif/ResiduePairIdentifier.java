package org.rcsb.strucmotif.domain.motif;

import org.rcsb.strucmotif.domain.selection.AuthorSelection;
import org.rcsb.strucmotif.domain.selection.IndexSelection;

import java.util.Objects;

/**
 * Residue pair identifiers combine the {@link IndexSelection} of each residue making up this pair.
 */
public class ResiduePairIdentifier {
    private final IndexSelection indexSelection1;
    private final IndexSelection indexSelection2;
    private final AuthorSelection authorSelection1;
    private final AuthorSelection authorSelection2;

    public ResiduePairIdentifier(IndexSelection indexSelection1, IndexSelection indexSelection2) {
        this(indexSelection1, indexSelection2, null, null);
    }

    public ResiduePairIdentifier(IndexSelection indexSelection1, IndexSelection indexSelection2, AuthorSelection authorSelection1, AuthorSelection authorSelection2) {
        this.indexSelection1 = indexSelection1;
        this.indexSelection2 = indexSelection2;
        this.authorSelection1 = authorSelection1;
        this.authorSelection2 = authorSelection2;
    }

    public IndexSelection getIndexSelection1() {
        return indexSelection1;
    }

    public IndexSelection getIndexSelection2() {
        return indexSelection2;
    }

    public AuthorSelection getAuthorSelection1() {
        return authorSelection1;
    }

    public AuthorSelection getAuthorSelection2() {
        return authorSelection2;
    }

    public boolean providesAuthorSelection() {
        return authorSelection1 != null && authorSelection2 != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResiduePairIdentifier that = (ResiduePairIdentifier) o;
        return Objects.equals(indexSelection1, that.indexSelection1) &&
                Objects.equals(indexSelection2, that.indexSelection2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(indexSelection1, indexSelection2);
    }

    @Override
    public String toString() {
        return indexSelection1 + "+" + indexSelection2;
    }
}
