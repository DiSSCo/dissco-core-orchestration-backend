package eu.dissco.orchestration.backend.configuration;

import eu.dissco.orchestration.backend.properties.KubernetesProperties;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.util.Config;
import java.io.IOException;
import lombok.RequiredArgsConstructor;


import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.TimeValue;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({KubernetesProperties.class})
public class KubernetesConfiguration {

  private final KubernetesProperties properties;

  @Bean
  public CloseableHttpClient httpClient() {
    var socketConfig = SocketConfig.custom().setTcpNoDelay(true).build();

    var connectionConfig = ConnectionConfig.custom().build();

    var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
            .setDefaultSocketConfig(socketConfig)
            .setDefaultConnectionConfig(connectionConfig)
            .build();

    return HttpClients.custom()
            .evictExpiredConnections()
            .evictIdleConnections(TimeValue.ofMinutes(1L))
            .setConnectionManager(connectionManager)
            .build();
  }

  @Bean
  public BatchV1Api batchV1Api() throws IOException {
    var client = apiClient();
    return new BatchV1Api(client);
  }

  @Bean
  public ApiClient apiClient() throws IOException {
    var apiClient = Config.defaultClient();
    var httpClient = apiClient.getHttpClient().newBuilder()
        .retryOnConnectionFailure(true)
        .readTimeout(properties.getApiReadTimeout())
        .writeTimeout(properties.getApiWriteTimeout())
        .connectTimeout(properties.getApiConnectTimeout())
        .pingInterval(properties.getApiPingInterval())
        .build();
    apiClient.setHttpClient(httpClient);
    return apiClient;
  }

}
