package edu.cmu.sphinx.api;

import java.io.Closeable;
import java.io.IOException;

public interface ByteInputStream extends Closeable {

  int read(byte[] buffer, int offset, int len) throws IOException;

}

