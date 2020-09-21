package org.rcsb.strucmotif.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Specification of the MessagePack implementation.
 */
public interface MessagePackCodec {
    /**
     * Encode a json-esque Map representation into its binary representation.
     * @param input the data to encode
     * @return a byte array
     */
    byte[] encode(Map<String, Object> input);

    /**
     * Read from a stream and decode into json-esque Map representation.
     * @param inputStream the data to decode
     * @return Map representation of the decoded data
     * @throws IOException if reading fails
     */
    Map<String, Object> decode(InputStream inputStream) throws IOException;
}
