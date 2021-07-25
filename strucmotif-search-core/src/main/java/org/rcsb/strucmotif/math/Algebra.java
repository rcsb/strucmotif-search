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
     * @return a float in this interval
     */
    public static float capToInterval(float lowerBound, float value, float upperBound) {
        return Math.max(lowerBound, Math.min(value, upperBound));
    }

    /**
     * Adds 2 3D vectors.
     * @param out the output
     * @param v1 first vector
     * @param v2 second vector
     */
    public static void add3d(float[] out, float[] v1, float[] v2) {
        out[0] = v1[0] + v2[0];
        out[1] = v1[1] + v2[1];
        out[2] = v1[2] + v2[2];
    }

    /**
     * Computes the centroid of a collection of 3D vectors.
     * @param vs collection of 3D vectors
     * @return the 3D vector describing the centroid
     */
    public static float[] centroid3d(Collection<float[]> vs) {
        float x = 0;
        float y = 0;
        float z = 0;
        for (float[] v : vs) {
            x += v[0];
            y += v[1];
            z += v[2];
        }
        return new float[] {
                x / vs.size(),
                y / vs.size(),
                z / vs.size()
        };
    }

    /**
     * Subtracts 2 3D vectors.
     * @param out the output
     * @param v1 first vector
     * @param v2 second vector
     */
    public static void subtract3d(float[] out, float[] v1, float[] v2) {
        out[0] = v1[0] - v2[0];
        out[1] = v1[1] - v2[1];
        out[2] = v1[2] - v2[2];
    }

    /**
     * Left-multiplies a 3D vector with a 3x3 matrix.
     * @param out the output
     * @param m the 3x3 matrix
     * @param v the vector
     */
    public static void multiply3d(float[] out, float[][] m, float[] v) {
        float x = v[0];
        float y = v[1];
        float z = v[2];

        out[0] = m[0][0] * x + m[1][0] * y + m[2][0] * z;
        out[1] = m[0][1] * x + m[1][1] * y + m[2][1] * z;
        out[2] = m[0][2] * x + m[1][2] * y + m[2][2] * z;
    }


    /**
     * Multiplies a 3D vector with a scalar.
     * @param out the output
     * @param v the vector
     * @param scalar the scalar
     */
    public static void multiply3d(float[] out, float[] v, float scalar) {
        out[0] = v[0] * scalar;
        out[1] = v[1] * scalar;
        out[2] = v[2] * scalar;
    }

    /**
     * Divides a 3D vector by a scalar.
     * @param out the output
     * @param v the vector
     * @param scalar the scalar
     */
    public static void divide3d(float[] out, float[] v, float scalar) {
        out[0] = v[0] / scalar;
        out[1] = v[1] / scalar;
        out[2] = v[2] / scalar;
    }

    /**
     * Computes the cross-produced of 2 3D vectors.
     * @param out the output
     * @param v1 first vector
     * @param v2 second vector
     */
    public static void crossProduct3d(float[] out, float[] v1, float[] v2) {
        float x1 = v1[0];
        float y1 = v1[1];
        float z1 = v1[2];
        out[0] = y1 * v2[2] - z1 * v2[1];
        out[1] = z1 * v2[0] - x1 * v2[2];
        out[2] = x1 * v2[1] - y1 * v2[0];
    }

    /**
     * Computes the dot-product of 2 3D vectors.
     * @param v1 first vector
     * @param v2 second vector
     * @return the dot-product of both vectors
     */
    public static float dotProduct3d(float[] v1, float[] v2) {
        return v1[0] * v2[0] +
                v1[1] * v2[1] +
                v1[2] * v2[2];
    }

    /**
     * Computes the squared distance between 2 3D vectors. Faster when root is not needed (comparing values et al.).
     * @param v1 first vector
     * @param v2 second vector
     * @return the squared distance between both vectors
     */
    public static float distanceSquared3d(float[] v1, float[] v2) {
        float dx = v1[0] - v2[0];
        float dy = v1[1] - v2[1];
        float dz = v1[2] - v2[2];

        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Computes the distance between 2 3D vectors.
     * @param v1 first vector
     * @param v2 second vector
     * @return the distance between both vectors
     */
    public static float distance3d(float[] v1, float[] v2) {
        return (float) Math.sqrt(distanceSquared3d(v1, v2));
    }

    /**
     * Computes the norm of a vector.
     * @param vec3d the vector
     * @return the norm of the vector
     */
    public static float norm3d(float[] vec3d) {
        return (float) Math.sqrt(dotProduct3d(vec3d, vec3d));
    }

    /**
     * Normalizes a vector (i.e. divide by its norm).
     * @param out the output
     * @param v the vector
     */
    public static void normalize3d(float[] out, float[] v) {
        divide3d(out, v, norm3d(v));
    }

    /**
     * Left-multiplies a 3D vector with a 4x4 matrix.
     * @param out the output
     * @param m the 4x4 matrix
     * @param v the vector
     */
    public static void multiply4d(float[] out, float[][] m, float[] v) {
        float x = v[0];
        float y = v[1];
        float z = v[2];

        out[0] = m[0][0] * x + m[0][1] * y + m[0][2] * z + m[0][3];
        out[1] = m[1][0] * x + m[1][1] * y + m[1][2] * z + m[1][3];
        out[2] = m[2][0] * x + m[2][1] * y + m[2][2] * z + m[2][3];
    }

    /**
     * Multiplies 2 4x4 matrices. Does not manipulate original matrices.
     * @param matrix4d1 first matrix
     * @param matrix4d2 second matrix
     * @return the resulting 4x4 matrix
     */
    public static float[][] multiply4d(float[][] matrix4d1, float[][] matrix4d2) {
        return new float[][] {{
            matrix4d1[0][0] * matrix4d2[0][0] + matrix4d1[0][1] * matrix4d2[1][0] +
                    matrix4d1[0][2] * matrix4d2[2][0] + matrix4d1[0][3] * matrix4d2[3][0],
            matrix4d1[0][0] * matrix4d2[0][1] + matrix4d1[0][1] * matrix4d2[1][1] +
                    matrix4d1[0][2] * matrix4d2[2][1] + matrix4d1[0][3] * matrix4d2[3][1],
            matrix4d1[0][0] * matrix4d2[0][2] + matrix4d1[0][1] * matrix4d2[1][2] +
                    matrix4d1[0][2] * matrix4d2[2][2] + matrix4d1[0][3] * matrix4d2[3][2],
            matrix4d1[0][0] * matrix4d2[0][3] + matrix4d1[0][1] * matrix4d2[1][3] +
                    matrix4d1[0][2] * matrix4d2[2][3] + matrix4d1[0][3] * matrix4d2[3][3]
        }, {
            matrix4d1[1][0] * matrix4d2[0][0] + matrix4d1[1][1] * matrix4d2[1][0] +
                    matrix4d1[1][2] * matrix4d2[2][0] + matrix4d1[1][3] * matrix4d2[3][0],
            matrix4d1[1][0] * matrix4d2[0][1] + matrix4d1[1][1] * matrix4d2[1][1] +
                    matrix4d1[1][2] * matrix4d2[2][1] + matrix4d1[1][3] * matrix4d2[3][1],
            matrix4d1[1][0] * matrix4d2[0][2] + matrix4d1[1][1] * matrix4d2[1][2] +
                    matrix4d1[1][2] * matrix4d2[2][2] + matrix4d1[1][3] * matrix4d2[3][2],
            matrix4d1[1][0] * matrix4d2[0][3] + matrix4d1[1][1] * matrix4d2[1][3] +
                    matrix4d1[1][2] * matrix4d2[2][3] + matrix4d1[1][3] * matrix4d2[3][3]
        }, {
            matrix4d1[2][0] * matrix4d2[0][0] + matrix4d1[2][1] * matrix4d2[1][0] +
                    matrix4d1[2][2] * matrix4d2[2][0] + matrix4d1[2][3] * matrix4d2[3][0],
            matrix4d1[2][0] * matrix4d2[0][1] + matrix4d1[2][1] * matrix4d2[1][1] +
                    matrix4d1[2][2] * matrix4d2[2][1] + matrix4d1[2][3] * matrix4d2[3][1],
            matrix4d1[2][0] * matrix4d2[0][2] + matrix4d1[2][1] * matrix4d2[1][2] +
                    matrix4d1[2][2] * matrix4d2[2][2] + matrix4d1[2][3] * matrix4d2[3][2],
            matrix4d1[2][0] * matrix4d2[0][3] + matrix4d1[2][1] * matrix4d2[1][3] +
                    matrix4d1[2][2] * matrix4d2[2][3] + matrix4d1[2][3] * matrix4d2[3][3]
        }, {
            matrix4d1[3][0] * matrix4d2[0][0] + matrix4d1[3][1] * matrix4d2[1][0] +
                    matrix4d1[3][2] * matrix4d2[2][0] + matrix4d1[3][3] * matrix4d2[3][0],
            matrix4d1[3][0] * matrix4d2[0][1] + matrix4d1[3][1] * matrix4d2[1][1] +
                    matrix4d1[3][2] * matrix4d2[2][1] + matrix4d1[3][3] * matrix4d2[3][1],
            matrix4d1[3][0] * matrix4d2[0][2] + matrix4d1[3][1] * matrix4d2[1][2] +
                    matrix4d1[3][2] * matrix4d2[2][2] + matrix4d1[3][3] * matrix4d2[3][2],
            matrix4d1[3][0] * matrix4d2[0][3] + matrix4d1[3][1] * matrix4d2[1][3] +
                    matrix4d1[3][2] * matrix4d2[2][3] + matrix4d1[3][3] * matrix4d2[3][3]
        }};
    }

    /**
     * Transpose a 3x3 matrix.
     * @param rot the original matrix
     * @return a new, transposed matrix
     */
    public static float[][] transpose3d(float[][] rot) {
        return new float[][] {
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
    public static float[][] composeTransformationMatrix(float[][] rotation3x3, float[] translation3d) {
        float[][] matrix = new float[4][];

        for (int i = 0; i < 3; i++) {
            matrix[i] = new float[4];
            System.arraycopy(rotation3x3[i], 0, matrix[i], 0, 3);
            matrix[i][3] = translation3d[i];
        }
        matrix[3] = new float[] { 0, 0, 0, 1 };

        return matrix;
    }
}
