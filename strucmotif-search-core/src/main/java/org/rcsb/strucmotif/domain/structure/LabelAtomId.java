package org.rcsb.strucmotif.domain.structure;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Enumerates all 'common' atoms by name.
 */
public enum LabelAtomId {
    /**
     * C atom.
     */
    C("C"),
    /**
     * C1' atom.
     */
    C1_PRIME("C1'"),
    /**
     * C2 atom.
     */
    C2("C2"),
    /**
     * C2' atom.
     */
    C2_PRIME("C2'"),
    /**
     * C3' atom.
     */
    C3_PRIME("C3'"), // 5
    /**
     * C4 atom.
     */
    C4("C4"),
    /**
     * C4' atom.
     */
    C4_PRIME("C4'"),
    /**
     * C5 atom.
     */
    C5("C5"),
    /**
     * C5' atom.
     */
    C5_PRIME("C5'"),
    /**
     * C6 atom.
     */
    C6("C6"), // 10
    /**
     * C8 atom.
     */
    C8("C8"),
    /**
     * CA atom.
     */
    CA("CA"),
    /**
     * CB atom.
     */
    CB("CB"),
    /**
     * CD atom.
     */
    CD("CD"),
    /**
     * CD1 atom.
     */
    CD1("CD1"), // 15
    /**
     * CD2 atom.
     */
    CD2("CD2"),
    /**
     * CE atom.
     */
    CE("CE"),
    /**
     * CE1 atom.
     */
    CE1("CE1"),
    /**
     * CE2 atom.
     */
    CE2("CE2"),
    /**
     * CE3 atom.
     */
    CE3("CE3"), // 20
    /**
     * CG atom.
     */
    CG("CG"),
    /**
     * CG1 atom.
     */
    CG1("CG1"),
    /**
     * CG2 atom.
     */
    CG2("CG2"),
    /**
     * CH2 atom.
     */
    CH2("CH2"),
    /**
     * CZ atom.
     */
    CZ("CZ"), // 25
    /**
     * CZ2 atom.
     */
    CZ2("CZ2"),
    /**
     * CZ3 atom.
     */
    CZ3("CZ3"),
    /**
     * N atom.
     */
    N("N"),
    /**
     * N1 atom.
     */
    N1("N1"),
    /**
     * N2 atom.
     */
    N2("N2"), // 30
    /**
     * N3 atom.
     */
    N3("N3"),
    /**
     * N4 atom.
     */
    N4("N4"),
    /**
     * N6 atom.
     */
    N6("N6"),
    /**
     * N7 atom.
     */
    N7("N7"),
    /**
     * N9 atom.
     */
    N9("N9"), // 35
    /**
     * ND1 atom.
     */
    ND1("ND1"),
    /**
     * ND2 atom.
     */
    ND2("ND2"),
    /**
     * NE atom.
     */
    NE("NE"),
    /**
     * NE1 atom.
     */
    NE1("NE1"),
    /**
     * NE2 atom.
     */
    NE2("NE2"), // 40
    /**
     * NH1 atom.
     */
    NH1("NH1"),
    /**
     * NH2 atom.
     */
    NH2("NH2"),
    /**
     * NZ atom.
     */
    NZ("NZ"),
    /**
     * O atom.
     */
    O("O"),
    /**
     * O1P atom.
     */
    O1P("O1P"), // 45
    /**
     * O2 atom.
     */
    O2("O2"),
    /**
     * O2' atom.
     */
    O2_PRIME("O2'"),
    /**
     * O2P atom.
     */
    O2P("O2P"),
    /**
     * O3' atom.
     */
    O3_PRIME("O3'"),
    /**
     * O4 atom.
     */
    O4("O4"), // 50
    /**
     * O4' atom.
     */
    O4_PRIME("O4'"),
    /**
     * O5' atom.
     */
    O5_PRIME("O5'"),
    /**
     * O6 atom.
     */
    O6("O6"),
    /**
     * OD1 atom.
     */
    OD1("OD1"),
    /**
     * OD2 atom.
     */
    OD2("OD2"), // 55
    /**
     * OE1 atom.
     */
    OE1("OE1"),
    /**
     * OE2 atom.
     */
    OE2("OE2"),
    /**
     * OG atom.
     */
    OG("OG"),
    /**
     * OG1 atom.
     */
    OG1("OG1"),
    /**
     * OH atom.
     */
    OH("OH"), // 60
    /**
     * OP1 atom.
     */
    OP1("OP1"),
    /**
     * OP2 atom.
     */
    OP2("OP2"),
    /**
     * OP3 atom.
     */
    OP3("OP3"),
    /**
     * OXT atom.
     */
    OXT("OXT"),
    /**
     * P atom.
     */
    P("P"), // 65
    /**
     * SD atom.
     */
    SD("SD"),
    /**
     * SG atom.
     */
    SG("SG"),
    /**
     * Unknown atom.
     */
    UNKNOWN_ATOM("?");

    /**
     * Cached values of this enum. Don't manipulate this array of things will burn.
     */
    public static final LabelAtomId[] values = values();

    private final String labelAtomId;

    LabelAtomId(String labelAtomId) {
        this.labelAtomId = labelAtomId;
    }

    /**
     * The label_atom_id of an atom.
     * @return a String
     */
    public String getLabelAtomId() {
        return labelAtomId;
    }

    private static final Map<String, LabelAtomId> MAPPING = Arrays.stream(LabelAtomId.values)
            .collect(Collectors.toMap(LabelAtomId::getLabelAtomId, Function.identity()));
    /**
     * Resolve a label_atom_id.
     * @param labelAtomId the label atom id
     * @return a LabelAtomId or null (if 'non-standard', e.g. occurring for 1 of the registered ResidueTypes)
     */
    public static LabelAtomId ofLabelAtomId(String labelAtomId) {
        LabelAtomId out = MAPPING.get(labelAtomId);
        return out != null ? out : LabelAtomId.UNKNOWN_ATOM;
    }
}
