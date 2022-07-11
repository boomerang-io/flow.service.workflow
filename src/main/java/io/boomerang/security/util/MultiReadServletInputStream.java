package io.boomerang.security.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

public class MultiReadServletInputStream extends ServletInputStream {

  private InputStream mutliReadInputStream;

  public MultiReadServletInputStream(byte[] buffer) {
      this.mutliReadInputStream = new ByteArrayInputStream(buffer);
  }

  @Override
  public boolean isFinished() {
    try {
      return mutliReadInputStream.available() == 0;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return false;
  }

  @Override
  public boolean isReady() {
    return true;
  }

  @Override
  public void setReadListener(ReadListener listener) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int read() throws IOException {
    return mutliReadInputStream.read();
  }
}