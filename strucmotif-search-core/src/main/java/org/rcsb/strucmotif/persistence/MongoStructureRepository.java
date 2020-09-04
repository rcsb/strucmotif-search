package org.rcsb.strucmotif.persistence;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;

@Service
public class MongoStructureRepository implements StructureRepository {
    /**
     * Key is: pdbId:assemblyId:residueIndex
     * Value is: ["labelAsymId", labelSeqId, "oneLetterCode", "labelAtomId1", 1.x, 1.y, 1.z, "labelAtomId2", 2.x, 2.y, 2.z, ...]
     */
    private final MongoCollection<DBObject> structures;

    @Autowired
    public MongoStructureRepository(MongoClientHolder mongoClientHolder) {
        MongoDatabase database = mongoClientHolder.getDatabase();
        structures = database.getCollection("structures", DBObject.class);
    }

    @Override
    public Structure select(StructureIdentifier structureIdentifier, Collection<IndexSelection> indexSelections) {
        Map<ChainIdentifier, List<Residue>> tmp = new LinkedHashMap<>();

        int aIndex = 0;
        for (IndexSelection indexSelection : indexSelections) {
            String key = structureIdentifier.getPdbId() + ":" + indexSelection.getAssemblyId() + ":" + indexSelection.getIndex();
            BasicDBList res = (BasicDBList) structures.find(eq("_id", key)).first().get("v");
            String chainId = (String) res.get(0);
            int seqId = (int) res.get(1);
            ChainIdentifier chainIdentifier = new ChainIdentifier(chainId, indexSelection.getAssemblyId());
            ResidueType residueType = ResidueType.ofOneLetterCode((String) res.get(2));
            ResidueIdentifier residueIdentifier = new ResidueIdentifier(residueType, seqId, indexSelection.getIndex());
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
        return StructureFactory.createStructure(structureIdentifier, chains);
    }

    @Override
    public void insert(Structure structure) {
        String pdbId = structure.getStructureIdentifier().getPdbId();
        List<DBObject> update = new ArrayList<>();

        for (Chain chain : structure.getChains()) {
            ChainIdentifier chainIdentifier = chain.getChainIdentifier();
            String chainId = chainIdentifier.getLabelAsymId();
            int assemblyId = chainIdentifier.getAssemblyId();

            for (Residue residue : chain.getResidues()) {
                ResidueIdentifier residueIdentifier = residue.getResidueIdentifier();
                int index = residueIdentifier.getIndex();

                Object[] atomData = new Object[3 + residue.getAtoms().size() * 4];
                int pointer = 0;
                atomData[pointer++] = chainId;
                atomData[pointer++] = residueIdentifier.getLabelSeqId();
                atomData[pointer++] = residueIdentifier.getResidueType().getOneLetterCode();

                for (Atom atom : residue.getAtoms()) {
                    AtomIdentifier atomIdentifier = atom.getAtomIdentifier();
                    String labelAtomId = atomIdentifier.getLabelAtomId();
                    double[] coord = atom.getCoord();

                    atomData[pointer++] = labelAtomId;
                    atomData[pointer++] = (int) Math.round(coord[0] * 10);
                    atomData[pointer++] = (int) Math.round(coord[1] * 10);
                    atomData[pointer++] = (int) Math.round(coord[2] * 10);
                }

                update.add(new BasicDBObjectBuilder()
                        .add("_id", pdbId + ":" + assemblyId + ":" + index)
                        .add("v", atomData)
                        .get());
            }
        }

        this.structures.insertMany(update);
    }

    @Override
    public void delete(StructureIdentifier structureIdentifier) {
        // structureIdentifier is at start of all components to remove
        Pattern pattern = Pattern.compile("^" + structureIdentifier.getPdbId());
        structures.deleteMany(Filters.regex("_id", pattern));
    }
}
