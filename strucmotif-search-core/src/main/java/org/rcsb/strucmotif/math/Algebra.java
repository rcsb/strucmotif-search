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
    public static void multiply3d(float[] out, float[] m, float[] v) {
        float x = v[0];
        float y = v[1];
        float z = v[2];

        out[0] = m[0] * x + m[3] * y + m[6] * z;
        out[1] = m[1] * x + m[4] * y + m[7] * z;
        out[2] = m[2] * x + m[5] * y + m[8] * z;
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
     * Left-multiplies a 3D vector with a flat 4x4 matrix.
     * @param out the output
     * @param m the flat 4x4 matrix
     * @param v the vector
     */
    public static void multiply4d(float[] out, float[] m, float[] v) {
        float x = v[0];
        float y = v[1];
        float z = v[2];

        out[0] = m[0] * x + m[1] * y + m[2] * z + m[3];
        out[1] = m[4] * x + m[5] * y + m[6] * z + m[7];
        out[2] = m[8] * x + m[9] * y + m[10] * z + m[11];
    }

    /**
     * Multiplies 2 flat 4x4 matrices. Does not manipulate original matrices.
     * @param a first matrix
     * @param b second matrix
     * @return the resulting flat 4x4 matrix
     */
    public static float[] multiply4d(float[] a, float[] b) {
        float a00 = a[0], a01 = a[1], a02 = a[2], a03 = a[3],
                a10 = a[4], a11 = a[5], a12 = a[6], a13 = a[7],
                a20 = a[8], a21 = a[9], a22 = a[10], a23 = a[11],
                a30 = a[12], a31 = a[13], a32 = a[14], a33 = a[15];
        float[] out = new float[16];

        // Cache only the current line of the second matrix
        float b0 = b[0], b1 = b[1], b2 = b[2], b3 = b[3];
        out[0] = b0 * a00 + b1 * a10 + b2 * a20 + b3 * a30;
        out[1] = b0 * a01 + b1 * a11 + b2 * a21 + b3 * a31;
        out[2] = b0 * a02 + b1 * a12 + b2 * a22 + b3 * a32;
        out[3] = b0 * a03 + b1 * a13 + b2 * a23 + b3 * a33;

        b0 = b[4]; b1 = b[5]; b2 = b[6]; b3 = b[7];
        out[4] = b0 * a00 + b1 * a10 + b2 * a20 + b3 * a30;
        out[5] = b0 * a01 + b1 * a11 + b2 * a21 + b3 * a31;
        out[6] = b0 * a02 + b1 * a12 + b2 * a22 + b3 * a32;
        out[7] = b0 * a03 + b1 * a13 + b2 * a23 + b3 * a33;

        b0 = b[8]; b1 = b[9]; b2 = b[10]; b3 = b[11];
        out[8] = b0 * a00 + b1 * a10 + b2 * a20 + b3 * a30;
        out[9] = b0 * a01 + b1 * a11 + b2 * a21 + b3 * a31;
        out[10] = b0 * a02 + b1 * a12 + b2 * a22 + b3 * a32;
        out[11] = b0 * a03 + b1 * a13 + b2 * a23 + b3 * a33;

        b0 = b[12]; b1 = b[13]; b2 = b[14]; b3 = b[15];
        out[12] = b0 * a00 + b1 * a10 + b2 * a20 + b3 * a30;
        out[13] = b0 * a01 + b1 * a11 + b2 * a21 + b3 * a31;
        out[14] = b0 * a02 + b1 * a12 + b2 * a22 + b3 * a32;
        out[15] = b0 * a03 + b1 * a13 + b2 * a23 + b3 * a33;

        return out;
    }

    /**
     * Transpose a 3x3 matrix.
     * @param rot the original matrix
     * @return a new, transposed matrix
     */
    public static float[] transpose3d(float[] rot) {
        return new float[] {
                rot[0], rot[3], rot[6],
                rot[1], rot[4], rot[7],
                rot[2], rot[5], rot[8]
        };
    }

    /**
     * Combine a rotation matrix and a translation vector into a 4x4 transformation matrix.
     * @param rotation9 a flat rotation matrix
     * @param translation3 a 3-element translation vector
     * @return a transformation matrix
     */
    public static float[] composeTransformationMatrix(float[] rotation9, float[] translation3) {
        return new float[] {
                rotation9[0], rotation9[1], rotation9[2], translation3[0],
                rotation9[3], rotation9[4], rotation9[5], translation3[1],
                rotation9[6], rotation9[7], rotation9[8], translation3[2],
                0, 0, 0, 1
        };
    }
}
