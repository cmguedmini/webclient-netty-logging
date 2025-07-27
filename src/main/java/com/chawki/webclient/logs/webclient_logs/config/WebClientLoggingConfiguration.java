package com.chawki.webclient.logs.webclient_logs.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "webclient.logging")
public class WebClientLoggingConfiguration {
    
    private boolean enabled = true;
    private boolean includeHeaders = true;
    private boolean includeBody = true;
    private int maxBodySize = 1000;
    private boolean includeParameters = true;
    private boolean maskSensitiveData = true;

    // Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isIncludeHeaders() {
        return includeHeaders;
    }

    public void setIncludeHeaders(boolean includeHeaders) {
        this.includeHeaders = includeHeaders;
    }

    public boolean isIncludeBody() {
        return includeBody;
    }

    public void setIncludeBody(boolean includeBody) {
        this.includeBody = includeBody;
    }

    public int getMaxBodySize() {
        return maxBodySize;
    }

    public void setMaxBodySize(int maxBodySize) {
        this.maxBodySize = maxBodySize;
    }

    public boolean isIncludeParameters() {
        return includeParameters;
    }

    public void setIncludeParameters(boolean includeParameters) {
        this.includeParameters = includeParameters;
    }

    public boolean isMaskSensitiveData() {
        return maskSensitiveData;
    }

    public void setMaskSensitiveData(boolean maskSensitiveData) {
        this.maskSensitiveData = maskSensitiveData;
    }

    @Override
    public String toString() {
        return "WebClientLoggingConfiguration{" +
                "enabled=" + enabled +
                ", includeHeaders=" + includeHeaders +
                ", includeBody=" + includeBody +
                ", maxBodySize=" + maxBodySize +
                ", includeParameters=" + includeParameters +
                ", maskSensitiveData=" + maskSensitiveData +
                '}';
    }
}