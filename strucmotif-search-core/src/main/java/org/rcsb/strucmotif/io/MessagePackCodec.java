package org.rcsb.strucmotif.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public interface MessagePackCodec {
    byte[] encodeArray(Object[] array);

    byte[] encode(Map<String, Object> input);

    Object[] decodeArray(InputStream inputStream) throws IOException;

    Map<String, Object> decode(InputStream inputStream) throws IOException;
}
