package org.rcsb.strucmotif2.align;

import org.rcsb.strucmotif2.domain.AlignmentResult;
import org.rcsb.strucmotif2.domain.AlignmentResultImpl;
import org.rcsb.strucmotif2.domain.AtomCorrespondence;
import org.rcsb.strucmotif2.domain.AtomPairingScheme;
import org.rcsb.strucmotif2.domain.Pair;
import org.rcsb.strucmotif2.domain.Transformation;
import org.rcsb.strucmotif2.domain.score.RootMeanSquareDeviation;
import org.rcsb.strucmotif2.domain.structure.Residue;
import org.rcsb.strucmotif2.math.Algebra;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Align 2 sets of residues by a quaternion-based characteristic polynomial. Finds a rigid transformation that will move
 * the 2nd argument.
 */
@Service
public class QuaternionAlignmentService implements AlignmentService {
    private static final double[][] IDENTITY_MATRIX_3D = new double[][] {
            { 1, 0, 0 },
            { 0, 1, 0 },
            { 0, 0, 1 }
    };

    @Override
    public AlignmentResult align(List<Residue> reference, List<Residue> candidate, AtomPairingScheme atomPairingScheme) {
        // validate parameters
        if (reference.size() != candidate.size()) {
            throw new IllegalArgumentException("cannot align containers of unequal size - " + reference.size()
                    + " vs " + candidate.size() + " : " + reference + " vs " + candidate);
        }
        Objects.requireNonNull(atomPairingScheme, "alignment scheme cannot be null");


        // find compatible combinations between reference and candidate atoms
        AtomCorrespondence atomCorrespondence = new AtomCorrespondence(reference, candidate, atomPairingScheme);
        return align(atomCorrespondence);
    }

    /**
     * Aligns 2 lists of 3D vectors by quaternion-based characteristic polynomial. Both lists of reference and candidate
     * points are expected to be equal of size. Furthermore, centroids have to be computed externally and points must be
     * centered.
     *
     * <p>base on code from: https://theobald.brandeis.edu/qcp/qcprot.c
     *
     * <p>Douglas L. Theobald (2005)
     * "Rapid calculation of RMSD using a quaternion-based characteristic
     * polynomial."
     * Acta Crystallographica A 61(4):478-480.
     *
     * <p>Pu Liu, Dmitris K. Agrafiotis, and Douglas L. Theobald (2009)
     * "Fast determination of the optimal rotational matrix for macromolecular
     * superpositions."
     * Journal of Computational Chemistry 31(7):1561-1563.
     *
     * <p>Copyright (c) 2009-2016 Pu Liu and Douglas L. Theobald
     * All rights reserved.
     *
     * <p>Redistribution and use in source and binary forms, with or without modification, are permitted
     * provided that the following conditions are met:
     * <ul>
     * <li>Redistributions of source code must retain the above copyright notice, this list of
     *   conditions and the following disclaimer.</li>
     * <li>Redistributions in binary form must reproduce the above copyright notice, this list
     *   of conditions and the following disclaimer in the documentation and/or other materials
     *   provided with the distribution.</li>
     * <li>Neither the name of the <ORGANIZATION> nor the names of its contributors may be used to
     *   endorse or promote products derived from this software without specific prior written
     *   permission.</li>
     * </ul>
     *
     * <p>THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
     * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
     * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
     * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
     * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
     * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
     * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
     * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
     * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
     * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
     * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
     *
     * @param atomCorrespondence the paired/mapped atoms
     * @return an object describing the transformation
     */
    private AlignmentResult align(AtomCorrespondence atomCorrespondence) {
        List<double[]> referencePoints = atomCorrespondence.getCenteredReference();
        double[] referenceCentroid = atomCorrespondence.getReferenceCentroid();
        List<double[]> candidatePoints = atomCorrespondence.getCenteredCandidate();
        double[] candidateCentroid = atomCorrespondence.getCandidateCentroid();

        Pair<Transformation, Double> alignment = align(referencePoints, referenceCentroid, candidatePoints, candidateCentroid);

        return new AlignmentResultImpl(atomCorrespondence.getOriginalReference(),
                atomCorrespondence.getOriginalCandidate(),
                alignment.getFirst(),
                new RootMeanSquareDeviation(alignment.getSecond()));
    }

    /**
     * Align two set of residues. Must have equal size.
     * @param referencePoints set of reference points
     * @param referenceCentroid the centroid of reference points
     * @param candidatePoints set of candidate points
     * @param candidateCentroid the centroid of candidate points
     * @return pair of transformation and RMSD
     */
    @SuppressWarnings("Duplicates")
    public static Pair<Transformation, Double> align(List<double[]> referencePoints, double[] referenceCentroid, List<double[]> candidatePoints, double[] candidateCentroid) {
        double[][] rot = new double[3][3];

        // inner product
        double G1 = 0.0;
        double G2 = 0.0;
        double[] A = new double[9];

        for (int i = 0; i < referencePoints.size(); i++) {
            double[] r = referencePoints.get(i);
            double[] c = candidatePoints.get(i);

            double x1 = r[0];
            double y1 = r[1];
            double z1 = r[2];
            G1 += x1 * x1 + y1 * y1 + z1 * z1;

            double x2 = c[0];
            double y2 = c[1];
            double z2 = c[2];
            G2 += (x2 * x2 + y2 * y2 + z2 * z2);

            A[0] +=  (x1 * x2);
            A[1] +=  (x1 * y2);
            A[2] +=  (x1 * z2);

            A[3] +=  (y1 * x2);
            A[4] +=  (y1 * y2);
            A[5] +=  (y1 * z2);

            A[6] +=  (z1 * x2);
            A[7] +=  (z1 * y2);
            A[8] +=  (z1 * z2);
        }
        double E0 = (G1 + G2) * 0.5;

        // fast calc RMSD and rotation
        double Sxx, Sxy, Sxz, Syx, Syy, Syz, Szx, Szy, Szz;
        double Szz2, Syy2, Sxx2, Sxy2, Syz2, Sxz2, Syx2, Szy2, Szx2,
                SyzSzymSyySzz2, Sxx2Syy2Szz2Syz2Szy2, Sxy2Sxz2Syx2Szx2,
                SxzpSzx, SyzpSzy, SxypSyx, SyzmSzy,
                SxzmSzx, SxymSyx, SxxpSyy, SxxmSyy;
        double[] C = new double[4];
        int i;
        double mxEigenV;
        double oldg;
        double b, a, delta, rms, qsqr;
        double q1, q2, q3, q4, normq;
        double a11, a12, a13, a14, a21, a22, a23, a24;
        double a31, a32, a33, a34, a41, a42, a43, a44;
        double a2, x2, y2, z2;
        double xy, az, zx, ay, yz, ax;
        double a3344_4334, a3244_4234, a3243_4233, a3143_4133,a3144_4134, a3142_4132;
        double evecprec = 1e-6;
        double evalprec = 1e-11;

        Sxx = A[0]; Sxy = A[1]; Sxz = A[2];
        Syx = A[3]; Syy = A[4]; Syz = A[5];
        Szx = A[6]; Szy = A[7]; Szz = A[8];

        Sxx2 = Sxx * Sxx;
        Syy2 = Syy * Syy;
        Szz2 = Szz * Szz;

        Sxy2 = Sxy * Sxy;
        Syz2 = Syz * Syz;
        Sxz2 = Sxz * Sxz;

        Syx2 = Syx * Syx;
        Szy2 = Szy * Szy;
        Szx2 = Szx * Szx;

        SyzSzymSyySzz2 = 2.0 * (Syz * Szy - Syy * Szz);
        Sxx2Syy2Szz2Syz2Szy2 = Syy2 + Szz2 - Sxx2 + Syz2 + Szy2;

        C[2] = -2.0 * (Sxx2 + Syy2 + Szz2 + Sxy2 + Syx2 + Sxz2 + Szx2 + Syz2 + Szy2);
        C[1] = 8.0 * (Sxx * Syz * Szy + Syy * Szx * Sxz + Szz * Sxy * Syx - Sxx * Syy * Szz - Syz * Szx * Sxy - Szy * Syx * Sxz);

        SxzpSzx = Sxz + Szx;
        SyzpSzy = Syz + Szy;
        SxypSyx = Sxy + Syx;
        SyzmSzy = Syz - Szy;
        SxzmSzx = Sxz - Szx;
        SxymSyx = Sxy - Syx;
        SxxpSyy = Sxx + Syy;
        SxxmSyy = Sxx - Syy;
        Sxy2Sxz2Syx2Szx2 = Sxy2 + Sxz2 - Syx2 - Szx2;

        C[0] = Sxy2Sxz2Syx2Szx2 * Sxy2Sxz2Syx2Szx2
                + (Sxx2Syy2Szz2Syz2Szy2 + SyzSzymSyySzz2) * (Sxx2Syy2Szz2Syz2Szy2 - SyzSzymSyySzz2)
                + (-(SxzpSzx) * (SyzmSzy) + (SxymSyx) * (SxxmSyy - Szz)) * (-(SxzmSzx) * (SyzpSzy) + (SxymSyx) * (SxxmSyy + Szz))
                + (-(SxzpSzx) * (SyzpSzy) - (SxypSyx) * (SxxpSyy - Szz)) * (-(SxzmSzx) * (SyzmSzy) - (SxypSyx) * (SxxpSyy + Szz))
                + (+(SxypSyx) * (SyzpSzy) + (SxzpSzx) * (SxxmSyy + Szz)) * (-(SxymSyx) * (SyzmSzy) + (SxzpSzx) * (SxxpSyy + Szz))
                + (+(SxypSyx) * (SyzmSzy) + (SxzmSzx) * (SxxmSyy - Szz)) * (-(SxymSyx) * (SyzpSzy) + (SxzmSzx) * (SxxpSyy - Szz));

        /* Newton-Raphson */
        mxEigenV = E0;
        for (i = 0; i < 50; ++i) {
            oldg = mxEigenV;
            x2 = mxEigenV * mxEigenV;
            b = (x2 + C[2]) * mxEigenV;
            a = b + C[1];
            delta = ((a * mxEigenV + C[0]) / (2.0 * x2 * mxEigenV + b + a));
            mxEigenV -= delta;
            if (Math.abs(mxEigenV - oldg) < Math.abs(evalprec * mxEigenV)) {
                break;
            }
        }

        /* the abs() is to guard against extremely small, but *negative* numbers due to floating point error */
        rms = Math.sqrt(Math.abs(2.0 * (E0 - mxEigenV) / referencePoints.size()));

        a11 = SxxpSyy + Szz - mxEigenV;
        a12 = SyzmSzy;
        a13 = -SxzmSzx;
        a14 = SxymSyx;
        a21 = SyzmSzy;
        a22 = SxxmSyy - Szz - mxEigenV;
        a23 = SxypSyx;
        a24 = SxzpSzx;
        a31 = a13;
        a32 = a23;
        a33 = Syy - Sxx - Szz - mxEigenV;
        a34 = SyzpSzy;
        a41 = a14;
        a42 = a24;
        a43 = a34;
        a44 = Szz - SxxpSyy - mxEigenV;
        a3344_4334 = a33 * a44 - a43 * a34;
        a3244_4234 = a32 * a44 - a42 * a34;
        a3243_4233 = a32 * a43 - a42 * a33;
        a3143_4133 = a31 * a43 - a41 * a33;
        a3144_4134 = a31 * a44 - a41 * a34;
        a3142_4132 = a31 * a42 - a41 * a32;
        q1 =  a22 * a3344_4334 - a23 * a3244_4234 + a24 * a3243_4233;
        q2 = -a21 * a3344_4334 + a23 * a3144_4134 - a24 * a3143_4133;
        q3 =  a21 * a3244_4234 - a22 * a3144_4134 + a24 * a3142_4132;
        q4 = -a21 * a3243_4233 + a22 * a3143_4133 - a23 * a3142_4132;

        qsqr = q1 * q1 + q2 * q2 + q3 * q3 + q4 * q4;

        /* The following code tries to calculate another column in the adjoint matrix when the norm of the
           current column is too small.
           Usually this block will never be activated. To be absolutely safe this should be
           uncommented, but it is most likely unnecessary.
        */
        if (qsqr < evecprec) {
            q1 =  a12 * a3344_4334 - a13 * a3244_4234 + a14 * a3243_4233;
            q2 = -a11 * a3344_4334 + a13 * a3144_4134 - a14 * a3143_4133;
            q3 =  a11 * a3244_4234 - a12 * a3144_4134 + a14 * a3142_4132;
            q4 = -a11 * a3243_4233 + a12 * a3143_4133 - a13 * a3142_4132;
            qsqr = q1 * q1 + q2 * q2 + q3 * q3 + q4 * q4;

            if (qsqr < evecprec) {
                double a1324_1423 = a13 * a24 - a14 * a23, a1224_1422 = a12 * a24 - a14 * a22;
                double a1223_1322 = a12 * a23 - a13 * a22, a1124_1421 = a11 * a24 - a14 * a21;
                double a1123_1321 = a11 * a23 - a13 * a21, a1122_1221 = a11 * a22 - a12 * a21;

                q1 =  a42 * a1324_1423 - a43 * a1224_1422 + a44 * a1223_1322;
                q2 = -a41 * a1324_1423 + a43 * a1124_1421 - a44 * a1123_1321;
                q3 =  a41 * a1224_1422 - a42 * a1124_1421 + a44 * a1122_1221;
                q4 = -a41 * a1223_1322 + a42 * a1123_1321 - a43 * a1122_1221;
                qsqr = q1*q1 + q2 *q2 + q3 * q3 + q4 * q4;

                if (qsqr < evecprec) {
                    q1 =  a32 * a1324_1423 - a33 * a1224_1422 + a34 * a1223_1322;
                    q2 = -a31 * a1324_1423 + a33 * a1124_1421 - a34 * a1123_1321;
                    q3 =  a31 * a1224_1422 - a32 * a1124_1421 + a34 * a1122_1221;
                    q4 = -a31 * a1223_1322 + a32 * a1123_1321 - a33 * a1122_1221;
                    qsqr = q1 * q1 + q2 * q2 + q3 * q3 + q4 * q4;

                    if (qsqr < evecprec) {
                        /* if qsqr is still too small, return the identity matrix. */
                        rot = IDENTITY_MATRIX_3D;
                    }
                }
            }
        } else {
            normq = Math.sqrt(qsqr);
            q1 /= normq;
            q2 /= normq;
            q3 /= normq;
            q4 /= normq;

            a2 = q1 * q1;
            x2 = q2 * q2;
            y2 = q3 * q3;
            z2 = q4 * q4;

            xy = q2 * q3;
            az = q1 * q4;
            zx = q4 * q2;
            ay = q1 * q3;
            yz = q3 * q4;
            ax = q1 * q2;

            rot[0][0] = a2 + x2 - y2 - z2;
            rot[0][1] = 2 * (xy + az);
            rot[0][2] = 2 * (zx - ay);
            rot[1][0] = 2 * (xy - az);
            rot[1][1] = a2 - x2 + y2 - z2;
            rot[1][2] = 2 * (yz + ax);
            rot[2][0] = 2 * (zx + ay);
            rot[2][1] = 2 * (yz - ax);
            rot[2][2] = a2 - x2 - y2 + z2;
        }

        double[] translation = Algebra.subtract3d(referenceCentroid, Algebra.multiply3d(Algebra.transpose3d(rot), candidateCentroid));
        return new Pair<>(new Transformation(translation, rot), rms);
    }
}
