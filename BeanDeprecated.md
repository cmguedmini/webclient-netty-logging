deprecated.keys=app.code.dns-timeout,code.log.test,legacy.timeout
deprecated.guide=https://ton-site.com/guide-migration
---
package com.example.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

@Component
public class DeprecatedPropertiesValidator implements SmartInitializingSingleton {

    private static final Logger logger = LoggerFactory.getLogger(DeprecatedPropertiesValidator.class);

    private final Environment environment;

    public DeprecatedPropertiesValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void afterSingletonsInstantiated() {
        Properties props = new Properties();
        try {
            props.load(new ClassPathResource("deprecated.properties").getInputStream());
        } catch (IOException e) {
            logger.warn("Impossible de charger le fichier deprecated.properties : {}", e.getMessage());
            return;
        }

        String guideUrl = props.getProperty("deprecated.guide", "https://default-guide.com");
        String keysRaw = props.getProperty("deprecated.keys", "");
        List<String> deprecatedKeys = Arrays.stream(keysRaw.split(","))
                                            .map(String::trim)
                                            .filter(k -> !k.isEmpty())
                                            .toList();

        for (String key : deprecatedKeys) {
            if (environment.containsProperty(key)) {
                String value = environment.getProperty(key);
                logger.warn("⚠️ La propriété dépréciée '{}' est utilisée avec la valeur '{}'. Veuillez la supprimer. Consultez le guide de migration : {}", key, value, guideUrl);
            }
        }
    }
}
---
package com.example.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.env.Environment;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class private Appender<ILoggingEvent> mockAppender;

    @BeforeEach
    void setupLogger() {
        logger = (Logger) org.slf4j.LoggerFactory.getLogger(DeprecatedPropertiesValidator.class);
        mockAppender = mock(Appender.class);
        when(mockAppender.getName()).thenReturn("MOCK");
        logger.addAppender(mockAppender);
        logger.setLevel(Level.WARN);
    }

    @Test
    void shouldLogWarningWhenDeprecatedPropertyIsPresent() {
        Environment env = mock(Environment.class);
        when(env.containsProperty("app.code.dns-timeout")).thenReturn(true);
        when(env.getProperty("app.code.dns-timeout")).thenReturn("5000");

        DeprecatedPropertiesValidator validator = new DeprecatedPropertiesValidator(env) {
            @Override
            public void afterSingletonsInstantiated() {
                this.deprecatedKeys = List.of("app.code.dns-timeout");
                this.guideUrl = "https://ton-site.com/guide-migration";
                super.afterSingletonsInstantiated();
            }
        };

        validator.afterSingletonsInstantiated();

        ArgumentCaptor<ILoggingEvent> captor = ArgumentCaptor.forClass(ILoggingEvent.class);
        verify(mockAppender, atLeastOnce()).doAppend(captor.capture());

        boolean found = captor.getAllValues().stream()
            .anyMatch(event -> event.getLevel() == Level.WARN &&
                               event.getFormattedMessage().contains("app.code.dns-timeout") &&
                               event.getFormattedMessage().contains("guide de migration"));

        assertTrue(found, "Le message de warning attendu n’a pas été trouvé.");
    }

    @Test
    void shouldNotLogWarningWhenNoDeprecatedPropertyIsPresent() {
        Environment env = mock(Environment.class);
        when(env.containsProperty("app.code.dns-timeout")).thenReturn(false);

        DeprecatedPropertiesValidator validator = new DeprecatedPropertiesValidator(env) {
            @Override
            public void afterSingletonsInstantiated() {
                this.deprecatedKeys = List.of("app.code.dns-timeout");
                this.guideUrl = "https://ton-site.com/guide-migration";
                super.afterSingletonsInstantiated();
            }
        };

        validator.afterSingletonsInstantiated();

        ArgumentCaptor<ILoggingEvent> captor = ArgumentCaptor.forClass(ILoggingEvent.class);
        verify(mockAppender, never()).doAppend(captor.capture());
    }
}
``
