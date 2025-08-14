package com.chawki.webclient.logs.webclient_logs.resolver;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.util.List;

@ConfigurationProperties(prefix = "jef.security")
public record SecurityProperties(ActuatorSecurity actuator) {

    public record ActuatorSecurity(boolean enabled, List<EndpointConfig> endpoints) {}

    public record EndpointConfig(String url, boolean authenticated, List<String> roles) {
        public EndpointConfig {
            if (roles == null) {
                roles = List.of();
            }
        }
    }
}
