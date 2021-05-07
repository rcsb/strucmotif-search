package org.rcsb.strucmotif.math;

import java.util.Collection;

/**
 * A collection of algebraic or generic mathematical functions.
 */
public class Algebra {
    private Algebra() {

    }

    /**
     * Caps a value to a defined interval. Returns lower respectively upper bound for values out of range and otherwise
     * the original value.
     * @param lowerBound the minimum value to accept
     * @param value the value to process
     * @param upperBound the maximum value to accept
     * @return an int in this interval
     */
    public static int capToInterval(int lowerBound, int value, int upperBound) {
        return Math.max(lowerBound, Math.min(value, upperBound));
    }

    /**
     * Caps a value to a defined interval. Returns lower respectively upper bound for values out of range and otherwise
     * the original value.
     * @param lowerBound the minimum value to accept
     * @param value the value to process
     * @param upperBound the maximum value to accept
     * @return a double in this interval
     */
    public static double capToInterval(double lowerBound, double value, double upperBound) {
        return Math.max(lowerBound, Math.min(value, upperBound));
    }

    /**
     * Adds 2 3D vectors. Does not manipulate original vectors.
     * @param vec3d1 first vector
     * @param vec3d2 second vector
     * @return the sum of both vectors
     */
    public static double[] add3d(double[] vec3d1, double[] vec3d2) {
        return new double[] {
                vec3d1[0] + vec3d2[0],
                vec3d1[1] + vec3d2[1],
                vec3d1[2] + vec3d2[2]
        };
    }

    /**
     * Computes the centroid of a collection of 3D vectors.
     * @param vec3ds collection of 3D vectors
     * @return the 3D vector describing the centroid
     */
    public static double[] centroid3d(Collection<double[]> vec3ds) {
        double x = 0;
        double y = 0;
        double z = 0;
        for (double[] vec3d : vec3ds) {
            x += vec3d[0];
            y += vec3d[1];
            z += vec3d[2];
        }
        return new double[] {
                x / vec3ds.size(),
                y / vec3ds.size(),
                z / vec3ds.size()
        };
    }

    /**
     * Subtracts 2 3D vectors. Does not manipulate original vectors.
     * @param vec3d1 first vector
     * @param vec3d2 second vector
     * @return the sum of both vectors
     */
    public static double[] subtract3d(double[] vec3d1, double[] vec3d2) {
        return new double[] {
                vec3d1[0] - vec3d2[0],
                vec3d1[1] - vec3d2[1],
                vec3d1[2] - vec3d2[2]
        };
    }

    /**
     * Left-multiplies a 3D vector with a 3x3 matrix. Does not manipulate original vector/matrix.
     * @param matrix3d the 3x3 matrix
     * @param vec3d the vector
     * @return the resulting vector
     */
    public static double[] multiply3d(double[][] matrix3d, double[] vec3d) {
        double x = vec3d[0];
        double y = vec3d[1];
        double z = vec3d[2];

        double tx = matrix3d[0][0] * x + matrix3d[1][0] * y + matrix3d[2][0] * z;
        double ty = matrix3d[0][1] * x + matrix3d[1][1] * y + matrix3d[2][1] * z;
        double tz = matrix3d[0][2] * x + matrix3d[1][2] * y + matrix3d[2][2] * z;
        return new double[] { tx, ty, tz };
    }


    /**
     * Multiplies a 3D vector with a scalar. Does not manipulate original vector.
     * @param vec3d the vector
     * @param scalar the scalar
     * @return a vector multiplied by the scalar
     */
    public static double[] multiply3d(double[] vec3d, double scalar) {
        return new double[] {
                vec3d[0] * scalar,
                vec3d[1] * scalar,
                vec3d[2] * scalar
        };
    }

    /**
     * Divides a 3D vector by a scalar. Does not manipulate original vector.
     * @param vec3d the vector
     * @param scalar the scalar
     * @return a vector divided by the scalar
     */
    public static double[] divide3d(double[] vec3d, double scalar) {
        return new double[] {
                vec3d[0] / scalar,
                vec3d[1] / scalar,
                vec3d[2] / scalar
        };
    }

    /**
     * Computes the cross-produced of 2 3D vectors. Does not manipulate original vectors.
     * @param vec3d1 first vector
     * @param vec3d2 second vector
     * @return the cross-product of both vectors
     */
    public static double[] crossProduct3d(double[] vec3d1, double[] vec3d2) {
        return new double[] {
                vec3d1[1] * vec3d2[2] - vec3d1[2] * vec3d2[1],
                vec3d1[2] * vec3d2[0] - vec3d1[0] * vec3d2[2],
                vec3d1[0] * vec3d2[1] - vec3d1[1] * vec3d2[0]
        };
    }

    /**
     * Computes the dot-product of 2 3D vectors.
     * @param vec3d1 first vector
     * @param vec3d2 second vector
     * @return the dot-product of both vectors
     */
    public static double dotProduct3d(double[] vec3d1, double[] vec3d2) {
        return vec3d1[0] * vec3d2[0] +
                vec3d1[1] * vec3d2[1] +
                vec3d1[2] * vec3d2[2];
    }

    /**
     * Computes the squared distance between 2 3D vectors. Faster when root is not needed (comparing values et al.).
     * @param vec3d1 first vector
     * @param vec3d2 second vector
     * @return the squared distance between both vectors
     */
    public static double distanceSquared3d(double[] vec3d1, double[] vec3d2) {
        double dx = vec3d1[0] - vec3d2[0];
        double dy = vec3d1[1] - vec3d2[1];
        double dz = vec3d1[2] - vec3d2[2];

        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Computes the distance between 2 3D vectors.
     * @param vec3d1 first vector
     * @param vec3d2 second vector
     * @return the distance between both vectors
     */
    public static double distance3d(double[] vec3d1, double[] vec3d2) {
        return Math.sqrt(distanceSquared3d(vec3d1, vec3d2));
    }

    /**
     * Computes the norm of a vector.
     * @param vec3d the vector
     * @return the norm of the vector
     */
    public static double norm3d(double[] vec3d) {
        return Math.sqrt(dotProduct3d(vec3d, vec3d));
    }

    /**
     * Normalizes a vector (i.e. divide by its norm). Does not manipulate original vector.
     * @param vec3d the vector
     * @return a normalized vector
     */
    public static double[] normalize3d(double[] vec3d) {
        return divide3d(vec3d, norm3d(vec3d));
    }

    /**
     * Left-multiplies a 3D vector with a 4x4 matrix. Does not manipulate original vector/matrix.
     * @param matrix4d the 4x4 matrix
     * @param vec3d the vector
     * @return the resulting vector
     */
    public static double[] multiply4d(double[][] matrix4d, double[] vec3d) {
        double x = vec3d[0];
        double y = vec3d[1];
        double z = vec3d[2];

        double tx = matrix4d[0][0] * x + matrix4d[0][1] * y + matrix4d[0][2] * z + matrix4d[0][3];
        double ty = matrix4d[1][0] * x + matrix4d[1][1] * y + matrix4d[1][2] * z + matrix4d[1][3];
        double tz = matrix4d[2][0] * x + matrix4d[2][1] * y + matrix4d[2][2] * z + matrix4d[2][3];
        return new double[] { tx, ty, tz };
    }

    /**
     * Multiplies 2 4x4 matrices. Does not manipulate original matrices.
     * @param matrix4d1 first matrix
     * @param matrix4d2 second matrix
     * @return the resuling 4x4 matrix
     */
    public static double[][] multiply4d(double[][] matrix4d1, double[][] matrix4d2) {
        return new double[][] {{
            matrix4d1[0][0] * matrix4d2[0][0] + matrix4d1[0][1] * matrix4d2[1][0] +
                    matrix4d1[0][2] * matrix4d2[2][0] * matrix4d1[0][3] * matrix4d2[3][0],
            matrix4d1[0][0] * matrix4d2[0][1] + matrix4d1[0][1] * matrix4d2[1][1] +
                    matrix4d1[0][2] * matrix4d2[2][1] * matrix4d1[0][3] * matrix4d2[3][1],
            matrix4d1[0][0] * matrix4d2[0][2] + matrix4d1[0][1] * matrix4d2[1][2] +
                    matrix4d1[0][2] * matrix4d2[2][2] * matrix4d1[0][3] * matrix4d2[3][2],
            matrix4d1[0][0] * matrix4d2[0][3] + matrix4d1[0][1] * matrix4d2[1][3] +
                    matrix4d1[0][2] * matrix4d2[2][3] * matrix4d1[0][3] * matrix4d2[3][3]
        }, {
            matrix4d1[1][0] * matrix4d2[0][0] + matrix4d1[1][1] * matrix4d2[1][0] +
                    matrix4d1[1][2] * matrix4d2[2][0] * matrix4d1[1][3] * matrix4d2[3][0],
            matrix4d1[1][0] * matrix4d2[0][1] + matrix4d1[1][1] * matrix4d2[1][1] +
                    matrix4d1[1][2] * matrix4d2[2][1] * matrix4d1[1][3] * matrix4d2[3][1],
            matrix4d1[1][0] * matrix4d2[0][2] + matrix4d1[1][1] * matrix4d2[1][2] +
                    matrix4d1[1][2] * matrix4d2[2][2] * matrix4d1[1][3] * matrix4d2[3][2],
            matrix4d1[1][0] * matrix4d2[0][3] + matrix4d1[1][1] * matrix4d2[1][3] +
                    matrix4d1[1][2] * matrix4d2[2][3] * matrix4d1[1][3] * matrix4d2[3][3]
        }, {
            matrix4d1[2][0] * matrix4d2[0][0] + matrix4d1[2][1] * matrix4d2[1][0] +
                    matrix4d1[2][2] * matrix4d2[2][0] * matrix4d1[2][3] * matrix4d2[3][0],
            matrix4d1[2][0] * matrix4d2[0][1] + matrix4d1[2][1] * matrix4d2[1][1] +
                    matrix4d1[2][2] * matrix4d2[2][1] * matrix4d1[2][3] * matrix4d2[3][1],
            matrix4d1[2][0] * matrix4d2[0][2] + matrix4d1[2][1] * matrix4d2[1][2] +
                    matrix4d1[2][2] * matrix4d2[2][2] * matrix4d1[2][3] * matrix4d2[3][2],
            matrix4d1[2][0] * matrix4d2[0][3] + matrix4d1[2][1] * matrix4d2[1][3] +
                    matrix4d1[2][2] * matrix4d2[2][3] * matrix4d1[2][3] * matrix4d2[3][3]
        }, {
            matrix4d1[3][0] * matrix4d2[0][0] + matrix4d1[3][1] * matrix4d2[1][0] +
                    matrix4d1[3][2] * matrix4d2[2][0] * matrix4d1[3][3] * matrix4d2[3][0],
            matrix4d1[3][0] * matrix4d2[0][1] + matrix4d1[3][1] * matrix4d2[1][1] +
                    matrix4d1[3][2] * matrix4d2[2][1] * matrix4d1[3][3] * matrix4d2[3][1],
            matrix4d1[3][0] * matrix4d2[0][2] + matrix4d1[3][1] * matrix4d2[1][2] +
                    matrix4d1[3][2] * matrix4d2[2][2] * matrix4d1[3][3] * matrix4d2[3][2],
            matrix4d1[3][0] * matrix4d2[0][3] + matrix4d1[3][1] * matrix4d2[1][3] +
                    matrix4d1[3][2] * matrix4d2[2][3] * matrix4d1[3][3] * matrix4d2[3][3]
        }};
    }

    /**
     * Transpose a 3x3 matrix.
     * @param rot the original matrix
     * @return a new, transposed matrix
     */
    public static double[][] transpose3d(double[][] rot) {
        return new double[][] {
                { rot[0][0], rot[1][0], rot[2][0] },
                { rot[0][1], rot[1][1], rot[2][1] },
                { rot[0][2], rot[1][2], rot[2][2] }
        };
    }

    /**
     * Combine a rotation matrix and a translation vector into a 4x4 transformation matrix.
     * @param rotation3x3 a 3x3 rotation matrix
     * @param translation3d a 3-element translation vector
     * @return a transformation matrix
     */
    public static double[][] composeTransformationMatrix(double[][] rotation3x3, double[] translation3d) {
        double[][] matrix = new double[4][];

        for (int i = 0; i < 3; i++) {
            matrix[i] = new double[4];
            System.arraycopy(rotation3x3[i], 0, matrix[i], 0, 3);
            matrix[i][3] = translation3d[i];
        }
        matrix[3] = new double[] { 0, 0, 0, 1 };

        return matrix;
    }
}
