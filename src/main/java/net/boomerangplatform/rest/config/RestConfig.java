package net.boomerangplatform.rest.config;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.net.ssl.SSLContext;
import org.apache.http.HttpHost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

@Component
public class RestConfig {

  @Value("${proxy.host:#{null}}")
  private Optional<String> boomerangProxyHost;

  @Value("${proxy.port:#{null}}")
  private Optional<String> boomerangProxyPort;

  @Bean
  @Qualifier("externalRestTemplate")
  public RestTemplate externalRestTemplate() {
    if (this.boomerangProxyHost.isPresent() && !this.boomerangProxyHost.get().isBlank()
        && this.boomerangProxyPort.isPresent() && !this.boomerangProxyPort.get().isBlank()) {
      HttpComponentsClientHttpRequestFactory clientHttpRequestFactory =
          new HttpComponentsClientHttpRequestFactory(
              HttpClientBuilder.create().setProxy(new HttpHost(this.boomerangProxyHost.get(),
                  Integer.valueOf(this.boomerangProxyPort.get()), "http")).build());
      return new RestTemplate(clientHttpRequestFactory);
    }
    return internalRestTemplate();
  }

  @Bean
  @Qualifier("insecureRestTemplate")
  public RestTemplate insecureRestTemplate()
      throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {

    final TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;
    final SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
        .loadTrustMaterial(null, acceptingTrustStrategy).build();

    final SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);
    final CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(csf).build();
    final HttpComponentsClientHttpRequestFactory requestFactory =
        new HttpComponentsClientHttpRequestFactory();
    requestFactory.setHttpClient(httpClient);
    final RestTemplate restTemplate = new RestTemplate(requestFactory);
    setRestTemplateInterceptors(restTemplate);
    return restTemplate;
  }

  @Bean
  @Qualifier("internalRestTemplate")
  public RestTemplate internalRestTemplate() {
    final HttpComponentsClientHttpRequestFactory requestFactory =
        new HttpComponentsClientHttpRequestFactory();
    final RestTemplate template = new RestTemplate(requestFactory);
    setRestTemplateInterceptors(template);
    return template;
  }
  
  @Bean
  @Qualifier("selfRestTemplate")
  public RestTemplate selfRestTemplate() {
    final HttpComponentsClientHttpRequestFactory requestFactory =
        new HttpComponentsClientHttpRequestFactory();
    final RestTemplate template = new RestTemplate(requestFactory);
    setRestTemplateInterceptors(template);
    return template;
  }

  private void setRestTemplateInterceptors(RestTemplate restTemplate) {
    List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
    if (CollectionUtils.isEmpty(interceptors)) {
      interceptors = new ArrayList<>();
    }
    restTemplate.setInterceptors(interceptors);
  }
}
