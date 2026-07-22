= Standard d'Observabilité Applicative : Traces, Logs & Métriques
:toc: left
:toclevels: 3
:icons: font
:source-highlighter: highlight.js
:sectnums:

== Executive Summary

Ce document définit les normes de développement et d'infrastructure pour garantir la traçabilité de bout en bout et la supervisabilité à travers l'ensemble de nos microservices (Spring Boot 3.5+, Java 25).

.Répartition des Responsabilités (Dev vs Infra)
[cols="1,2,2,2", options="header"]
|===
| Pilier | Technologie | Responsable | Destination
| **Traces** | OpenTelemetry Java Agent (v2.29.0) | **Infra** (Injection Agent JVM) | Zipkin / OpenTelemetry Collector
| **Logs** | Logback (Modèle externe standardisé) | **Infra** (Injection `logback.xml`) | Elasticsearch / Kibana
| **Métriques** | Micrometer + Spring Boot Actuator | **Dev** (Code) & **Infra** (Scrape) | Prometheus / Grafana
|===

---

== 1. Métriques : Configuration Micrometer & Prometheus (Grafana)

Côté applicatif, les métriques sont gérées par **Micrometer** via Spring Boot Actuator. L'infrastructure Prometheus vient gratter (*scrape*) le endpoint `/actuator/prometheus`.

=== A. Dépendances Maven (`pom.xml`)

[source,xml]
----
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
----

=== B. Configuration Spring Boot (`application.yml`)

[source,yaml]
----
management:
  endpoints:
    web:
      exposure:
        include: health, info, prometheus
  endpoint:
    prometheus:
      enabled: true
    health:
      show-details: always
  metrics:
    tags:
      application: ${spring.application.name}
    distribution:
      percentiles-histogram:
        http.server.requests: true

spring:
  threads:
    virtual:
      enabled: true
----

=== C. Métriques Métier Personnalisées (Dev)

Pour remonter des métriques métier spécifiques dans les dashboards Grafana, injectez `MeterRegistry` :

[source,java]
----
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class OrderBusinessService {

    private final Counter orderSuccessCounter;
    private final Timer orderProcessingTimer;

    public OrderBusinessService(MeterRegistry registry) {
        this.orderSuccessCounter = Counter.builder("orders.processed.total")
                .description("Nombre total de commandes traitées avec succès")
                .tag("type", "standard")
                .register(registry);

        this.orderProcessingTimer = Timer.builder("orders.processing.time")
                .description("Temps d'exécution du traitement des commandes")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    public void processOrder() {
        long startTime = System.nanoTime();
        try {
            // Traitement métier...
            orderSuccessCounter.increment();
        } finally {
            orderProcessingTimer.record(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
        }
    }
}
----

---

== 2. Modèle de Configuration Logs (Géré par l'Infra)

NOTE: **Information Dev :** Les développeurs n'ont **aucun fichier Logback à créer ou inclure** dans les projets. L'Infrastructure injecte un modèle standardisé lors du déploiement sur les environnements.

À titre d'information, voici le modèle injecté par l'Infra qui capture le MDC alimenté par l'Agent OTel :

[source,xml]
----
<!-- Fichier injecté au déploiement par l'Infra (-Dlogging.config=...) -->
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>

    <property name="CONSOLE_LOG_PATTERN" 
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - [trace_id=%X{trace_id:-} span_id=%X{span_id:-} user_id=%X{user_id:-anon}] - %msg%n"/>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${CONSOLE_LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
----

---

== 3. Propagation du Contexte Utilisateur (`user_id`)

Si le `trace_id` et le `span_id` sont injectés automatiquement par l'agent OTel dans les logs, le `user_id` applicatif doit être poussé dans le MDC par le code Java pour être capturé par le template Logback Infra.

[source,java]
----
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class UserContextMdcFilter extends OncePerRequestFilter {

    private static final String USER_ID_KEY = "user_id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) {
        try {
            String userId = request.getHeader("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                MDC.put(USER_ID_KEY, userId);
            }
            filterChain.doFilter(request, response);
        } finally {
            // Nettoyage systématique pour éviter les fuites de contexte entre requêtes
            MDC.remove(USER_ID_KEY);
        }
    }
}
----

---

== 4. Guide de Traitement des Traces par Cas d'Usage (Dev)

=== HTTP / OpenFeign
* **Statut** : 100% Automatique via Agent OTel.
* **Instruction Dev** : Aucune action requise. L'injection et l'extraction de l'en-tête W3C `traceparent` sont gérées par l'agent.

=== Virtual Threads (Java 25) & Asynchronisme (`@Async`)
* **Statut** : Automatique.
* **Instruction Dev** : Activer `spring.threads.virtual.enabled=true`. L'agent transmet le contexte de trace de manière transparente aux Virtual Threads.

=== Stream Parallèle (`parallelStream()`)
* **Statut** : Action Manuelle Requise.
* **Problématique** : `parallelStream()` s'exécute dans le `ForkJoinPool.commonPool()`, ce qui peut rompre le fil de la trace.

[source,java]
----
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.annotations.WithSpan;

@Service
public class StreamProcessingService {

    @WithSpan("process-items-stream")
    public void processBatch(List<String> items) {
        Context currentContext = Context.current();

        items.parallelStream().forEach(item -> {
            try (var scope = currentContext.makeCurrent()) {
                doWork(item);
            }
        });
    }

    private void doWork(String item) {
        log.info("Traitement de l'item : {}", item);
    }
}
----

=== JMS & `DefaultMessageListenerContainer` (DMLC)
* **Statut** : Automatique pour la propagation de trace + Config DMLC requise pour Virtual Threads.

[source,java]
----
@Configuration
public class JmsConfig {

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        // Déléguer l'exécution des listeners aux Virtual Threads
        factory.setTaskExecutor(Executors.newVirtualThreadPerTaskExecutor());
        factory.setConcurrency("5-20");
        return factory;
    }
}
----

=== Recherches Elasticsearch
* **Statut** : Automatique via l'Agent OTel.
* **Instruction Dev** : Utiliser les clients officiels (`elasticsearch-java`, `Spring Data Elasticsearch`). Les requêtes HTTP sous-jacentes sont interceptées et liées au span courant.

=== Gestion des Exceptions (`@ControllerAdvice`)
* **Statut** : Action Manuelle Requise.
* **Problématique** : Une exception gérée par un `@ControllerAdvice` ne fait pas échouer le Span HTTP au sens OTel si elle n'est pas explicite.

[source,java]
----
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorPayload> handleBusinessException(BusinessException ex) {
        // 1. Renseigner l'exception dans le Span OTel courant
        Span currentSpan = Span.current();
        currentSpan.recordException(ex);
        currentSpan.setStatus(StatusCode.ERROR, ex.getMessage());

        log.error("Erreur métier : {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                             .body(new ErrorPayload("BUSINESS_ERROR", ex.getMessage()));
    }
}
----

---

== 5. Matrice de Synthèse pour l'Équipe Dev

[cols="2,1,1,3", options="header"]
|===
| Cas d'usage | Support Agent / Framework | Action Code Dev | Consigne / Règle
| **HTTP / OpenFeign** | OTel Agent | Aucune | Traces automatiques.
| **Virtual Threads / `@Async`** | OTel Agent | Aucune | Activer `spring.threads.virtual.enabled=true`.
| **`parallelStream()`** | OTel Agent | **OUI** | Utiliser `try (var scope = Context.current().makeCurrent())`.
| **JMS (Listeners / DMLC)** | OTel Agent | **OUI (Config)** | Offloader le `TaskExecutor` DMLC sur Virtual Threads.
| **Elasticsearch** | OTel Agent | Aucune | Tracé automatiquement avec les clients officiels.
| **`@ControllerAdvice`** | OTel Agent | **OUI** | Exécuter `Span.current().recordException(ex)` et `setStatus()`.
| **Configuration Logback** | Template Infra | **AUCUNE** | Ne pas inclure de `logback-spring.xml` dans le repository.
| **Métriques Applicatives** | Micrometer | **OUI** | Injecter `MeterRegistry` pour les compteurs métier.
|===

---

== 6. Configuration de Lancement Déploiement (Référence Infra)

Pour référence, voici les options injectées au démarrage de la JVM sur les environnements :

[source,bash]
----
java -javaagent:/infra/agents/opentelemetry-javaagent-2.29.0.jar \
     -Dlogging.config=/infra/logback/logback-standard.xml \
     -Dotel.service.name=mon-microservice \
     -Dotel.traces.exporter=otlp \
     -Dotel.exporter.otlp.endpoint=http://otel-collector:4318 \
     -Dotel.exporter.otlp.protocol=http/protobuf \
     -Dotel.instrumentation.elasticsearch.capture-search-query=true \
     -Dotel.metrics.exporter=none \
     -jar app.jar
----
