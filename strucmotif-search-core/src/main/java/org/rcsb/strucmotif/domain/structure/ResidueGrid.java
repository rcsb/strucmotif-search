package org.rcsb.strucmotif.domain.structure;

import java.util.ArrayList;
import java.util.List;

/**
 * Efficiently determine residues in contacts by a spatial hashing approach. Adapted to code to be inline with other
 * classes, removed support for 2nd set of atoms, only consider alpha carbons (and equivalents) for computation.
 * <p>
 * see: <a href="https://github.com/biojava/biojava/blob/master/biojava-structure/src/main/java/org/biojava/nbio/structure/contact/Grid.java">Grid.java</a>
 * original author: Jose M. Duarte &lt;jose.duarte@rcsb.org&gt;
 */
public class ResidueGrid {
    /**
     * The scale: we use units of hundredths of Angstroms (thus cutoffs can be specified with a maximum precision of 0.01A)
     */
    private static final int SCALE = 100;
    private final float squaredCutoff;
    private final int cellSize;

    private final ResidueGraph.ResidueVectors residueVectors;
    private final ResidueGrid.BoundingBox boundingBox;
    private final int[] intBounds;
    private final ResidueGrid.ResidueGridCell[][][] gridCells;

    /**
     * Construct a residue grid from a structure.
     * @param residueVectors the data
     * @param squaredCutoff maximum distance between residues
     */
    public ResidueGrid(ResidueGraph.ResidueVectors residueVectors, float squaredCutoff) {
        this.residueVectors = residueVectors;
        this.squaredCutoff = squaredCutoff;
        this.cellSize = (int) Math.floor(Math.sqrt(squaredCutoff) * SCALE);

        this.boundingBox = new ResidueGrid.BoundingBox();

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
        for (int i = 0; i < residueVectors.backboneVectors().length - 2; i = i + 3) {
            assignCoordsToGridCell(residueVectors.backboneVectors(), i);
        }
    }

    private void assignCoordsToGridCell(float[] coords, int i) {
        int xind = xintgrid2xgridindex(getFloor(coords[i]));
        int yind = yintgrid2ygridindex(getFloor(coords[i + 1]));
        int zind = zintgrid2zgridindex(getFloor(coords[i + 2]));
        if (gridCells[xind][yind][zind] == null) {
            gridCells[xind][yind][zind] = new ResidueGridCell();
        }
        this.gridCells[xind][yind][zind].addIndex(i / 3);
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

    private int getFloor(float number) {
        return cellSize * ((int) Math.floor(number * ResidueGrid.SCALE / cellSize));
    }

    /**
     * All contacts registered.
     * @return a collection of contacts
     */
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

    class BoundingBox {
        final float xmin;
        final float xmax;
        final float ymin;
        final float ymax;
        final float zmin;
        final float zmax;

        public BoundingBox() {
            float xmin = Float.MAX_VALUE;
            float xmax = -Float.MAX_VALUE;
            float ymin = Float.MAX_VALUE;
            float ymax = -Float.MAX_VALUE;
            float zmin = Float.MAX_VALUE;
            float zmax = -Float.MAX_VALUE;

            float[] v = residueVectors.backboneVectors();
            for (int i = 0; i < v.length - 2; i = i + 3) {
                if (v[i] > xmax) {
                    xmax = v[i];
                }
                if (v[i] < xmin) {
                    xmin = v[i];
                }

                if (v[i+ 1] > ymax) {
                    ymax = v[i + 1];
                }
                if (v[i+ 1] < ymin) {
                    ymin = v[i + 1];
                }

                if (v[i + 2] > zmax) {
                    zmax = v[i + 2];
                }
                if (v[i + 2] < zmin) {
                    zmin = v[i + 2];
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
            float[] backboneVectors = residueVectors.backboneVectors();
            for (int i : indices) {
                for (int j : other.indices) {
                    if (j > i) {
                        float squaredDistance = ResidueGraph.distanceSquared3d(backboneVectors, i, j);
                        if (squaredDistance < squaredCutoff) {
                            contacts.add(new ResidueContact(i, j, (float) Math.sqrt(squaredDistance)));
                        }
                    }
                }
            }
            return contacts;
        }
    }

    public record ResidueContact(int i, int j, float distance) {}
}
