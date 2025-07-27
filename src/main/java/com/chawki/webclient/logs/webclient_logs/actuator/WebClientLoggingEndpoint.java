package com.chawki.webclient.logs.webclient_logs.actuator;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.stereotype.Component;

import com.chawki.webclient.logs.webclient_logs.config.WebClientLoggingConfiguration;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@Endpoint(id = "webclient-logging")
public class WebClientLoggingEndpoint {

    private static final Logger log = LoggerFactory.getLogger(WebClientLoggingEndpoint.class);

    private final WebClientLoggingConfiguration loggingConfig;

    @Autowired
    public WebClientLoggingEndpoint(WebClientLoggingConfiguration loggingConfig) {
        this.loggingConfig = loggingConfig;
    }

    @ReadOperation
    public Map<String, Object> getLoggingConfiguration() {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", "active");
        response.put("configuration", createConfigurationMap());
        
        log.info("WebClient logging configuration retrieved via actuator endpoint");
        return response;
    }

    @WriteOperation
    public Map<String, Object> updateLoggingConfiguration(Boolean enabled, 
                                                          Boolean includeHeaders, 
                                                          Boolean includeBody, 
                                                          Integer maxBodySize,
                                                          Boolean includeParameters,
                                                          Boolean maskSensitiveData) {
        
        Map<String, Object> changes = new HashMap<>();
        
        if (enabled != null && enabled != loggingConfig.isEnabled()) {
            loggingConfig.setEnabled(enabled);
            changes.put("enabled", String.format("%s -> %s", !enabled, enabled));
            log.info("WebClient logging enabled status changed to: {}", enabled);
        }
        
        if (includeHeaders != null && includeHeaders != loggingConfig.isIncludeHeaders()) {
            loggingConfig.setIncludeHeaders(includeHeaders);
            changes.put("includeHeaders", String.format("%s -> %s", !includeHeaders, includeHeaders));
            log.info("WebClient logging include headers changed to: {}", includeHeaders);
        }
        
        if (includeBody != null && includeBody != loggingConfig.isIncludeBody()) {
            loggingConfig.setIncludeBody(includeBody);
            changes.put("includeBody", String.format("%s -> %s", !includeBody, includeBody));
            log.info("WebClient logging include body changed to: {}", includeBody);
        }
        
        if (maxBodySize != null && maxBodySize != loggingConfig.getMaxBodySize()) {
            int oldSize = loggingConfig.getMaxBodySize();
            loggingConfig.setMaxBodySize(maxBodySize);
            changes.put("maxBodySize", String.format("%d -> %d", oldSize, maxBodySize));
            log.info("WebClient logging max body size changed to: {}", maxBodySize);
        }
        
        if (includeParameters != null && includeParameters != loggingConfig.isIncludeParameters()) {
            loggingConfig.setIncludeParameters(includeParameters);
            changes.put("includeParameters", String.format("%s -> %s", !includeParameters, includeParameters));
            log.info("WebClient logging include parameters changed to: {}", includeParameters);
        }
        
        if (maskSensitiveData != null && maskSensitiveData != loggingConfig.isMaskSensitiveData()) {
            loggingConfig.setMaskSensitiveData(maskSensitiveData);
            changes.put("maskSensitiveData", String.format("%s -> %s", !maskSensitiveData, maskSensitiveData));
            log.info("WebClient logging mask sensitive data changed to: {}", maskSensitiveData);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", "updated");
        response.put("changes", changes);
        response.put("currentConfiguration", createConfigurationMap());
        
        if (changes.isEmpty()) {
            response.put("message", "No changes made - all values were already set to the provided values");
        } else {
            response.put("message", "WebClient logging configuration updated successfully");
        }
        
        return response;
    }

    private Map<String, Object> createConfigurationMap() {
        Map<String, Object> config = new HashMap<>();
        config.put("enabled", loggingConfig.isEnabled());
        config.put("includeHeaders", loggingConfig.isIncludeHeaders());
        config.put("includeBody", loggingConfig.isIncludeBody());
        config.put("maxBodySize", loggingConfig.getMaxBodySize());
        config.put("includeParameters", loggingConfig.isIncludeParameters());
        config.put("maskSensitiveData", loggingConfig.isMaskSensitiveData());
        return config;
    }
}
