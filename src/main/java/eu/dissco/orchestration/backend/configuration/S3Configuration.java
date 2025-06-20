package eu.dissco.orchestration.backend.configuration;

import eu.dissco.orchestration.backend.properties.S3Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@RequiredArgsConstructor
public class S3Configuration {

  private final S3Properties s3Properties;

  @Bean
  public S3Client s3Client() {
    return S3Client.builder().credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create(s3Properties.getAccessKey(),
                s3Properties.getAccessSecret())))
        .region(Region.EU_WEST_2)
        .build();
  }
}
