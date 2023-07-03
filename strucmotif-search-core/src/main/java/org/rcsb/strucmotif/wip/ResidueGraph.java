package org.rcsb.strucmotif.wip;

import org.rcsb.strucmotif.align.QuaternionAlignmentService;
import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.domain.Transformation;
import org.rcsb.strucmotif.domain.motif.*;
import org.rcsb.strucmotif.domain.structure.LabelAtomId;
import org.rcsb.strucmotif.domain.structure.LabelSelection;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.math.Algebra;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static org.rcsb.strucmotif.math.Algebra.*;
import static org.rcsb.strucmotif.math.Algebra.normalize3d;

public class ResidueGraph {
    private static final Logger logger = LoggerFactory.getLogger(ResidueGraph.class);
    private final Structure structure;
    private int matrixSize;
//    // holds backbone distance (32 values), side-chain distance (32 values), and angle (10 values) between a residue pair
//    private short[] data;
//    private int[] columnIndices;
//    private int[] rowIndices;
    private Map<Long, Short> data;
    private final int selectionCount;
    private final int residueCount;
    private final int pairingCount;

    public enum ResidueGraphMode {
        /**
         * Only report contacts of deposited coordinates.
         */
        DEPOSITED,
        /**
         * Report contacts between deposited coordinates as well as deposited coordinates and a transformed partner.
         */
        DEPOSITED_AND_CONTACTS,
        /**
         * Report contacts within a specific assembly.
         */
        ASSEMBLY,
        /**
         * Report all contacts for a list of {@link LabelSelection} values.
         */
        SELECTION
    }

    public static record ResidueGraphOptions(ResidueGraphMode mode, String assemblyIdentifier, List<Map<LabelAtomId, float[]>> residues, List<LabelSelection> selections) {
        /**
         * Only report contacts of deposited coordinates.
         *
         * @return the corresponding options
         */
        public static ResidueGraphOptions deposited() {
            return new ResidueGraphOptions(ResidueGraphMode.DEPOSITED, null, null, null);
        }

        /**
         * Report contacts between deposited coordinates as well as deposited coordinates and a transformed partner.
         *
         * @return the corresponding options
         */
        public static ResidueGraphOptions depositedAndContacts() {
            return new ResidueGraphOptions(ResidueGraphMode.DEPOSITED_AND_CONTACTS, null, null, null);
        }

        /**
         * Only report contacts of deposited coordinates.
         *
         * @param assemblyIdentifier which assembly to consider?
         * @return the corresponding options
         */
        public static ResidueGraphOptions assembly(String assemblyIdentifier) {
            return new ResidueGraphOptions(ResidueGraphMode.ASSEMBLY, assemblyIdentifier, null, null);
        }

        /**
         * Report all contacts, regardless of assembly or applied transformation. This makes only sense when the graph
         * is computed on a substructure, defined by a number of {@link LabelSelection}.
         *
         * @param residues the manifested residues of interest
         * @param selections the respective selection
         * @return the corresponding options
         */
        public static ResidueGraphOptions selection(List<Map<LabelAtomId, float[]>> residues, List<LabelSelection> selections) {
            return new ResidueGraphOptions(ResidueGraphMode.SELECTION, null, residues, selections);
        }
    }

    public ResidueGraph(Structure structure, StrucmotifConfig strucmotifConfig, ResidueGraphOptions options) {
        this.structure = structure;

        ResidueVectors residueVectors = populateResidueVectors(structure, options);

        this.selectionCount = options.mode == ResidueGraphMode.SELECTION ? options.selections.size() : residueVectors.backboneVectors.length;
        this.residueCount = residueVectors.backboneVectors.length / 3;
        this.pairingCount = fillResidueGrid(structure, residueVectors, strucmotifConfig.getSquaredDistanceCutoff(), options);
    }

    static record ResidueVectors(int[] residueIndices, float[] backboneVectors, float[] sideChainVectors, float[] normalVectors) {}

    private ResidueVectors populateResidueVectors(Structure structure, ResidueGraphOptions options) {
        int valid = 0;
        int bound = options.mode == ResidueGraphMode.SELECTION ? options.selections.size() : structure.getInstancedResidueCount();
        int[] residueIndices = new int[bound];
        float[] backboneVectors = new float[3 * bound];
        float[] sideChainVectors = new float[3 * bound];
        float[] normalVectors = new float[3 * bound];

        if (options.mode == ResidueGraphMode.SELECTION) {
            for (int i = 0; i < residueIndices.length; i++) {
                LabelSelection labelSelection = options.selections.get(i);
                // this may not be the actual residueIndex retrieved during search, but it doesn't matter
                int residueIndex = structure.getResidueIndex(labelSelection);
                residueIndices[i] = residueIndex;
            }
        } else {
            for (int i = 0; i < structure.getInstancedResidueCount(); i++) {
                residueIndices[i] = i;
            }
        }

        for (int i = 0; i < residueIndices.length; i++) {
            ResidueType residueType = structure.getResidueType(i);
            Map<LabelAtomId, float[]> residue = structure.manifestResidue(residueIndices[i]);

            float[] backbone = getBackboneCoords(residue);
            if (backbone == null) {
                continue;
            }
            float[] sideChain = residueType == ResidueType.GLYCINE ? getVirtualCB(residue) : getSideChainCoords(residue);
            if (sideChain == null) {
                continue;
            }

            residueIndices[valid] = i;
            int insertionPoint = valid * 3;
            System.arraycopy(backbone, 0, backboneVectors, insertionPoint, 3);
            System.arraycopy(sideChain, 0, sideChainVectors, insertionPoint, 3);
            float[] normalVector = normalVector(backbone, sideChain);
            System.arraycopy(normalVector, 0, normalVectors, insertionPoint, 3);
            valid++;
        }

        // compact arrays
        residueIndices = Arrays.copyOf(residueIndices, valid);
        backboneVectors = Arrays.copyOf(backboneVectors, valid * 3);
        sideChainVectors = Arrays.copyOf(sideChainVectors, valid * 3);
        normalVectors = Arrays.copyOf(normalVectors, valid * 3);

        return new ResidueVectors(residueIndices, backboneVectors, sideChainVectors, normalVectors);
    }

    private int fillResidueGrid(Structure structure, ResidueVectors residueVectors, float squaredDistanceCutoff, ResidueGraphOptions options) {
        // temporary ResidueGrid for efficient distance calculation
        ResidueGrid residueGrid = new ResidueGrid(residueVectors, squaredDistanceCutoff);
        ResidueGraphMode mode = options.mode;

        List<Set<String>> assemblyGrouping = new ArrayList<>();
        for (String assemblyIdentifier : structure.getAssemblyIdentifiers()) {
            Set<String> referencedTransformationIdentifiers = new HashSet<>();
            String[] referencedChainInstances = structure.getReferencedChainInstances(assemblyIdentifier);
            for (int i = 0; i < referencedChainInstances.length - 1; i = i + 2) {
                referencedTransformationIdentifiers.add(referencedChainInstances[i] + "_" + referencedChainInstances[i + 1]);
            }
            assemblyGrouping.add(referencedTransformationIdentifiers);
        }

        // track the first occurrence of each chain and consider that 'deposited'
        Set<String> acceptedChains = new HashSet<>();
        Set<String> acceptedTransformationIdentifiers = new HashSet<>();
        if (mode == ResidueGraphMode.DEPOSITED || mode == ResidueGraphMode.DEPOSITED_AND_CONTACTS) {
            // TODO add whole chains in contact if mode = deposited_and_contacts
            for (String assemblyIdentifier : structure.getAssemblyIdentifiers()) {
                String[] referencedChainInstances = structure.getReferencedChainInstances(assemblyIdentifier);
                for (int i = 0; i < referencedChainInstances.length - 1; i = i + 2) {
                    String chainIdentifier = referencedChainInstances[i];
                    if (!acceptedChains.contains(chainIdentifier)) {
                        acceptedChains.add(chainIdentifier);
                        acceptedTransformationIdentifiers.add(chainIdentifier + "_" + referencedChainInstances[i + 1]);
                    }
                }
            }
        } else if (mode == ResidueGraphMode.ASSEMBLY) {
            String[] requestedChains = structure.getReferencedChainInstances(options.assemblyIdentifier);
            for (int i = 0; i < requestedChains.length - 1; i = i + 2) {
                acceptedTransformationIdentifiers.add(requestedChains[i] + "_" + requestedChains[i + 1]);
            }
        }

        int contacts = 0;
        this.data = new LinkedHashMap<>();
        int[] residueIndices = residueVectors.residueIndices;
        for (ResidueGrid.ResidueContact residueContact : residueGrid.getIndicesContacts()) {
            int residueIndex1 = residueIndices[residueContact.i()];
            int residueIndex2 = residueIndices[residueContact.j()];

            String transformationIdentifier1 = structure.getLabelAsymId(residueIndex1) + "_" + structure.getTransformationIdentifier(residueIndex1);
            String transformationIdentifier2 = structure.getLabelAsymId(residueIndex2) + "_" + structure.getTransformationIdentifier(residueIndex2);
            if (mode != ResidueGraphMode.SELECTION) {
                if (!acceptedTransformationIdentifiers.contains(transformationIdentifier1)) {
                    continue;
                }

                if (mode == ResidueGraphMode.DEPOSITED || mode == ResidueGraphMode.ASSEMBLY) {
                    if (!acceptedTransformationIdentifiers.contains(transformationIdentifier2)) {
                        continue;
                    }
                }
            }

            // ensure both chain instances are part of the same assembly
            if (assemblyGrouping.stream().noneMatch(group -> group.contains(transformationIdentifier1) && group.contains(transformationIdentifier2))) {
                continue;
            }

            DistanceType backboneDistance = DistanceType.ofDistance(residueContact.distance());
            DistanceType sideChainDistance = DistanceType.ofDistance((float) Math.sqrt(distanceSquared3d(residueVectors.sideChainVectors, residueIndex1, residueIndex2)));
            AngleType angle = AngleType.ofAngle(angle(residueVectors.normalVectors, residueIndex1, residueIndex2));

            // jam all values into a single short
            long key = (long) residueIndex1 << 32 | residueIndex2;
            short value = encode(backboneDistance, sideChainDistance, angle);
            data.put(key, value);

            contacts++;
        }
        return contacts;
    }

    public int getSelectionCount() {
        return selectionCount;
    }

    public int getResidueCount() {
        return residueCount;
    }

    public int getPairingCount() {
        return pairingCount;
    }

    public LongStream pairingsSequential() {
        return data.keySet().stream().mapToLong(l -> l);
    }

    public LongStream pairingsParallel() {
        return data.keySet().parallelStream().mapToLong(l -> l);
    }

    public Stream<ResiduePairOccurrence> residuePairOccurrencesSequential() {
        return pairingsSequential()
                .mapToObj(this::createResiduePairOccurrence);
    }

    public Stream<ResiduePairOccurrence> residuePairOccurrencesParallel() {
        return pairingsParallel()
                .mapToObj(this::createResiduePairOccurrence);
    }

    private ResiduePairOccurrence createResiduePairOccurrence(long residuePairIdentifier) {
        long key = residuePairIdentifier;
        int residueIndex2 = (int) residuePairIdentifier & 0xFF;
        residuePairIdentifier >>= 32;
        int residueIndex1 = (int) residuePairIdentifier & 0xFF;

        ResidueType residueType1 = structure.getResidueType(residueIndex1);
        ResidueType residueType2 = structure.getResidueType(residueIndex2);
        short value = data.get(key);
        int residuePairDescriptor;

        // first residue must have lower one-letter code - if not: flip
        if (residueType1.getInternalCode().compareTo(residueType2.getInternalCode()) > 0) {
            residuePairIdentifier = (long) residueIndex2 << 32 | residueIndex1;
            residuePairDescriptor = encode(residueType2, residueType1, value);
        } else {
            residuePairDescriptor = encode(residueType1, residueType2, value);
        }

        return new ResiduePairOccurrence(residuePairDescriptor, residuePairIdentifier);
    }

    static short encode(DistanceType backboneDistance, DistanceType sideChainDistance, AngleType angle) {
        // XXCC CCCD DDDD EEEE - C: backboneDistance (32 values, 5 bits), D: sideChainDistance, E: angle (10 values, 4 bits)
        return (short) ((byte) backboneDistance.ordinal() << 10 | (byte) sideChainDistance.ordinal() << 5 | (byte) angle.ordinal());
    }

    static int encode(ResidueType residueType1, ResidueType residueType2, short otherProps) {
        return (byte) residueType1.ordinal() << 24 | (byte) residueType2.ordinal() << 17 | otherProps;
    }

    public static int encode(ResiduePairDescriptor residuePairDescriptor) {
        return encode(residuePairDescriptor.getResidueType1(), residuePairDescriptor.getResidueType2(), encode(residuePairDescriptor.getBackboneDistance(), residuePairDescriptor.getSideChainDistance(), residuePairDescriptor.getAngle()));
    }

    static int encode(ResidueType residueType1, ResidueType residueType2, DistanceType backboneDistance, DistanceType sideChainDistance, AngleType angle) {
        // XXXX AAAA AAAB BBBB BBCC CCCD DDDD EEEE - A: residueType1 (36 values, 7 bits), B: residueType2, C: backboneDistance (32 values, 5 bits), D: sideChainDistance, E: angle (10 values, 4 bits)
        return (byte) residueType1.ordinal() << 24 | (byte) residueType2.ordinal() << 17 | (byte) backboneDistance.ordinal() << 10 | (byte) sideChainDistance.ordinal() << 5 | (byte) angle.ordinal();
    }

    static Enum<?>[] decode(short value) {
        int k = value & 0x1F;
        value >>= 5;
        int j = value & 0x1F;
        value >>= 5;
        int i = value & 0x1F;
        return new Enum<?>[] { DistanceType.values()[i], DistanceType.values()[j], AngleType.values()[k] };
    }

    static Enum<?>[] decode(int value) {
        int m = value & 0x1F;
        value >>= 5;
        int l = value & 0x1F;
        value >>= 5;
        int k = value & 0x1F;
        value >>= 7;
        int j = value & 0x7F;
        value >>= 7;
        int i = value & 0x7F;
        return new Enum<?>[] { ResidueType.values()[i], ResidueType.values()[j], DistanceType.values()[k], DistanceType.values()[l], AngleType.values()[m] };
    }

    private static float[] getBackboneCoords(Map<LabelAtomId, float[]> residue) {
        if (residue.containsKey(LabelAtomId.CA)) {
            return residue.get(LabelAtomId.CA);
        }
        if (residue.containsKey(LabelAtomId.C4_PRIME)) {
            return residue.get(LabelAtomId.C4_PRIME);
        }
        return null;
    }

    private static float[] getSideChainCoords(Map<LabelAtomId, float[]> residue) {
        if (residue.containsKey(LabelAtomId.CB)) {
            return residue.get(LabelAtomId.CB);
        }
        if (residue.containsKey(LabelAtomId.C1_PRIME)) {
            return residue.get(LabelAtomId.C1_PRIME);
        }
        return null;
    }

    private static float[] normalVector(float[] a, float[] b) {
        float[] ba = new float[3];
        subtract3d(ba, b, a);
        normalize3d(ba, ba);
        return ba;
    }

    // already centered coordinates to save operations
    private static final List<float[]> REFERENCE_BACKBONE = List.of(new float[] { -0.698f, 0.184f, 1.008f }, // N
            new float[] { 0.525f, 0.109f, 0.200f }, // CA
            new float[] { 0.174f, -0.292f, -1.208f }); // C
    private static final float[] REFERENCE_CB = new float[] { 1.472f, -0.929f, 0.804f };
    private static final float[] REFERENCE_CENTROID = new float[3];

    static float[] getVirtualCB(Map<LabelAtomId, float[]> residue) {
        float[] n = residue.get(LabelAtomId.N);
        float[] ca = residue.get(LabelAtomId.CA);
        float[] c = residue.get(LabelAtomId.C);
        if (n == null || ca == null || c == null) {
            return null;
        }

        List<float[]> coords = List.of(n, ca, c);
        float[] v = Algebra.centroid3d(coords);

        // TODO change this to flat array?
        Transformation transformation = QuaternionAlignmentService.align(coords, v, REFERENCE_BACKBONE, REFERENCE_CENTROID).getFirst();
        Algebra.multiply4d(v, transformation.getTransformationMatrix(), REFERENCE_CB);
        return v;
    }

    private float distanceSquared3d(float[] vectors, int i, int j) {
        i *= 3;
        j *= 3;
        float dx = vectors[i] - vectors[j];
        float dy = vectors[i + 1] - vectors[j + 1];
        float dz = vectors[i + 2] - vectors[j + 2];

        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Compute the angle between 2 vectors. Must be normal vectors (will not be normalized in this call).
     * @param vectors data
     * @param i index of 1st vector
     * @param j index of 2nd vector
     * @return the angle in degrees
     */
    static float angle(float[] vectors, int i, int j) {
        i *= 3;
        j *= 3;
        // be careful, normally dot product has to be divided by norm of v1 and v2
        float vDot = vectors[i] * vectors[j] + vectors[i + 1] * vectors[j + 1] + vectors[i + 2] * vectors[j + 2];
        return (float) Math.toDegrees(Math.acos(capToInterval(-1, vDot, 1)));
    }
}
