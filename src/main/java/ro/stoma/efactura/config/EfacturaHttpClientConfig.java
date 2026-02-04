package ro.stoma.efactura.config;

import java.time.Duration;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.impl.DefaultConnectionKeepAliveStrategy;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

@Configuration
public class EfacturaHttpClientConfig {
  @Bean(name = "efacturaClientHttpRequestFactory")
  public ClientHttpRequestFactory efacturaClientHttpRequestFactory(EfacturaProperties properties) {
    EfacturaProperties.Api api = properties.getApi();
    Duration connectTimeout = api.getConnectTimeout();
    Duration readTimeout = api.getReadTimeout();
    Duration keepAlive = api.getKeepAlive();

    PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
        .setMaxConnTotal(api.getMaxTotalConnections())
        .setMaxConnPerRoute(api.getMaxPerRouteConnections())
        .build();

    RequestConfig requestConfig = RequestConfig.custom()
        .setConnectTimeout(toTimeout(connectTimeout))
        .setConnectionRequestTimeout(toTimeout(connectTimeout))
        .setResponseTimeout(toTimeout(readTimeout))
        .build();

    CloseableHttpClient httpClient = HttpClients.custom()
        .setConnectionManager(connectionManager)
        .setDefaultRequestConfig(requestConfig)
        .setKeepAliveStrategy((response, context) -> {
          TimeValue serverKeepAlive = DefaultConnectionKeepAliveStrategy.INSTANCE.getKeepAliveDuration(response, context);
          if (serverKeepAlive == null || serverKeepAlive.getDuration() <= 0) {
            return TimeValue.ofMilliseconds(keepAlive.toMillis());
          }
          return serverKeepAlive;
        })
        .build();

    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
    factory.setConnectTimeout(toMillis(connectTimeout));
    factory.setConnectionRequestTimeout(toMillis(connectTimeout));
    return factory;
  }

  private static Timeout toTimeout(Duration duration) {
    return Timeout.ofMilliseconds(toMillis(duration));
  }

  private static int toMillis(Duration duration) {
    long millis = duration == null ? 0 : duration.toMillis();
    if (millis > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    }
    return (int) millis;
  }
}
