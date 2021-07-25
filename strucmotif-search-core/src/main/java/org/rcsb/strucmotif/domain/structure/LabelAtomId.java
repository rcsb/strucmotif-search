package org.rcsb.strucmotif.domain.structure;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum LabelAtomId {
    C("C"),
    C1_PRIME("C1'"),
    C2("C2"),
    C2_PRIME("C2'"),
    C3_PRIME("C3'"),
    C4("C4"),
    C4_PRIME("C4'"),
    C5("C5"),
    C5_PRIME("C5'"),
    C6("C6"),
    C8("C8"),
    CA("CA"),
    CB("CB"),
    CD("CD"),
    CD1("CD1"),
    CD2("CD2"),
    CE("CE"),
    CE1("CE1"),
    CE2("CE2"),
    CE3("CE3"),
    CG("CG"),
    CG1("CG1"),
    CG2("CG2"),
    CH2("CH2"),
    CZ("CZ"),
    CZ2("CZ2"),
    CZ3("CZ3"),
    N("N"),
    N1("N1"),
    N2("N2"),
    N3("N3"),
    N4("N4"),
    N6("N6"),
    N7("N7"),
    N9("N9"),
    ND1("ND1"),
    ND2("ND2"),
    NE("NE"),
    NE1("NE1"),
    NE2("NE2"),
    NH1("NH1"),
    NH2("NH2"),
    NZ("NZ"),
    O("O"),
    O1P("O1P"),
    O2("O2"),
    O2_PRIME("O2'"),
    O2P("O2P"),
    O3_PRIME("O3'"),
    O4("O4"),
    O4_PRIME("O4'"),
    O5_PRIME("O5'"),
    O6("O6"),
    OD1("OD1"),
    OD2("OD2"),
    OE1("OE1"),
    OE2("OE2"),
    OG("OG"),
    OG1("OG1"),
    OH("OH"),
    OP1("OP1"),
    OP2("OP2"),
    OP3("OP3"),
    OXT("OXT"),
    P("P"),
    SD("SD"),
    SG("SG"),
    UNKNOWN_ATOM("?");
    
    private final String labelAtomId;

    LabelAtomId(String labelAtomId) {
        this.labelAtomId = labelAtomId;
    }

    public String getLabelAtomId() {
        return labelAtomId;
    }

    private static final Map<String, LabelAtomId> MAPPING = Arrays.stream(LabelAtomId.values())
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
