package edu.cmu.sphinx.api;

import java.io.IOException;
import java.io.InputStream;

public class InputStreams {

  public static ByteInputStream fromInputStream(InputStream inputStream) {
    return new InputStreamWrapper(inputStream);
  }

  public static class InputStreamWrapper implements ByteInputStream {

    private final InputStream stream;

    public InputStreamWrapper(InputStream stream) {
      this.stream = stream;
    }

    @Override
    public int read(byte[] buffer, int offset, int len) throws IOException {
      return stream.read(buffer, offset, len);
    }

    @Override
    public void close() throws IOException {
      stream.close();
    }
  }
}
