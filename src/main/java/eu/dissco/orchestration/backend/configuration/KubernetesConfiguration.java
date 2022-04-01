package eu.dissco.orchestration.backend.configuration;

import eu.dissco.orchestration.backend.properties.KubernetesProperties;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.util.Config;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
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
    var connectionConfig = ConnectionConfig.custom()
        .setCharset(StandardCharsets.UTF_8).build();
    return HttpClients.custom().evictExpiredConnections()
        .setDefaultSocketConfig(socketConfig)
        .setDefaultConnectionConfig(connectionConfig).evictIdleConnections(1L,
            TimeUnit.MINUTES)
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
