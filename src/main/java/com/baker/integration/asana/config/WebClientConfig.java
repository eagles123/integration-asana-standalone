package com.baker.integration.asana.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean("asanaWebClient")
    public WebClient asanaWebClient(AsanaAppProperties asanaAppProperties) {
        return WebClient.builder()
                .baseUrl(asanaAppProperties.getApiBaseUrl())
                .build();
    }

    @Bean("paragonWebClient")
    public WebClient paragonWebClient(ParagonProperties paragonProperties) {
        return WebClient.builder()
                .baseUrl(paragonProperties.getApiBaseUrl())
                .build();
    }

    @Bean("serviceAssetsWebClient")
    public WebClient serviceAssetsWebClient(ServiceAssetsProperties serviceAssetsProperties) {
        return WebClient.builder()
                .baseUrl(serviceAssetsProperties.getBaseUrl())
                .build();
    }

    @Bean("streamingWebClient")
    public WebClient streamingWebClient() {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMinutes(10));

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .build();
    }
}
