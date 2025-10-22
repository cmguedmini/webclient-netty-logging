package com.example.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.*;

public class DeprecatedPropertiesLogger implements EnvironmentPostProcessor, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(DeprecatedPropertiesLogger.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        DeprecatedConfig config = loadDeprecatedConfig();

        Set<String> allPropertyNames = new HashSet<>();
        for (PropertySource<?> source : environment.getPropertySources()) {
            if (source.getSource() instanceof Map) {
                allPropertyNames.addAll(((Map<?, ?>) source.getSource()).keySet().stream()
                        .map(Object::toString)
                        .toList());
            }
        }

        // Vérification des propriétés exactes
        for (String property : config.properties) {
            if (environment.containsProperty(property)) {
                String value = environment.getProperty(property);
                logger.warn("⚠️ La propriété obsolète '{}' est utilisée avec la valeur '{}'. Veuillez la supprimer ou la remplacer. Consultez le guide de migration : {}", property, value, config.guide);
            }
        }

        // Vérification des wildcards
        for (String wildcard : config.wildcards) {
            String prefix = wildcard.replace("*", "");
            for (String propName : allPropertyNames) {
                if (propName.startsWith(prefix)) {
                    String value = environment.getProperty(propName);
                    logger.warn("⚠️ La propriété obsolète '{}' (match avec '{}') est utilisée avec la valeur '{}'. Veuillez la supprimer ou la remplacer. Consultez le guide de migration : {}", propName, wildcard, value, config.guide);
                }
            }
        }
    }

    private DeprecatedConfig loadDeprecatedConfig() {
        DeprecatedConfig config = new DeprecatedConfig();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("deprecated-properties.yml")) {
            if (input == null) {
                logger.warn("Fichier 'deprecated-properties.yml' introuvable.");
                return config;
            }
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(input);
            Map<String, Object> deprecated = (Map<String, Object>) data.get("deprecated");

            config.guide = (String) deprecated.get("guide");
            config.properties = (List<String>) deprecated.getOrDefault("properties", List.of());
            config.wildcards = (List<String>) deprecated.getOrDefault("wildcards", List.of());
        } catch (Exception e) {
            logger.error("Erreur lors du chargement de la configuration des propriétés obsolètes : ", e);
        }
        return config;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private static class DeprecatedConfig {
        public String guide;
        public List<String> properties = new ArrayList<>();
        public List<String> wildcards = new ArrayList<>();
    }
}

--------------------
deprecated:
  guide: "https://ton-site.com/guide-migration"
  properties:
    - app.old-property
    - legacy.timeout
  wildcards:
    - app.code.*
    - legacy.feature.*
----------------------------------
Déclaration dans spring.factories
Assure-toi que ce fichier existe :
src/main/resources/META-INF/spring.factories

Contenu :
org.springframework.boot.env.EnvironmentPostProcessor=\
com.example.config.DeprecatedPropertiesLogger
---------------------------------
<dependency>
    <groupId>org.yaml</groupId>
    <artifactId>snakeyaml</artifactId>
    <version>2.0</version>
</dependency>
-----------------



Merci pour la précision ! Voici deux **tests unitaires** clairs et simples :

1. ✅ **Test avec une propriété obsolète** → vérifie que le message de warning est bien affiché.
2. ✅ **Test avec une propriété valide** → vérifie qu’aucun message de warning n’est affiché.

---

## 🧪 Préparation : capturer les logs avec une `Appender` Mockito

On va utiliser **Mockito** pour intercepter les logs via une `Appender` mockée de Logback.

### 🧩 Ajoute cette dépendance dans ton `pom.xml` :

```xml
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.4.11</version>
    <scope>test</scope>
</dependency>
```

---

## 🧪 Classe de test complète

```java
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeprecatedPropertiesLoggerTest {

    private Logger logger;
    private Appender<ILoggingEvent> mockAppender;

    @BeforeEach
    void setup() {
        logger = (Logger) org.slf4j.LoggerFactory.getLogger(DeprecatedPropertiesLogger.class);
        mockAppender = mock(Appender.class);
        when(mockAppender.getName()).thenReturn("MOCK");
        logger.addAppender(mockAppender);
        logger.setLevel(Level.WARN);
    }

    @Test
    void shouldLogWarningForDeprecatedProperty() {
        ConfigurableEnvironment environment = mock(ConfigurableEnvironment.class);
        Map<String, Object> properties = Map.of("app.old-property", "true");
        MapPropertySource propertySource = new MapPropertySource("test", properties);

        when(environment.getPropertySources()).thenReturn(new org.springframework.core.env.MutablePropertySources());
        environment.getPropertySources().addLast(propertySource);
        when(environment.containsProperty("app.old-property")).thenReturn(true);
        when(environment.getProperty("app.old-property")).thenReturn("true");

        DeprecatedPropertiesLogger processor = new DeprecatedPropertiesLogger() {
            @Override
            protected DeprecatedConfig loadDeprecatedConfig() {
                DeprecatedConfig config = new DeprecatedConfig();
                config.guide = "https://ton-site.com/guide-migration";
                config.properties = List.of("app.old-property");
                config.wildcards = List.of();
                return config;
            }
        };

        processor.postProcessEnvironment(environment, mock(SpringApplication.class));

        ArgumentCaptor<ILoggingEvent> captor = ArgumentCaptor.forClass(ILoggingEvent.class);
        verify(mockAppender, atLeastOnce()).doAppend(captor.capture());

        boolean foundWarning = captor.getAllValues().stream()
            .anyMatch(event -> event.getLevel() == Level.WARN &&
                               event.getFormattedMessage().contains("app.old-property") &&
                               event.getFormattedMessage().contains("guide de migration"));

        assertTrue(foundWarning, "Le message de warning attendu n’a pas été trouvé.");
    }

    @Test
    void shouldNotLogWarningForValidPropertiesOnly() {
        ConfigurableEnvironment environment = mock(ConfigurableEnvironment.class);
        Map<String, Object> properties = Map.of("app.valid-property", "ok");
        MapPropertySource propertySource = new MapPropertySource("test", properties);

        when(environment.getPropertySources()).thenReturn(new org.springframework.core.env.MutablePropertySources());
        environment.getPropertySources().addLast(propertySource);
        when(environment.containsProperty("app.valid-property")).thenReturn(true);
        when(environment.getProperty("app.valid-property")).thenReturn("ok");

        DeprecatedPropertiesLogger processor = new DeprecatedPropertiesLogger() {
            @Override
            protected DeprecatedConfig loadDeprecatedConfig() {
                DeprecatedConfig config = new DeprecatedConfig();
                config.guide = "https://ton-site.com/guide-migration";
                config.properties = List.of("app.old-property");
                config.wildcards = List.of("legacy.*");
                return config;
            }
        };

        processor.postProcessEnvironment(environment, mock(SpringApplication.class));

        ArgumentCaptor<ILoggingEvent> captor = ArgumentCaptor.forClass(ILoggingEvent.class);
        verify(mockAppender, never()).doAppend(captor.capture());
    }
}
```

---

## ✅ Ce que ces tests font :

- **Premier test** : vérifie qu’un message de warning est bien loggé quand une propriété obsolète est présente.
- **Deuxième test** : vérifie qu’aucun message de warning n’est loggé quand seules des propriétés valides sont utilisées.

---

Souhaites-tu que je t’aide à intégrer ces tests dans ton projet ou à les adapter à une autre structure de code ?
