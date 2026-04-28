Voici le code complet et consolidé de votre utilitaire de maintenance JMS, prêt pour une utilisation en environnement professionnel avec Java 21 et Spring Boot 3.

### 1. Code Source de l'Utilitaire

#### `JmsMaintenanceService.java`
Gère la logique d'arrêt et de démarrage de tous les containers détectés.

```java
package com.example.jms.maintenance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service gérant l'état global des listeners JMS.
 * L'utilisation de Virtual Threads (Java 21) est implicitement supportée 
 * si activée au niveau de l'application.
 */
@Service
@ConditionalOnProperty(name = "jms.maintenance.enabled", havingValue = "true", matchIfMissing = true)
public class JmsMaintenanceService {
    private static final Logger log = LoggerFactory.getLogger(JmsMaintenanceService.class);
    
    private final JmsListenerEndpointRegistry registry;
    private final AtomicBoolean maintenanceMode = new AtomicBoolean(false);

    public JmsMaintenanceService(JmsListenerEndpointRegistry registry) {
        this.registry = registry;
    }

    public void setMaintenanceMode(boolean active) {
        maintenanceMode.set(active);
        log.info("Action Maintenance JMS demandée : {}", active ? "STOP" : "START");
        
        registry.getListenerContainers().forEach(container -> {
            if (active && container.isRunning()) {
                container.stop();
                log.debug("Container {} stoppé.", container);
            } else if (!active && !container.isRunning()) {
                container.start();
                log.debug("Container {} démarré.", container);
            }
        });
    }

    public boolean isMaintenanceMode() {
        return maintenanceMode.get();
    }
}
```

#### `JmsMaintenanceEndpoint.java`
L'interface Actuator pour exposer le service via HTTP/JMX.

```java
package com.example.jms.maintenance;

import org.springframework.boot.actuate.endpoint.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Endpoint(id = "jms-maintenance")
@ConditionalOnProperty(name = "jms.maintenance.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(JmsMaintenanceService.class)
public class JmsMaintenanceEndpoint {

    private final JmsMaintenanceService maintenanceService;

    public JmsMaintenanceEndpoint(JmsMaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @ReadOperation
    public Map<String, Object> status() {
        return Map.of(
            "maintenanceMode", maintenanceService.isMaintenanceMode(),
            "status", maintenanceService.isMaintenanceMode() ? "PAUSED" : "RUNNING"
        );
    }

    @WriteOperation
    public String toggleMaintenance(@Selector String action) {
        return switch (action.toLowerCase()) {
            case "on" -> {
                maintenanceService.setMaintenanceMode(true);
                yield "Mode maintenance ACTIVÉ : Consommation JMS suspendue.";
            }
            case "off" -> {
                maintenanceService.setMaintenanceMode(false);
                yield "Mode maintenance DÉSACTIVÉ : Consommation JMS reprise.";
            }
            default -> "Action invalide. Utilisez 'on' ou 'off'.";
        };
    }
}
```

---

### 2. Documentation Technique (Format AsciiDoc)

Vous pouvez copier ce contenu dans un fichier `README.adoc` ou dans votre base de connaissance.

```asciidoc
= Documentation Technique : Utilitaire JMS Maintenance
:version: 1.0.0
:toc: left

== Présentation
Cet utilitaire fournit un mécanisme de "coupe-circuit" global pour les listeners JMS. Il permet d'arrêter ou de redémarrer à chaud tous les consommateurs de messages (IBM MQ, ActiveMQ, etc.) via un endpoint Spring Boot Actuator.

== Configuration requise
* Java 21
* Spring Boot 3.2+
* Dépendance `spring-boot-starter-actuator` présente dans le projet cible.

== Intégration étape par étape

=== 1. Dépendance Maven
Ajoutez l'artefact utilitaire à votre `pom.xml`.

=== 2. Activation des composants
Importez l'utilitaire dans votre configuration Spring :

[source,java]
----
@Configuration
@Import({JmsMaintenanceService.class, JmsMaintenanceEndpoint.class})
public class MyJmsConfig { }
----

=== 3. Propriétés de configuration (application.properties)
Pour exposer l'endpoint et gérer son activation :

[source,properties]
----
# Activer/Désactiver l'utilitaire au démarrage (défaut: true)
jms.maintenance.enabled=true

# Exposer l'endpoint sur le Web
management.endpoints.web.exposure.include=health,info,jms-maintenance
----

== Utilisation (Opérations)

=== Vérifier le statut
`GET /actuator/jms-maintenance`

=== Activer la maintenance (Arrêt des flux)
`POST /actuator/jms-maintenance/on`

=== Désactiver la maintenance (Reprise des flux)
`POST /actuator/jms-maintenance/off`

== Sécurité
Il est impératif que l'accès à `/actuator/jms-maintenance` soit protégé par Spring Security, car il permet de stopper le traitement des messages métier.
```

---

### 3. Test Unitaire (Validation du comportement)

```java
package com.example.jms.maintenance;

import org.junit.jupiter.api.Test;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.jms.listener.MessageListenerContainer;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class JmsMaintenanceServiceTest {

    @Test
    void testToggleMaintenanceMode() {
        JmsListenerEndpointRegistry registry = mock(JmsListenerEndpointRegistry.class);
        MessageListenerContainer container = mock(MessageListenerContainer.class);
        
        when(registry.getListenerContainers()).thenReturn(List.of(container));
        when(container.isRunning()).thenReturn(true);

        JmsMaintenanceService service = new JmsMaintenanceService(registry);

        // Activation
        service.setMaintenanceMode(true);
        verify(container).stop();
        assertTrue(service.isMaintenanceMode());

        // Désactivation
        when(container.isRunning()).thenReturn(false);
        service.setMaintenanceMode(false);
        verify(container).start();
        assertFalse(service.isMaintenanceMode());
    }
}
```

### Pourquoi cette version est optimale ?
* **Java 21 Ready** : Utilise les `switch expressions` et une structure compatible avec les Virtual Threads.
* **Contrôle Total** : La propriété `jms.maintenance.enabled` permet aux équipes de désactiver l'outil par environnement.
* **Robuste** : Le `@ConditionalOnBean` évite que l'endpoint n'essaie de démarrer si le service a été désactivé par propriété.

Voici une proposition de paragraphe technique pour la clôture ou la description de votre ticket JIRA. Ce texte résume l'architecture technique et les choix structurants de l'implémentation :

---

### Description de l'implémentation (Ticket JIRA)

L'implémentation repose sur la création d'un utilitaire léger et modulaire exploitant les capacités d'introspection de **Spring JMS** et de **Spring Boot Actuator**. Le cœur de la solution est le `JmsMaintenanceService`, qui interagit directement avec le `JmsListenerEndpointRegistry` pour piloter l'ensemble des `MessageListenerContainers` actifs (compatible IBM MQ et ActiveMQ). Un point de terminaison personnalisé, `@Endpoint(id = "jms-maintenance")`, a été exposé pour permettre le basculement global à chaud via des requêtes HTTP (POST `/on` et `/off`). Pour garantir une flexibilité maximale, l'activation de cet utilitaire est conditionnée par la propriété `jms.maintenance.enabled` (utilisant `@ConditionalOnProperty`), permettant ainsi aux équipes de désactiver la fonctionnalité par environnement sans modification de code. L'ensemble est optimisé pour **Java 21** et **Spring Boot 3**, assurant une compatibilité avec les threads virtuels et les derniers standards de sécurité de l'Actuator.

---

### Résumé technique pour le ticket :
* **Composants clés** : `JmsMaintenanceService` (logique), `JmsMaintenanceEndpoint` (interface).
* **Mode d'activation** : Explicite via `@Import` et configurable via `application.properties`.
* **Compatibilité** : Agnostique du broker (Standard JMS).
* **Sécurité** : Intégration native dans le cycle de vie Actuator.
