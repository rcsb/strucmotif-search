package org.rcsb.strucmotif.io;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An optimized {@link DataOutputStream} without synchronization in its write methods.
 */
public class UnsynchronizedDataOutputStream extends DataOutputStream {
    public UnsynchronizedDataOutputStream(OutputStream out) {
        super(out);
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
        incCount(1);
    }

    /**
     * Increases the written counter by the specified value
     * until it reaches Integer.MAX_VALUE.
     */
    private void incCount(int value) {
        int temp = written + value;
        if (temp < 0) {
            temp = Integer.MAX_VALUE;
        }
        written = temp;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
        incCount(len);
    }
}
