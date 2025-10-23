package com.example.config;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class DeprecatedPropertiesConfigLoader {

    public List<String> getDeprecatedKeys() {
        return parseList(load().getProperty("deprecated.keys"));
    }

    public List<String> getDeprecatedWildcards() {
        return parseList(load().getProperty("deprecated.wildcards"));
    }

    public String getGuideUrl() {
        return load().getProperty("deprecated.guide", "https://default-guide.com");
    }

    private Properties load() {
        Properties props = new Properties();
        try {
            props.load(new ClassPathResource("deprecated.properties").getInputStream());
        } catch (IOException e) {
            // Log or handle error as needed
        }
        return props;
    }

    private List<String> parseList(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
------
@Component
@Import(DeprecatedPropertiesConfiguration.class)
public class DeprecatedPropertiesValidator implements SmartInitializingSingleton {

    private static final Logger logger = LoggerFactory.getLogger(DeprecatedPropertiesValidator.class);

    private final ConfigurableEnvironment environment;
    private final DeprecatedPropertiesConfigLoader configLoader;

    public DeprecatedPropertiesValidator(ConfigurableEnvironment environment,
                                         DeprecatedPropertiesConfigLoader configLoader) {
        this.environment = environment;
        this.configLoader = configLoader;
    }

    @Override
    public void afterSingletonsInstantiated() {
        List<String> deprecatedKeys = configLoader.getDeprecatedKeys();
        List<String> deprecatedPrefixes = configLoader.getDeprecatedWildcards();
        String guideUrl = configLoader.getGuideUrl();

        Set<String> allPropertyNames = new HashSet<>();
        for (PropertySource<?> source : environment.getPropertySources()) {
            if (source.getSource() instanceof Map<?, ?> map) {
                for (Object key : map.keySet()) {
                    allPropertyNames.add(key.toString());
                }
            }
        }

        for (String key : deprecatedKeys) {
            if (environment.containsProperty(key)) {
                String value = environment.getProperty(key);
                logger.warn("⚠️ La propriété dépréciée '{}' est utilisée avec la valeur '{}'. Veuillez la supprimer. Consultez le guide de migration : {}", key, value, guideUrl);
            }
        }

        for (String prefix : deprecatedPrefixes) {
            for (String propName : allPropertyNames) {
                if (propName.startsWith(prefix)) {
                    String value = environment.getProperty(propName);
                    logger.warn("⚠️ La propriété dépréciée '{}' (préfixe '{}') est utilisée avec la valeur '{}'. Veuillez la supprimer. Consultez le guide de migration : {}", propName, prefix, value, guideUrl);
                }
            }
        }
    }
}
----
package com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DeprecatedPropertiesConfiguration {

    @Bean
    public DeprecatedPropertiesConfigLoader deprecatedPropertiesConfigLoader() {
        return new DeprecatedPropertiesConfigLoader();
    }
}
----
package com.example.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeprecatedPropertiesValidatorTest {

    Logger logger;
    private Appender<ILoggingEvent> mockAppender;

    @BeforeEach
    void setupLogger() {
        logger = (Logger) org.slf4j.LoggerFactory.getLogger(DeprecatedPropertiesValidator.class);
        mockAppender = mock(Appender.class);
        when(mockAppender.getNameExactProperties() {
        // Mock de l'environnement
        ConfigurableEnvironment environment = mock(ConfigurableEnvironment.class);
        Map<String, Object> properties = Map.of(
            "app.old-property", "true",
            "jef.core.timeout", "5000",
            "code.log.test", "true",
            "app.valid.property", "ok"
        );
        MapPropertySource propertySource = new MapPropertySource("test", properties);
        when(environment.getPropertySources()).thenReturn(new org.springframework.core.env.MutablePropertySources());
        environment.getPropertySources().addLast(propertySource);

        for (String key : properties.keySet()) {
            when(environment.containsProperty(key)).thenReturn(true);
            when(environment.getProperty(key)).thenReturn(properties.get(key).toString());
        }

        // Mock du config loader
        DeprecatedPropertiesConfigLoader configLoader = mock(DeprecatedPropertiesConfigLoader.class);
        when(configLoader.getDeprecatedKeys()).thenReturn(List.of("app.old-property"));
        when(configLoader.getDeprecatedWildcards()).thenReturn(List.of("jef.core.", "code.log."));
        when(configLoader.getGuideUrl()).thenReturn("https://ton-site.com/guide-migration");

        // Instancier le validator avec les mocks
        DeprecatedPropertiesValidator validator = new DeprecatedPropertiesValidator(environment, configLoader);
        validator.afterSingletonsInstantiated();

        // Vérifier les logs
        ArgumentCaptor<ILoggingEvent> captor = ArgumentCaptor.forClass(ILoggingEvent.class);
        verify(mockAppender, atLeast(2)).doAppend(captor.capture());

        List<ILoggingEvent> logs = captor.getAllValues();

        assertTrue(logs.stream().anyMatch(log -> log.getFormattedMessage().contains("app.old-property")));
        assertTrue(logs.stream().anyMatch(log -> log.getFormattedMessage().contains("jef.core.timeout")));
        assertTrue(logs.stream().anyMatch(log -> log.getFormattedMessage().contains("code.log.test")));
        assertFalse(logs.stream().anyMatch(log -> log.getFormattedMessage().contains("app.valid.property")));
    }

    @Test
    void shouldNotLogWarningsWhenNoDeprecatedPropertiesPresent() {
        ConfigurableEnvironment environment = mock(ConfigurableEnvironment.class);
        Map<String, Object> properties = Map.of("app.valid.property", "ok");
        MapPropertySource propertySource = new MapPropertySource("test", properties);
        when(environment.getPropertySources()).thenReturn(new org.springframework.core.env.MutablePropertySources());
        environment.getPropertySources().addLast(propertySource);

        for (String key : properties.keySet()) {
            when(environment.containsProperty(key)).thenReturn(true);
            when(environment.getProperty(key)).thenReturn(properties.get(key).toString());
        }

        DeprecatedPropertiesConfigLoader configLoader = mock(DeprecatedPropertiesConfigLoader.class);
        when(configLoader.getDeprecatedKeys()).thenReturn(List.of("app.old-property"));
        when(configLoader.getDeprecatedWildcards()).thenReturn(List.of("jef.core.", "code.log."));
        when(configLoader.getGuideUrl()).thenReturn("https://ton-site.com/guide-migration");

        DeprecatedPropertiesValidator validator = new DeprecatedPropertiesValidator(environment, configLoader);
        validator.afterSingletonsInstantiated();

        ArgumentCaptor<ILoggingEvent> captor = ArgumentCaptor.forClass(ILoggingEvent.class);
        verify(mockAppender, never()).doAppend(captor.capture());
    }

    @Test
void shouldLogWarningForDeprecatedProperty() {
    // Préparer le logger
    Logger logger = (Logger) LoggerFactory.getLogger(DeprecatedPropertiesValidator.class);
    Appender<ILoggingEvent> mockAppender = mock(Appender.class);
    when(mockAppender.getName()).thenReturn("MOCK");
    logger.addAppender(mockAppender);
    logger.setLevel(Level.WARN);

    // Simuler l'environnement avec une propriété dépréciée
    ConfigurableEnvironment environment = mock(ConfigurableEnvironment.class);
    Map<String, Object> properties = Map.of("app.old-property", "true");
    MapPropertySource propertySource = new MapPropertySource("test", properties);
    when(environment.getPropertySources()).thenReturn(new org.springframework.core.env.MutablePropertySources());
    environment.getPropertySources().addLast(propertySource);
    when(environment.containsProperty("app.old-property")).thenReturn(true);
    when(environment.getProperty("app.old-property")).thenReturn("true");

    // Mock du config loader avec une propriété dépréciée
    DeprecatedPropertiesConfigLoader configLoader = mock(DeprecatedPropertiesConfigLoader.class);
    when(configLoader.getDeprecatedKeys()).thenReturn(List.of("app.old-property"));
    when(configLoader.getDeprecatedWildcards()).thenReturn(List.of());
    when(configLoader.getGuideUrl()).thenReturn("https://ton-site.com/guide-migration");

    // Instancier le validator
    DeprecatedPropertiesValidator validator = new DeprecatedPropertiesValidator(environment, configLoader);
    validator.afterSingletonsInstantiated();

    // Capturer les logs
    ArgumentCaptor<ILoggingEvent> captor = ArgumentCaptor.forClass(ILoggingEvent.class);
    verify(mockAppender, atLeastOnce()).doAppend(captor.capture());

    List<ILoggingEvent> logs = captor.getAllValues();

    // Vérifier que le message de warning est bien généré
    boolean found = logs.stream().anyMatch(log ->
        log.getLevel() == Level.WARN &&
        log.getFormattedMessage().contains("app.old-property") &&
        log.getFormattedMessage().contains("guide de migration")
    );

    assertTrue(found, "Le message de warning pour 'app.old-property' n’a pas été détecté.");
}
}
