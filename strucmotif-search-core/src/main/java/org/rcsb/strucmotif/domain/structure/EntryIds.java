package org.rcsb.strucmotif.domain.structure;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * A collection of functions dealing with structure entry identifiers.
 * <p>
 * PDB entries come in two shapes: the extended ID ({@code pdb_} plus 8 lowercase alphanumerics, e.g.
 * {@code pdb_00004hhb}) and the legacy 4-character ID (e.g. {@code 4HHB}). Everything else is a computed structure
 * model, e.g. {@code AF_AFA0A009IHW8F1}.
 * <p>
 * Identifiers are expected to be well-formed, nothing here rejects a value. Values that aren't identifiers at all,
 * such as URLs or local keys, pass through unchanged.
 */
public class EntryIds {
    private EntryIds() {}

    private static final Pattern EXTENDED_PDB_ID = Pattern.compile("^pdb_[a-z0-9]{8}$");
    private static final Pattern LEGACY_PDB_ID = Pattern.compile("^\\d[a-zA-Z0-9]{3}$");
    /** Prepended to a legacy ID to form its extended equivalent, e.g. 4hhb becomes pdb_00004hhb. */
    private static final String EXTENDED_PADDING = "pdb_0000";
    /** Index of the 4-character code embedded in an extended PDB ID. */
    private static final int EXTENDED_CODE_START = 8;

    /**
     * Tests whether a string is an extended PDB ID, e.g. {@code pdb_00004hhb}.
     * @param value the string to test, may be null
     * @return true if so
     */
    public static boolean isExtendedPdbId(String value) {
        return value != null && EXTENDED_PDB_ID.matcher(value).matches();
    }

    /**
     * Tests whether a string is a legacy 4-character PDB ID, e.g. {@code 4HHB}.
     * @param value the string to test, may be null
     * @return true if so
     */
    public static boolean isLegacyPdbId(String value) {
        return value != null && LEGACY_PDB_ID.matcher(value).matches();
    }

    /**
     * Tests whether a string is a PDB ID of either shape.
     * @param value the string to test, may be null
     * @return true if so
     */
    public static boolean isPdbId(String value) {
        return isExtendedPdbId(value) || isLegacyPdbId(value);
    }

    /**
     * Tests whether a string is a computed structure model ID, e.g. {@code AF_AFA0A009IHW8F1}. Anything that isn't a
     * PDB ID is one.
     * @param value the string to test, may be null
     * @return true if so
     */
    public static boolean isComputedModelId(String value) {
        return value != null && !isPdbId(value);
    }

    /**
     * The form an identifier takes in the index: extended for PDB entries, unchanged otherwise. Callers may still hand
     * out legacy IDs - REST clients, external data sources such as M-CSA - so they are mapped here before anything is
     * looked up or compared.
     * @param identifier the raw identifier, may be null
     * @return the identifier as it is stored
     */
    public static String indexed(String identifier) {
        if (!isLegacyPdbId(identifier)) {
            return identifier;
        }
        return EXTENDED_PADDING + identifier.toLowerCase(Locale.ROOT);
    }

    /**
     * The legacy 4-character code an extended PDB ID corresponds to, e.g. {@code 4hhb} for {@code pdb_00004hhb}. This
     * is the inverse of {@link #indexed(String)} and only defined for the extended IDs that were migrated from a legacy
     * one: identifiers issued natively after the cutover ({@code pdb_1000axyz}) don't have a legacy form.
     * @param identifier the identifier to convert, may be null
     * @return the legacy ID, or null if there is none
     */
    public static String legacyCode(String identifier) {
        if (!isExtendedPdbId(identifier) || !identifier.startsWith(EXTENDED_PADDING)) {
            return null;
        }
        String code = identifier.substring(EXTENDED_CODE_START);
        return isLegacyPdbId(code) ? code : null;
    }

    /**
     * The canonical form of an identifier whose case may have been lost, e.g. by round-tripping through a CIF block
     * header - ciftools upper-cases those, so {@code pdb_00004hhb} comes back as {@code PDB_00004HHB}. Extended PDB IDs
     * are lower-cased; legacy and computed structure model IDs are returned unchanged, since upper-case is their
     * conventional form.
     * @param value the identifier, in any case, may be null
     * @return the identifier in its canonical case
     */
    public static String canonical(String value) {
        if (value == null) {
            return null;
        }
        String lowerCase = value.toLowerCase(Locale.ROOT);
        return isExtendedPdbId(lowerCase) ? lowerCase : value;
    }

    /**
     * The two characters used to shard an identifier across directories, e.g. {@code hh} for both {@code 4HHB} and
     * {@code pdb_00004hhb}. Extended IDs are sharded on the two characters of their embedded code, so that they bucket
     * like the legacy IDs they correspond to rather than all landing under {@code db}.
     * @param identifier the identifier to shard
     * @return the two-character shard
     */
    public static String middle(String identifier) {
        int start = isExtendedPdbId(identifier) ? EXTENDED_CODE_START + 1 : 1;
        return identifier.substring(start, start + 2);
    }
}
