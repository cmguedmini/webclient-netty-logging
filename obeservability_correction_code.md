Je vais raisonner en mode dossier de conception technique afin que vous puissiez directement créer une évolution du starter et la faire valider par l'équipe Architecture/Infra.

1. Starter Log : Corrections à apporter
1.1 Dépendances à supprimer
Avant
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2021.0.5</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-tracing-bridge-otel</artifactId>
            <version>1.0.12</version>
            <scope>import</scope>
            <type>pom</type>
        </dependency>
    </dependencies>
</dependencyManagement>

Correction

Suppression complète :

spring-cloud-dependencies 2021.0.5


et :

micrometer-tracing-bridge-brave


Le starter n'a plus besoin de Brave.

1.2 Dépendances du starter
Nouveau pom.xml
<dependencies>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-autoconfigure</artifactId>
        <scope>provided</scope>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <dependency>
        <groupId>jakarta.servlet</groupId>
        <artifactId>jakarta.servlet-api</artifactId>
        <scope>provided</scope>
    </dependency>

    <dependency>
        <groupId>io.opentelemetry</groupId>
        <artifactId>opentelemetry-api</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-core</artifactId>
        <optional>true</optional>
    </dependency>

</dependencies>

2. DriLogContextHolderFilter
Problème actuel

Aujourd'hui :

MDC.put("Component",springApplicationName);
MDC.put("originApp",header);


mais jamais :

traceId
spanId


Donc Kibana dépend du comportement interne de Micrometer/Brave.

Nouveau code complet
package com.dri.logging;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

@RequiredArgsConstructor
public class DriLogContextHolderFilter implements Filter {

    private static final String USER_ID = "userId";

    private static final String COMPONENT = "Component";

    private static final String ORIGIN_APP = "originApp";

    private static final String ANONYMOUS = "AnonymousUser";

    private static final String UNKNOWN = "x-sender_not-define";

    private final String applicationName;

    private final String senderHeader;

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain)
            throws IOException, ServletException {

        try {

            populateUser();

            populateComponent();

            populateOrigin((HttpServletRequest) request);

            populateTraceContext();

            chain.doFilter(request, response);

        }
        finally {

            MDC.remove("traceId");
            MDC.remove("spanId");
            MDC.remove(USER_ID);
            MDC.remove(COMPONENT);
            MDC.remove(ORIGIN_APP);
        }
    }

    private void populateComponent() {
        MDC.put(COMPONENT, applicationName);
    }

    private void populateUser() {

        String user = ANONYMOUS;

        if (SecurityContextHolder.getContext().getAuthentication() != null) {

            user = SecurityContextHolder
                    .getContext()
                    .getAuthentication()
                    .getName();
        }

        MDC.put(USER_ID, user);
    }

    private void populateOrigin(HttpServletRequest request) {

        String sender = request.getHeader(senderHeader);

        MDC.put(
                ORIGIN_APP,
                sender != null
                        ? sender
                        : UNKNOWN
        );
    }

    private void populateTraceContext() {

        SpanContext context =
                Span.current().getSpanContext();

        if (!context.isValid()) {
            return;
        }

        MDC.put(
                "traceId",
                context.getTraceId()
        );

        MDC.put(
                "spanId",
                context.getSpanId()
        );
    }
}

3. DriLogAutoConfiguration
Avant

Vous injectez :

private final Tracer tracer;


ce qui n'est plus nécessaire.

Nouveau code
@AutoConfiguration
@ConditionalOnProperty(
        name = "dri.starter.log.enabled",
        matchIfMissing = true)
@EnableConfigurationProperties(DriLogProperties.class)
public class DriLogAutoConfiguration {

    @Bean
    public FilterRegistrationBean<DriLogContextHolderFilter>
            loggingFilter(
                    DriLogProperties properties,
                    @Value("${spring.application.name}")
                    String applicationName) {

        FilterRegistrationBean<DriLogContextHolderFilter>
                registration =
                new FilterRegistrationBean<>();

        registration.setFilter(
                new DriLogContextHolderFilter(
                        applicationName,
                        properties.getSender()
                )
        );

        registration.addUrlPatterns(
                properties.getUrlPatterms()
        );

        registration.setOrder(
                properties.getContextFilterOrder()
        );

        return registration;
    }
}

4. AutoConfiguration.imports

Créer ou vérifier :

src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports


Contenu :

com.dri.logging.DriLogAutoConfiguration

5. DriLogProperties

Pas de changement obligatoire.

@ConfigurationProperties(prefix = "dri.starter.log")
@Getter
@Setter
public class DriLogProperties {

    public static final int CONTEXT_HOLDER_ORDER = 2;

    private boolean enabled = true;

    private String[] urlPatterms = {"/*"};

    private int contextFilterOrder = CONTEXT_HOLDER_ORDER;

    private String sender = "x-sender";
}

6. Projet A
Dépendances

Aucune obligation de modification.

Cependant je recommande de supprimer :

<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>


si elle n'est plus utilisée ailleurs.

Feign

Conserver.

@Bean
public MicrometerCapability micrometerCapability(
        MeterRegistry meterRegistry) {

    return new MicrometerCapability(meterRegistry);
}


Aucun impact sur les logs.

7. Projet B
Dépendances

Conserver :

<dependency>
    <groupId>io.github.openfeign</groupId>
    <artifactId>feign-micrometer</artifactId>
</dependency>


Aucun changement obligatoire.

Ajouter un interceptor Feign
Nouveau composant
@Configuration
public class FeignConfiguration {

    @Bean
    public RequestInterceptor senderHeaderInterceptor(
            @Value("${spring.application.name}")
            String applicationName) {

        return template ->
                template.header(
                        "x-sender",
                        applicationName
                );
    }
}

8. application.yml Projet A et B

Ajouter explicitement :

dri:
  starter:
    log:
      enabled: true
      sender: x-sender
      context-filter-order: 100

9. Vérification Infra

Le logback injecté doit utiliser :

%X{traceId}
%X{spanId}


Exemple :

<pattern>
%d{yyyy-MM-dd HH:mm:ss.SSS}
[%thread]
%-5level
traceId=%X{traceId}
spanId=%X{spanId}
component=%X{Component}
origin=%X{originApp}
user=%X{userId}
%logger
- %msg%n
</pattern>

10. Résumé des modifications par composant
Starter Log

✅ Supprimer Brave

micrometer-tracing-bridge-brave


✅ Ajouter

io.opentelemetry:opentelemetry-api


✅ Modifier

DriLogContextHolderFilter


pour injecter :

Span.current().getSpanContext()


dans :

MDC.put("traceId", ...)
MDC.put("spanId", ...)


✅ Supprimer

Tracer tracer


de l'AutoConfiguration.

Projet A

✅ Aucun changement fonctionnel obligatoire.

✅ Vérifier seulement que le nouveau starter est utilisé.

Projet B

✅ Utiliser le nouveau starter.

✅ Ajouter l'interceptor Feign x-sender.

✅ Vérifier que l'application démarre avec le même agent OTel.

Infra

✅ Vérifier que le pattern logback contient :

%X{traceId}
%X{spanId}


et pas :

%X{trace_id}
%X{span_id}


ni :

%X{X-B3-TraceId}
%X{X-B3-SpanId}


C'est le point le plus critique avant toute évolution du code. Si le logback de l'infra attend trace_id/span_id alors que votre starter remplit traceId/spanId, vous aurez exactement le symptôme observé sur le Projet B.
