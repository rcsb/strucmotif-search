package org.rcsb.strucmotif.align;

import org.rcsb.strucmotif.domain.align.AlignmentResult;
import org.rcsb.strucmotif.domain.align.AtomCorrespondence;
import org.rcsb.strucmotif.domain.align.AtomPairingScheme;
import org.rcsb.strucmotif.domain.Pair;
import org.rcsb.strucmotif.domain.structure.LabelAtomId;
import org.rcsb.strucmotif.math.Algebra;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Align 2 sets of residues by a quaternion-based characteristic polynomial. Finds a rigid transformation that will move
 * the 2nd argument.
 */
@Service
public class QuaternionAlignmentService implements AlignmentService {
    private static final float[] IDENTITY_MATRIX_3D = new float[] { 1, 0, 0, 0, 1, 0, 0, 0, 1 };

    @Override
    public AlignmentResult align(List<Map<LabelAtomId, float[]>> reference, List<Map<LabelAtomId, float[]>> candidate, AtomPairingScheme atomPairingScheme) {
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
     * <p>base on code from: <a href="https://theobald.brandeis.edu/qcp/qcprot.c">https://theobald.brandeis.edu/qcp/qcprot.c</a>
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
        List<float[]> referencePoints = atomCorrespondence.getCenteredReferenceVectors();
        float[] referenceCentroid = atomCorrespondence.getReferenceCentroid();
        List<float[]> candidatePoints = atomCorrespondence.getCenteredCandidateVectors();
        float[] candidateCentroid = atomCorrespondence.getCandidateCentroid();

        Pair<float[], Float> alignment = align(referencePoints, referenceCentroid, candidatePoints, candidateCentroid);

        return new AlignmentResult(alignment.first(), alignment.second());
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
    public static Pair<float[], Float> align(List<float[]> referencePoints, float[] referenceCentroid, List<float[]> candidatePoints, float[] candidateCentroid) {
        float[] rot = new float[9];

        // inner product
        double g1 = 0.0;
        double g2 = 0.0;
        double[] matA = new double[9];

        for (int i = 0; i < referencePoints.size(); i++) {
            float[] r = referencePoints.get(i);
            float[] c = candidatePoints.get(i);

            double x1 = r[0];
            double y1 = r[1];
            double z1 = r[2];
            g1 += x1 * x1 + y1 * y1 + z1 * z1;

            double x2 = c[0];
            double y2 = c[1];
            double z2 = c[2];
            g2 += (x2 * x2 + y2 * y2 + z2 * z2);

            matA[0] +=  (x1 * x2);
            matA[1] +=  (x1 * y2);
            matA[2] +=  (x1 * z2);

            matA[3] +=  (y1 * x2);
            matA[4] +=  (y1 * y2);
            matA[5] +=  (y1 * z2);

            matA[6] +=  (z1 * x2);
            matA[7] +=  (z1 * y2);
            matA[8] +=  (z1 * z2);
        }
        double e0 = (g1 + g2) * 0.5;

        // fast calc RMSD and rotation
        double sxx;
        double sxy;
        double sxz;
        double syx;
        double syy;
        double syz;
        double szx;
        double szy;
        double szz;
        double szz2;
        double syy2;
        double sxx2;
        double sxy2;
        double syz2;
        double sxz2;
        double syx2;
        double szy2;
        double szx2;
        double syzszymsyyszz2;
        double sxx2syy2szz2syz2szy2;
        double sxy2sxz2syx2szx2;
        double sxzpszx;
        double syzpszy;
        double sxypsyx;
        double syzmszy;
        double sxzmszx;
        double sxymsyx;
        double sxxpsyy;
        double sxxmsyy;
        double[] c = new double[4];
        int i;
        double mxEigenV;
        double oldg;
        double b;
        double a;
        double delta;
        double rms;
        double qsqr;
        double q1;
        double q2;
        double q3;
        double q4;
        double normq;
        double a11;
        double a12;
        double a13;
        double a14;
        double a21;
        double a22;
        double a23;
        double a24;
        double a31;
        double a32;
        double a33;
        double a34;
        double a41;
        double a42;
        double a43;
        double a44;
        double a2;
        double x2;
        double y2;
        double z2;
        double xy;
        double az;
        double zx;
        double ay;
        double yz;
        double ax;
        double a33444334;
        double a32444234;
        double a32434233;
        double a31434133;
        double a31444134;
        double a31424132;
        double evecprec = 1e-6;
        double evalprec = 1e-11;

        sxx = matA[0]; sxy = matA[1]; sxz = matA[2];
        syx = matA[3]; syy = matA[4]; syz = matA[5];
        szx = matA[6]; szy = matA[7]; szz = matA[8];

        sxx2 = sxx * sxx;
        syy2 = syy * syy;
        szz2 = szz * szz;

        sxy2 = sxy * sxy;
        syz2 = syz * syz;
        sxz2 = sxz * sxz;

        syx2 = syx * syx;
        szy2 = szy * szy;
        szx2 = szx * szx;

        syzszymsyyszz2 = 2.0 * (syz * szy - syy * szz);
        sxx2syy2szz2syz2szy2 = syy2 + szz2 - sxx2 + syz2 + szy2;

        c[2] = -2.0 * (sxx2 + syy2 + szz2 + sxy2 + syx2 + sxz2 + szx2 + syz2 + szy2);
        c[1] = 8.0 * (sxx * syz * szy + syy * szx * sxz + szz * sxy * syx - sxx * syy * szz - syz * szx * sxy - szy * syx * sxz);

        sxzpszx = sxz + szx;
        syzpszy = syz + szy;
        sxypsyx = sxy + syx;
        syzmszy = syz - szy;
        sxzmszx = sxz - szx;
        sxymsyx = sxy - syx;
        sxxpsyy = sxx + syy;
        sxxmsyy = sxx - syy;
        sxy2sxz2syx2szx2 = sxy2 + sxz2 - syx2 - szx2;

        c[0] = sxy2sxz2syx2szx2 * sxy2sxz2syx2szx2
                + (sxx2syy2szz2syz2szy2 + syzszymsyyszz2) * (sxx2syy2szz2syz2szy2 - syzszymsyyszz2)
                + (-(sxzpszx) * (syzmszy) + (sxymsyx) * (sxxmsyy - szz)) * (-(sxzmszx) * (syzpszy) + (sxymsyx) * (sxxmsyy + szz))
                + (-(sxzpszx) * (syzpszy) - (sxypsyx) * (sxxpsyy - szz)) * (-(sxzmszx) * (syzmszy) - (sxypsyx) * (sxxpsyy + szz))
                + ((sxypsyx) * (syzpszy) + (sxzpszx) * (sxxmsyy + szz)) * (-(sxymsyx) * (syzmszy) + (sxzpszx) * (sxxpsyy + szz))
                + ((sxypsyx) * (syzmszy) + (sxzmszx) * (sxxmsyy - szz)) * (-(sxymsyx) * (syzpszy) + (sxzmszx) * (sxxpsyy - szz));

        /* Newton-Raphson */
        mxEigenV = e0;
        for (i = 0; i < 50; ++i) {
            oldg = mxEigenV;
            x2 = mxEigenV * mxEigenV;
            b = (x2 + c[2]) * mxEigenV;
            a = b + c[1];
            delta = ((a * mxEigenV + c[0]) / (2.0 * x2 * mxEigenV + b + a));
            mxEigenV -= delta;
            if (Math.abs(mxEigenV - oldg) < Math.abs(evalprec * mxEigenV)) {
                break;
            }
        }

        /* the abs() is to guard against tiny, but *negative* numbers due to floating point error */
        rms = Math.sqrt(Math.abs(2.0 * (e0 - mxEigenV) / referencePoints.size()));

        a11 = sxxpsyy + szz - mxEigenV;
        a12 = syzmszy;
        a13 = -sxzmszx;
        a14 = sxymsyx;
        a21 = syzmszy;
        a22 = sxxmsyy - szz - mxEigenV;
        a23 = sxypsyx;
        a24 = sxzpszx;
        a31 = a13;
        a32 = a23;
        a33 = syy - sxx - szz - mxEigenV;
        a34 = syzpszy;
        a41 = a14;
        a42 = a24;
        a43 = a34;
        a44 = szz - sxxpsyy - mxEigenV;
        a33444334 = a33 * a44 - a43 * a34;
        a32444234 = a32 * a44 - a42 * a34;
        a32434233 = a32 * a43 - a42 * a33;
        a31434133 = a31 * a43 - a41 * a33;
        a31444134 = a31 * a44 - a41 * a34;
        a31424132 = a31 * a42 - a41 * a32;
        q1 =  a22 * a33444334 - a23 * a32444234 + a24 * a32434233;
        q2 = -a21 * a33444334 + a23 * a31444134 - a24 * a31434133;
        q3 =  a21 * a32444234 - a22 * a31444134 + a24 * a31424132;
        q4 = -a21 * a32434233 + a22 * a31434133 - a23 * a31424132;

        qsqr = q1 * q1 + q2 * q2 + q3 * q3 + q4 * q4;

        /* The following code tries to calculate another column in the adjoint matrix when the norm of the
           current column is too small.
           Usually this block will never be activated. To be absolutely safe this should be
           uncommented, but it is most likely unnecessary.
        */
        if (qsqr < evecprec) {
            q1 =  a12 * a33444334 - a13 * a32444234 + a14 * a32434233;
            q2 = -a11 * a33444334 + a13 * a31444134 - a14 * a31434133;
            q3 =  a11 * a32444234 - a12 * a31444134 + a14 * a31424132;
            q4 = -a11 * a32434233 + a12 * a31434133 - a13 * a31424132;
            qsqr = q1 * q1 + q2 * q2 + q3 * q3 + q4 * q4;

            if (qsqr < evecprec) {
                double a13241423 = a13 * a24 - a14 * a23;
                double a12241422 = a12 * a24 - a14 * a22;
                double a12231322 = a12 * a23 - a13 * a22;
                double a11241421 = a11 * a24 - a14 * a21;
                double a11231321 = a11 * a23 - a13 * a21;
                double a11221221 = a11 * a22 - a12 * a21;

                q1 =  a42 * a13241423 - a43 * a12241422 + a44 * a12231322;
                q2 = -a41 * a13241423 + a43 * a11241421 - a44 * a11231321;
                q3 =  a41 * a12241422 - a42 * a11241421 + a44 * a11221221;
                q4 = -a41 * a12231322 + a42 * a11231321 - a43 * a11221221;
                qsqr = q1*q1 + q2 *q2 + q3 * q3 + q4 * q4;

                if (qsqr < evecprec) {
                    q1 =  a32 * a13241423 - a33 * a12241422 + a34 * a12231322;
                    q2 = -a31 * a13241423 + a33 * a11241421 - a34 * a11231321;
                    q3 =  a31 * a12241422 - a32 * a11241421 + a34 * a11221221;
                    q4 = -a31 * a12231322 + a32 * a11231321 - a33 * a11221221;
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

            rot[0] = (float) (a2 + x2 - y2 - z2);
            rot[1] = (float) (2 * (xy + az));
            rot[2] = (float) (2 * (zx - ay));
            rot[3] = (float) (2 * (xy - az));
            rot[4] = (float) (a2 - x2 + y2 - z2);
            rot[5] = (float) (2 * (yz + ax));
            rot[6] = (float) (2 * (zx + ay));
            rot[7] = (float) (2 * (yz - ax));
            rot[8] = (float) (a2 - x2 - y2 + z2);
        }

        Algebra.multiply3d(candidateCentroid, Algebra.transpose3d(rot), candidateCentroid);
        float[] translation = new float[3];
        Algebra.subtract3d(translation, referenceCentroid, candidateCentroid);
        float[] transformation = Algebra.composeTransformationMatrix(rot, translation);
        return new Pair<>(transformation, (float) rms);
    }
}
