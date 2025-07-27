package com.chawki.webclient.logs.webclient_logs.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import com.chawki.webclient.logs.webclient_logs.logging.WebClientLoggingFilter;

import io.netty.handler.logging.LogLevel;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

@Configuration
public class WebClientConfig {

    @Value("${webclient.base-url}")
    private String baseUrl;

    @Value("${webclient.timeout.connection:5000}")
    private int connectionTimeout;

    @Value("${webclient.timeout.response:10000}")
    private int responseTimeout;

    @Value("${webclient.max-in-memory-size:1048576}")
    private int maxInMemorySize;

    @Bean
    public WebClient webClient(WebClientLoggingFilter loggingFilter) {
        // Configure Netty HttpClient
        HttpClient httpClient = HttpClient.create()
        		.wiretap("reactor.netty.http.client.HttpClient", 
                        LogLevel.DEBUG, 
                        AdvancedByteBufFormat.TEXTUAL); // Enable Netty wire logging

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                //.filter(loggingFilter) // Add custom logging filter
                .build();
    }

    @Bean
    public WebClientLoggingFilter webClientLoggingFilter(WebClientLoggingConfiguration loggingConfig) {
        return new WebClientLoggingFilter(loggingConfig);
    }
}