Rapport d'Audit Technique
Analyse de la gestion de la traçabilité distribuée et de l'observabilité
Projets A et B utilisant le starter de log interne

Date : Juillet 2026
 Auteur : Équipe Architecture / Expertise Spring Boot & Observability

1. Contexte

Deux applications Spring Boot (Projet A et Projet B) utilisent un starter interne de logging destiné à enrichir les logs applicatifs et faciliter l'observabilité.

Les deux applications :

utilisent Spring Boot 3.5.x ;
utilisent Feign pour les appels sortants ;
sont instrumentées via un agent Java OpenTelemetry injecté par l'équipe Infrastructure ;
exportent les traces vers Zipkin ;
disposent d'un logback.xml injecté au runtime par l'équipe Infrastructure.

L'objectif est de permettre la corrélation entre :

les logs applicatifs (Kibana) ;
les traces distribuées (Zipkin).

Cette corrélation repose sur la présence des attributs :

traceId
spanId


dans les logs.

2. État actuel observé
Projet A
Fonctionnement observé

Le Projet A affiche correctement :

traceId
spanId


dans Kibana.

Les traces sont également visibles dans Zipkin.

Instrumentation observée

Dans Zipkin :

otel.library.name = io.opentelemetry.tomcat-10.0
otel.scope.name   = io.opentelemetry.tomcat-10.0


Le projet utilise :

<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>


et :

sleuth:
  baggage:
    remote-fields:
      - x-sender

Conclusion

Le contexte de traçage est correctement propagé jusqu'au MDC Logback.

Les logs sont corrélables avec les traces distribuées.

Projet B
Fonctionnement observé

Le Projet B :

✅ génère correctement des traces OpenTelemetry.

✅ exporte les traces vers Zipkin.

❌ ne publie pas les champs :

traceId
spanId


dans Kibana.

Instrumentation observée

Dans Zipkin :

otel.library.name = io.opentelemetry.undertow-1.4
otel.scope.name   = io.opentelemetry.undertow-1.4


Le projet repose sur :

<dependency>
    <groupId>io.github.openfeign</groupId>
    <artifactId>feign-micrometer</artifactId>
</dependency>

Conséquence opérationnelle

L'exploitation doit actuellement :

récupérer le traceId depuis le Projet A ;
rechercher manuellement la trace dans Zipkin ;
naviguer dans les spans pour identifier les traitements du Projet B.

Cette démarche :

augmente le temps d'analyse ;
complexifie les investigations ;
réduit l'efficacité du monitoring.
3. Analyse du starter de log
Fonctionnement actuel

Le starter contient le filtre :

DriLogContextHolderFilter


qui alimente le MDC avec :

MDC.put("userId", user);
MDC.put("Component", springApplicationName);
MDC.put("originApp", header);

Observation importante

Le starter ne renseigne jamais :

traceId
spanId


dans le MDC.

Le comportement observé repose donc exclusivement sur :

OpenTelemetry Agent
        +
Micrometer
        +
Brave
        +
Logback


pour remplir automatiquement le MDC.

4. Défaillances identifiées
Défaillance n°1
Couplage implicite au comportement de Brave

Le starter repose indirectement sur :

micrometer-tracing-bridge-brave


pour enrichir le MDC.

Or :

cette opération est implicite ;
elle dépend de l'environnement d'exécution ;
elle n'est pas maîtrisée par le starter.

Conséquence :

Le comportement est différent entre les applications.

Défaillance n°2
Dépendances incompatibles avec Spring Boot 3.5

Le starter importe :

spring-cloud-dependencies 2021.0.5


Cette version a été conçue pour :

Spring Boot 2.6.x


et non pour :

Spring Boot 3.5.x


Elle appartient à l'écosystème :

Spring Cloud Sleuth


désormais remplacé par :

Micrometer Observation
Micrometer Tracing
OpenTelemetry


dans Spring Boot 3.

Défaillance n°3
Absence d'intégration native OpenTelemetry

Le starter ne récupère jamais directement :

Span.current()


ni :

SpanContext


via l'API OpenTelemetry.

Le starter ignore donc complètement le mécanisme principal de tracing utilisé aujourd'hui.

Défaillance n°4
Risque de comportement différent selon le serveur web

Le Projet A fonctionne sur :

Tomcat


Le Projet B fonctionne sur :

Undertow


Les deux serveurs n'ont pas exactement les mêmes mécanismes internes de gestion du contexte d'exécution.

Le comportement du MDC peut donc varier.

Le dysfonctionnement observé sur le Projet B est cohérent avec cette hypothèse.

5. Cause racine probable

La cause racine la plus probable est :

Le starter n'enrichit pas lui-même le MDC avec les informations de tracing et dépend d'un mécanisme indirect issu de Brave/Micrometer dont le comportement n'est pas garanti de manière identique entre Tomcat et Undertow.

En conséquence :

Projet A
→ MDC enrichi
→ traceId visible

Projet B
→ MDC non enrichi
→ traceId absent


alors que les traces existent bien dans Zipkin.

6. Solution recommandée
Principe

Le starter doit devenir totalement autonome pour l'alimentation du MDC.

Il ne doit plus dépendre :

de Brave
de Sleuth
de mécanismes implicites Micrometer


mais exploiter directement :

OpenTelemetry API

7. Évolutions techniques proposées
Évolution 1
Suppression de Brave

Retrait :

micrometer-tracing-bridge-brave


du starter.

Évolution 2
Ajout d'OpenTelemetry API

Ajout :

<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-api</artifactId>
</dependency>

Évolution 3
Enrichissement explicite du MDC

Ajout dans le filtre :

SpanContext context =
        Span.current().getSpanContext();

if (context.isValid()) {

    MDC.put(
        "traceId",
        context.getTraceId());

    MDC.put(
        "spanId",
        context.getSpanId());
}

Évolution 4
Suppression de la dépendance au Tracer Micrometer

Suppression dans :

DriLogAutoConfiguration


de :

Tracer tracer


La récupération du contexte sera assurée directement par OpenTelemetry.

Évolution 5
Standardisation du mécanisme pour tous les projets

Le même starter sera utilisé pour :

Tomcat ;
Undertow ;
futurs projets.

Le comportement deviendra indépendant du moteur web.

8. Bénéfices attendus
Bénéfices fonctionnels
Kibana

Les logs du Projet B contiendront :

traceId
spanId


de façon systématique.

Corrélation Log ↔ Trace

Depuis Kibana :

traceId


pourra être directement recherché dans :

Zipkin


sans dépendre du Projet A.

Réduction du temps d'analyse

Suppression des recherches croisées :

Projet A
→ Zipkin
→ Projet B

9. Risques identifiés
Risque faible

Le changement ne modifie pas :

les échanges métier ;
les API ;
Feign ;
Zipkin ;
l'agent OpenTelemetry.
Risque principal

Le logback fourni par l'équipe Infrastructure doit utiliser :

%X{traceId}
%X{spanId}


Si le pattern utilise :

%X{trace_id}
%X{span_id}


ou :

%X{X-B3-TraceId}


alors les nouvelles informations ne seront pas visibles.

Une vérification Infra est nécessaire avant mise en production.

10. Recommandation du comité d'architecture

Il est recommandé de :

faire évoluer le starter log vers une intégration OpenTelemetry native ;
supprimer la dépendance historique à Brave ;
enrichir explicitement le MDC avec traceId et spanId ;
valider le pattern Logback utilisé par l'équipe Infrastructure ;
déployer le nouveau starter sur le Projet B puis progressivement sur l'ensemble des applications Spring Boot 3.x.
Conclusion

La traçabilité distribuée fonctionne actuellement sur les deux applications puisque les traces sont visibles dans Zipkin. Le dysfonctionnement concerne uniquement l'enrichissement des logs avec les identifiants de corrélation (traceId et spanId). La solution proposée consiste à rendre le starter responsable de l'alimentation du MDC via l'API OpenTelemetry standard, garantissant ainsi un comportement homogène et pérenne sur l'ensemble du parc applicatif Spring Boot 3.5.x.
