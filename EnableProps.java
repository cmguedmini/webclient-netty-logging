
// src/main/java/com/example/http/EnableProps.java
package com.example.http;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(HttpClient5Properties.class)
public class EnableProps { }
