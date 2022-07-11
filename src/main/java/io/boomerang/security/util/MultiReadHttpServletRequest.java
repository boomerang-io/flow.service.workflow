package io.boomerang.security.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.springframework.util.StreamUtils;

/*
 * Reference: https://www.baeldung.com/spring-reading-httpservletrequest-multiple-times
 */
public class MultiReadHttpServletRequest extends HttpServletRequestWrapper {

  private final byte[] buffer;

  public MultiReadHttpServletRequest(HttpServletRequest request) throws IOException {
    super(request);
    InputStream requestInputStream = request.getInputStream();
    this.buffer = StreamUtils.copyToByteArray(requestInputStream);
  }

  @Override
  public ServletInputStream getInputStream() throws IOException {
      return new MultiReadServletInputStream(this.buffer);
  }

  @Override
  public BufferedReader getReader() throws IOException {
      ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.buffer);
      return new BufferedReader(new InputStreamReader(byteArrayInputStream));
  }
}
