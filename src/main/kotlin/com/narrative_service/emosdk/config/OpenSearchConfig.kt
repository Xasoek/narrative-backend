package com.narrative_service.emosdk.config

import org.apache.hc.core5.http.HttpHost
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenSearchConfig {
    @Bean
    fun openSearchClient(): OpenSearchClient {
        val host = HttpHost("http", "opensearch", 9200)

        val transport = ApacheHttpClient5TransportBuilder
            .builder(host)
            .build()

        return OpenSearchClient(transport)
    }
}