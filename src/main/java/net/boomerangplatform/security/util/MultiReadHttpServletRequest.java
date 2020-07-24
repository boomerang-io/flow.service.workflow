package net.boomerangplatform.security.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.ReadListener;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;
import org.springframework.util.StreamUtils;

public class MultiReadHttpServletRequest implements HttpServletRequest {

  private final byte[] buffer;
  private final HttpServletRequest request;

  public MultiReadHttpServletRequest(HttpServletRequest request) throws IOException {
    this.request = request;
    ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();
    StreamUtils.copy(request.getInputStream(), bufferStream);
    this.buffer = bufferStream.toByteArray();
  }

  @Override
  public ServletInputStream getInputStream() throws IOException {
    ByteArrayInputStream stream = new ByteArrayInputStream(buffer);
    return new ServletInputStream() {
      private ReadListener listener;

      @Override
      public boolean isFinished() {
        return stream.available() == 0;
      }

      @Override
      public boolean isReady() {
        return true;
      }

      @Override
      public void setReadListener(ReadListener readListener) {
        this.listener = readListener;
        if (this.listener != null) {
          try {
            listener.onDataAvailable();
          } catch (IOException e) {
            listener.onError(e);
          }
        }
      }

      @Override
      public int read() throws IOException {
        try {
          return stream.read();
        } finally {
          if (listener != null && isFinished()) {
            listener.onAllDataRead();
          }
        }
      }
    };
  }

  @Override
  public String getAuthType() {
    return request.getAuthType();
  }

  @Override
  public Cookie[] getCookies() {
    return request.getCookies();
  }

  @Override
  public long getDateHeader(String name) {
    return request.getDateHeader(name);
  }

  @Override
  public String getHeader(String name) {
    return request.getHeader(name);
  }

  @Override
  public Enumeration<String> getHeaders(String name) {
    return request.getHeaders(name);
  }

  @Override
  public Enumeration<String> getHeaderNames() {
    return request.getHeaderNames();
  }

  @Override
  public int getIntHeader(String name) {
    return request.getIntHeader(name);
  }

  @Override
  public String getMethod() {
    return request.getMethod();
  }

  @Override
  public String getPathInfo() {
    return request.getPathInfo();
  }

  @Override
  public String getPathTranslated() {
    return request.getPathTranslated();
  }

  @Override
  public String getContextPath() {
    return request.getContextPath();
  }

  @Override
  public String getQueryString() {
    return request.getQueryString();
  }

  @Override
  public String getRemoteUser() {
    return request.getRemoteUser();
  }

  @Override
  public boolean isUserInRole(String role) {
    return request.isUserInRole(role);
  }

  @Override
  public Principal getUserPrincipal() {
    return request.getUserPrincipal();
  }

  @Override
  public String getRequestedSessionId() {
    return request.getRequestedSessionId(); // NOSONAR
  }

  @Override
  public String getRequestURI() {
    return request.getRequestURI();
  }

  @Override
  public StringBuffer getRequestURL() {
    return request.getRequestURL();
  }

  @Override
  public String getServletPath() {
    return request.getServletPath();
  }

  @Override
  public HttpSession getSession(boolean create) {
    return request.getSession(create);
  }

  @Override
  public HttpSession getSession() {
    return request.getSession();
  }

  @Override
  public String changeSessionId() {
    return request.changeSessionId();
  }

  @Override
  public boolean isRequestedSessionIdValid() {
    return request.isRequestedSessionIdValid();
  }

  @Override
  public boolean isRequestedSessionIdFromCookie() {
    return request.isRequestedSessionIdFromCookie();
  }

  @Override
  public boolean isRequestedSessionIdFromURL() {
    return request.isRequestedSessionIdFromURL();
  }

  @Override
  public boolean isRequestedSessionIdFromUrl() {
    return request.isRequestedSessionIdFromUrl(); // NOSONAR
  }

  @Override
  public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
    return request.authenticate(response);
  }

  @Override
  public void login(String username, String password) throws ServletException {
    request.login(username, password);
  }

  @Override
  public void logout() throws ServletException {
    request.logout();
  }

  @Override
  public Collection<Part> getParts() throws IOException, ServletException {
    return request.getParts();
  }

  @Override
  public Part getPart(String name) throws IOException, ServletException {
    return request.getPart(name);
  }

  @Override
  public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass)
      throws IOException, ServletException {
    return request.upgrade(handlerClass);
  }

  @Override
  public Object getAttribute(String name) {
    return request.getAttribute(name);
  }

  @Override
  public Enumeration<String> getAttributeNames() {
    return request.getAttributeNames();
  }

  @Override
  public String getCharacterEncoding() {
    return request.getCharacterEncoding();
  }

  @Override
  public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
    request.setCharacterEncoding(env);
  }

  @Override
  public int getContentLength() {
    return request.getContentLength();
  }

  @Override
  public long getContentLengthLong() {
    return request.getContentLengthLong();
  }

  @Override
  public String getContentType() {
    return request.getContentType();
  }

  @Override
  public String getParameter(String name) {
    return request.getParameter(name);
  }

  @Override
  public Enumeration<String> getParameterNames() {
    return request.getParameterNames();
  }

  @Override
  public String[] getParameterValues(String name) {
    return request.getParameterValues(name);
  }

  @Override
  public Map<String, String[]> getParameterMap() {
    return request.getParameterMap();
  }

  @Override
  public String getProtocol() {
    return request.getProtocol();
  }

  @Override
  public String getScheme() {
    return request.getScheme();
  }

  @Override
  public String getServerName() {
    return request.getServerName();
  }

  @Override
  public int getServerPort() {
    return request.getServerPort();
  }

  @Override
  public BufferedReader getReader() throws IOException {
    return request.getReader();
  }

  @Override
  public String getRemoteAddr() {
    return request.getRemoteAddr();
  }

  @Override
  public String getRemoteHost() {
    return request.getRemoteHost();
  }

  @Override
  public void setAttribute(String name, Object o) {
    request.setAttribute(name, o);
  }

  @Override
  public void removeAttribute(String name) {
    request.removeAttribute(name);
  }

  @Override
  public Locale getLocale() {
    return request.getLocale();
  }

  @Override
  public Enumeration<Locale> getLocales() {
    return request.getLocales();
  }

  @Override
  public boolean isSecure() {
    return request.isSecure();
  }

  @Override
  public RequestDispatcher getRequestDispatcher(String path) {
    return request.getRequestDispatcher(path);
  }

  @Override
  public String getRealPath(String path) {
    return request.getRealPath(path); // NOSONAR
  }

  @Override
  public int getRemotePort() {
    return request.getRemotePort();
  }

  @Override
  public String getLocalName() {
    return request.getLocalName();
  }

  @Override
  public String getLocalAddr() {
    return request.getLocalAddr();
  }

  @Override
  public int getLocalPort() {
    return request.getLocalPort();
  }

  @Override
  public ServletContext getServletContext() {
    return request.getServletContext();
  }

  @Override
  public AsyncContext startAsync() {
    return request.startAsync();
  }

  @Override
  public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) {
    return request.startAsync(servletRequest, servletResponse);
  }

  @Override
  public boolean isAsyncStarted() {
    return request.isAsyncStarted();
  }

  @Override
  public boolean isAsyncSupported() {
    return request.isAsyncSupported();
  }

  @Override
  public AsyncContext getAsyncContext() {
    return request.getAsyncContext();
  }

  @Override
  public DispatcherType getDispatcherType() {
    return request.getDispatcherType();
  }
}
