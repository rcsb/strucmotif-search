package org.rcsb.strucmotif.io.read;

import com.google.common.base.CharMatcher;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.BasicDBList;
import org.rcsb.strucmotif.domain.Matrix4DTransformation;
import org.rcsb.strucmotif.domain.identifier.AtomIdentifier;
import org.rcsb.strucmotif.domain.identifier.ChainIdentifier;
import org.rcsb.strucmotif.domain.identifier.ResidueIdentifier;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.selection.IndexSelection;
import org.rcsb.strucmotif.domain.structure.Atom;
import org.rcsb.strucmotif.domain.structure.Chain;
import org.rcsb.strucmotif.domain.structure.Residue;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.domain.structure.StructureFactory;
import org.rcsb.strucmotif.persistence.MongoResidueDB;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.CharMatcher.inRange;

@Singleton
public class MongoDBSelectionReaderImpl implements SelectionReader {
    private static final CharMatcher LETTER_MATCHER = inRange('a', 'z').or(inRange('A', 'Z'));
    private static final CharMatcher DIGIT_MATCHER = inRange('0', '9');
    private final MongoResidueDB residueDB;

    @Inject
    public MongoDBSelectionReaderImpl(MongoResidueDB residueDB) {
        this.residueDB = residueDB;
    }

    @Override
    public Structure readById(String pdbId, Collection<IndexSelection> selection) {
        String title = residueDB.selectTitle(pdbId);
        Map<ChainIdentifier, List<Residue>> tmp = new LinkedHashMap<>();

        int aIndex = 0;
        for (IndexSelection indexSelection : selection) {
            int index = indexSelection.getIndex();
            BasicDBList res = residueDB.selectResidue(pdbId, indexSelection.getAssemblyId(), index);
            String chainId = (String) res.get(0);
            String raw = (String) res.get(1);
            ChainIdentifier chainIdentifier = new ChainIdentifier("?", chainId, indexSelection.getAssemblyId());
            ResidueType residueType = ResidueType.ofOneLetterCode((String) res.get(2));
            ResidueIdentifier residueIdentifier = new ResidueIdentifier(residueType, Integer.parseInt(DIGIT_MATCHER.retainFrom(raw)), LETTER_MATCHER.retainFrom(raw), index);
            List<Atom> atoms = new ArrayList<>((int) Math.round((res.size() - 3) * 0.25));
            for (int i = 3; i < res.size(); i = i + 4) {
                String name = (String) res.get(i);
                double[] coord = new double[] {
                        ((int) res.get(i + 1)) * 0.1,
                        ((int) res.get(i + 2)) * 0.1,
                        ((int) res.get(i + 3)) * 0.1
                };
                atoms.add(StructureFactory.createAtom(new AtomIdentifier(name, ++aIndex), coord));
            }
            Residue residue = StructureFactory.createResidue(residueIdentifier, atoms, Matrix4DTransformation.IDENTITY_MATRIX_4D);
            tmp.computeIfAbsent(chainIdentifier, c -> new ArrayList<>()).add(residue);
        }

        List<Chain> chains = tmp.entrySet()
                .stream()
                .map(entry -> StructureFactory.createChain(entry.getKey(), entry.getValue(), Matrix4DTransformation.IDENTITY_MATRIX_4D))
                .collect(Collectors.toList());
        return StructureFactory.createStructure(new StructureIdentifier(pdbId), title, chains);
    }

    @Override
    public Structure readFromInputStream(InputStream inputStream) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Structure readFromInputStream(InputStream inputStream, Collection<IndexSelection> selection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Structure readById(String pdbId) {
        throw new UnsupportedOperationException();
    }
}
