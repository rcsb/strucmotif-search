package org.rcsb.strucmotif.domain;

import org.rcsb.strucmotif.domain.structure.Chain;
import org.rcsb.strucmotif.domain.structure.Residue;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.math.Algebra;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Efficiently determine residues in contacts by a spatial hashing approach. Adapted to code to be inline with other
 * classes, removed support for 2nd set of atoms, only consider alpha carbons (and equivalents) for computation.
 *
 * see: https://github.com/biojava/biojava/blob/master/biojava-structure/src/main/java/org/biojava/nbio/structure/contact/Grid.java
 * original author: Jose M. Duarte <jose.duarte@rcsb.org>
 */
public class ResidueGrid {
    /**
     * The scale: we use units of hundredths of Angstroms (thus cutoffs can be specified with a maximum precision of 0.01A)
     */
    private static final int SCALE = 100;
    private final double squaredCutoff;
    private final int cellSize;

    private final Structure structure;
    private final List<Residue> residues;
    private final BoundingBox boundingBox;
    private final int[] intBounds;
    private final ResidueGridCell[][][] gridCells;

    public ResidueGrid(Structure structure, double squaredCutoff) {
        this.squaredCutoff = squaredCutoff;
        this.cellSize = (int) Math.floor(Math.sqrt(squaredCutoff) * SCALE);

        this.structure = structure;
        this.residues = structure.getChains()
                .stream()
                .map(Chain::getResidues)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        this.boundingBox = new BoundingBox(structure);

        this.intBounds = boundingBox.getIntBounds();
        this.gridCells = new ResidueGridCell
                [1 + (intBounds[3] - intBounds[0]) / cellSize]
                [1 + (intBounds[4] - intBounds[1]) / cellSize]
                [1 + (intBounds[5] - intBounds[2]) / cellSize];

        fillGrid();
    }

    /**
     * Creates the grid based on the {@link BoundingBox} defined by all atoms and places the atoms into their
     * corresponding grid cells.
     */
    private void fillGrid() {
        int i = 0;
        for (Chain chain : structure.getChains()) {
            for (Residue residue : chain.getResidues()) {
                assignCoordsToGridCell(residue.getBackboneCoordinates(), i);
                i++;
            }
        }
    }

    private void assignCoordsToGridCell(double[] coords, int i) {
        int xind = xintgrid2xgridindex(getFloor(coords[0]));
        int yind = yintgrid2ygridindex(getFloor(coords[1]));
        int zind = zintgrid2zgridindex(getFloor(coords[2]));
        if (gridCells[xind][yind][zind] == null) {
            gridCells[xind][yind][zind] = new ResidueGridCell();
        }
        this.gridCells[xind][yind][zind].addIndex(i);
    }

    private int xintgrid2xgridindex(int x) {
        return (x - intBounds[0]) / cellSize;
    }

    private int yintgrid2ygridindex(int y) {
        return (y - intBounds[1]) / cellSize;
    }

    private int zintgrid2zgridindex(int z) {
        return (z - intBounds[2]) / cellSize;
    }

    private int getFloor(double number) {
        return cellSize * ((int) Math.floor(number * ResidueGrid.SCALE / cellSize));
    }

    public List<ResidueContact> getIndicesContacts() {
        List<ResidueContact> contacts = new ArrayList<>();

        for (int xind = 0; xind < gridCells.length; xind++) {
            for (int yind = 0; yind < gridCells[xind].length; yind++) {
                for (int zind = 0; zind < gridCells[xind][yind].length; zind++) {
                    // distances of points within this cell
                    ResidueGridCell thisCell = gridCells[xind][yind][zind];
                    if (thisCell == null) {
                        continue;
                    }

                    contacts.addAll(thisCell.getContactsWithinGridCell());

                    // distances of points from this box to all neighbouring boxes: 26 iterations (26 neighbouring boxes)
                    // TODO can these simple direction (i.e. only 0 and +1)
                    for (int x = xind - 1; x <= xind + 1; x++) {
                        for (int y = yind - 1; y <= yind + 1; y++) {
                            for (int z = zind - 1; z <= zind + 1; z++) {
                                if (x == xind && y == yind && z == zind) {
                                    continue;
                                }

                                if (x >= 0 && x < gridCells.length && y >= 0 && y < gridCells[x].length && z >= 0 && z < gridCells[x][y].length) {
                                    if (gridCells[x][y][z] == null) {
                                        continue;
                                    }

                                    contacts.addAll(thisCell.getContactsToGridCell(gridCells[x][y][z]));
                                }
                            }
                        }
                    }
                }
            }
        }

        return contacts;
    }

    public Residue getResidue(int index) {
        return residues.get(index);
    }

    class BoundingBox {
        final double xmin;
        final double xmax;
        final double ymin;
        final double ymax;
        final double zmin;
        final double zmax;

        public BoundingBox(Structure structure) {
            double xmin = Double.MAX_VALUE;
            double xmax = -Double.MAX_VALUE;
            double ymin = Double.MAX_VALUE;
            double ymax = -Double.MAX_VALUE;
            double zmin = Double.MAX_VALUE;
            double zmax = -Double.MAX_VALUE;

            for (Residue residue : residues) {
                double[] coords = residue.getBackboneCoordinates();
                if (coords == null) {
                    System.out.println();
                }
                if (coords[0] > xmax) {
                    xmax = coords[0];
                } else if (coords[0] < xmin) {
                    xmin = coords[0];
                }

                if (coords[1] > ymax) {
                    ymax = coords[1];
                } else if (coords[1] < ymin) {
                    ymin = coords[1];
                }

                if (coords[2] > zmax) {
                    zmax = coords[2];
                } else if (coords[2] < zmin) {
                    zmin = coords[2];
                }
            }

            this.xmin = xmin;
            this.xmax = xmax;
            this.ymin = ymin;
            this.ymax = ymax;
            this.zmin = zmin;
            this.zmax = zmax;
        }

        /**
         * Calculates the member variable bounds:
         * - elements 0, 1, 2: minimum x, y, z
         * - elements 3, 4, 5: maximum x, y, z
         * @return an int[]
         */
        public int[] getIntBounds() {
            return new int[] {
                    getFloor(boundingBox.xmin),
                    getFloor(boundingBox.ymin),
                    getFloor(boundingBox.zmin),
                    getFloor(boundingBox.xmax),
                    getFloor(boundingBox.ymax),
                    getFloor(boundingBox.zmax)
            };
        }
    }

    class ResidueGridCell {
        private final List<Integer> indices;

        public ResidueGridCell() {
            this.indices = new ArrayList<>();
        }

        public void addIndex(int index) {
            indices.add(index);
        }

        public List<ResidueContact> getContactsWithinGridCell() {
            return getContactsToGridCell(this);
        }

        private List<ResidueContact> getContactsToGridCell(ResidueGridCell other) {
            List<ResidueContact> contacts = new ArrayList<>();
            for (int i : indices) {
                for (int j : other.indices) {
                    if (j > i) {
                        double squaredDistance = Algebra.distanceSquared3d(residues.get(i).getBackboneCoordinates(), residues.get(j).getBackboneCoordinates());
                        if (squaredDistance < squaredCutoff) {
                            contacts.add(new ResidueContact(i, j, Math.sqrt(squaredDistance)));
                        }
                    }
                }
            }
            return contacts;
        }
    }

    class ResidueContact {
        private final int i;
        private final int j;
        private final double distance;

        public ResidueContact(int i, int j, double distance) {
            this.i = i;
            this.j = j;
            this.distance = distance;
        }

        public int getI() {
            return i;
        }

        public int getJ() {
            return j;
        }

        public double getDistance() {
            return distance;
        }
    }
}
