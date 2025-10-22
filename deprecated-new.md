package com.example.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class DeprecatedPropertiesLogger implements EnvironmentPostProcessor, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(DeprecatedPropertiesLogger.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        DeprecatedConfig config = loadDeprecatedConfig(environment);

        Set<String> allPropertyNames = new HashSet<>();
        for (PropertySource<?> source : environment.getPropertySources()) {
            if (source.getSource() instanceof Map<?, ?> map) {
                for (Object key : map.keySet()) {
                    allPropertyNames.add(key.toString());
                }
            }
        }

        // Vérification des propriétés exactes
        for (String property : config.properties) {
            if (environment.containsProperty(property)) {
                String value = environment.getProperty(property);
                logger.warn("⚠️ La propriété obsolète '{}' est utilisée avec la valeur '{}'. Veuillez la supprimer ou la remplacer. Consultez le guide de migration : {}",
                        property, value, config.guide);
            }
        }

        // Vérification des préfixes (wildcards)
        for (String prefix : config.wildcards) {
            for (String propName : allPropertyNames) {
                if (propName.startsWith(prefix)) {
                    String value = environment.getProperty(propName);
                    logger.warn("⚠️ La propriété obsolète '{}' (préfixe '{}') est utilisée avec la valeur '{}'. Veuillez la supprimer ou la remplacer. Consultez le guide de migration : {}",
                            propName, prefix, value, config.guide);
                }
            }
        }
    }

    private DeprecatedConfig loadDeprecatedConfig(ConfigurableEnvironment environment) {
        DeprecatedConfig config = new DeprecatedConfig();

        config.guide = environment.getProperty("deprecated.guide", "https://default-guide.com");

        String[] props = environment.getProperty("deprecated.properties", String[].class);
        config.properties = props != null ? List.of(props) : List.of();

        String[] wildcards = environment.getProperty("deprecated.wildcards", String[].class);
        config.wildcards = wildcards != null ? List.of(wildcards) : List.of();

        return config;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    protected static class DeprecatedConfig {
        public String guide;
        public List<String> properties = new ArrayList<>();
        public List<String> wildcards = new ArrayList<>();
    }
}

---

deprecated:
  guide: "https://ton-site.com/guide-migration"
  properties:
    - app.old-property
    - legacy.timeout
  wildcards:
    - app.code.
    - legacy.feature.
------
package com.example.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "deprecated")
public class DeprecatedPropertiesConfig {

    private String guide;
    private List<String> properties;
    private List<String> wildcards;

    // Getters et Setters
    public String getGuide() {
        return guide;
    }

    public void setGuide(String guide) {
        this.guide = guide;
    }

    public List<String> getProperties() {
        return properties;
    }

    public void setProperties(List<String> properties) {
        this.properties = properties;
    }

    public List<String> getWildcards() {
        return wildcards;
    }

    public void setWildcards(List<String> wildcards) {
        this.wildcards = wildcards;
    }
}
------
package com.example.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.List;
import.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeprecatedPropertiesLoggerTest {

    private Logger logger;
    private Appender<ILoggingEvent> mockAppender;

    @BeforeEach
    void setup() {
        logger = (Logger) LoggerFactory.getLogger(DeprecatedPropertiesLogger.class);
        mockAppender = mock(Appender.class);
        when(mockAppender.getName()).thenReturn("MOCK");
        logger.addAppender(mockAppender);
        logger.setLevel(Level.WARN);
    }

    @Test
    void shouldLogWarningForDeprecatedProperty() {
        // Mock de l'environnement
        ConfigurableEnvironment environment = mock(ConfigurableEnvironment.class);
        Map<String, Object> properties = Map.of("app.old-property", "true");
        MapPropertySource propertySource = new MapPropertySource("test", properties);

        when(environment.getPropertySources()).thenReturn(new org.springframework.core.env.MutablePropertySources());
        environment.getPropertySources().addLast(propertySource);
        when(environment.containsProperty("app.old-property")).thenReturn(true);
        when(environment.getProperty("app.old-property")).thenReturn("true");

        // Instancier le post processor
        DeprecatedPropertiesLogger processor = new DeprecatedPropertiesLogger() {
            @Override
            protected DeprecatedConfig loadDeprecatedConfig(ConfigurableEnvironment env) {
                DeprecatedConfig config = new DeprecatedConfig();
                config.guide = "https://ton-site.com/guide-migration";
                config.properties = List.of("app.old-property");
                config.wildcards = List.of();
                return config;
            }
        };

        processor.postProcessEnvironment(environment, mock(SpringApplication.class));

        // Capturer les logs
        ArgumentCaptor<ILoggingEvent> captor = ArgumentCaptor.forClass(ILoggingEvent.class);
        verify(mockAppender, atLeastOnce()).doAppend(captor.capture());

        List<ILoggingEvent> logs = captor.getAllValues();
        boolean found = logs.stream().anyMatch(log ->
            log.getLevel() == Level.WARN &&
            log.getFormattedMessage().contains("app.old-property") &&
            log.getFormattedMessage().contains("guide de migration")
        );

        assertTrue(found, "Le message de warning attendu n’a pas été trouvé.");
    }

    @Test
    void shouldNotLogWarningForValidProperty() {
        // Mock de l'environnement
        ConfigurableEnvironment environment = mock(ConfigurableEnvironment.class);
        Map<String, Object> properties = Map.of("app.valid-property", "ok");
        MapPropertySource propertySource = new MapPropertySource("test", properties);

        when(environment.getPropertySources()).thenReturn(new org.springframework.core.env.MutablePropertySources());
        environment.getPropertySources().addLast(propertySource);
        when(environment.containsProperty("app.valid-property")).thenReturn(true);
        when(environment.getProperty("app.valid-property")).thenReturn("ok");

        // Instancier le post processor
        DeprecatedPropertiesLogger processor = new DeprecatedPropertiesLogger() {
            @Override
            protected DeprecatedConfig loadDeprecatedConfig(ConfigurableEnvironment env) {
                DeprecatedConfig config = new DeprecatedConfig();
                config.guide = "https://ton-site.com/guide-migration";
                config.properties = List.of("app.old-property");
                config.wildcards = List.of("legacy.");
                return config;
            }
        };

        processor.postProcessEnvironment(environment, mock(SpringApplication.class));

        // Capturer les logs
        ArgumentCaptor<ILoggingEvent> captor = ArgumentCaptor.forClass(ILoggingEvent.class);
        verify(mockAppender, never()).doAppend(captor.capture());
    }
}
