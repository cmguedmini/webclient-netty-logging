Voici un document complet, prêt à être posé dans ton repo, avec une section Références en bas.

```markdown
# Migration Spring Boot 2.7.x → 3.5.x  
## Changements de propriétés (Core, Batch, JMS, Security, Data, Actuator)

Public cible : développeurs et intégrateurs de la plateforme.

Stack concernée : Spring Core, Spring Batch, JMS, Spring Security, Spring Data, Actuator.

---

## 1. Contexte et stratégie de migration

### 1.1. Périmètre technique

- Version de départ : **Spring Boot 2.7.x** (dernière lignée 2.x LTS). [page:2]  
- Version cible : **Spring Boot 3.5.x**, basée sur Spring Framework 6.x et Jakarta EE (Java 17 minimum). [page:2]  

Implications directes pour la configuration :

- Passage massif de `javax.*` → `jakarta.*` côté APIs (Servlet, JPA, JMS, etc.). [page:2]  
- Suppression des propriétés dépréciées en 2.x au fil des versions 3.0 → 3.5 (liste dans la doc “Deprecated Application Properties”). [page:1]  

### 1.2. Méthode recommandée pour les propriétés

1. **Mettre à jour en dernier 2.7.x** avant de viser 3.x (stabilité des dépréciations). [page:2]  
2. **Ajouter temporairement** la dépendance `spring-boot-properties-migrator` : [page:2]  

   Maven :  
   ```xml
   <dependency>
     <groupId>org.springframework.boot</groupId>
     <artifactId>spring-boot-properties-migrator</artifactId>
     <scope>runtime</scope>
   </dependency>
   ```  

   Gradle :  
   ```kotlin
   runtimeOnly("org.springframework.boot:spring-boot-properties-migrator")
   ```  

3. Démarrer l’application, analyser les logs :  
   - Propriétés **renommées** avec nouvelle clé proposée.  
   - Propriétés **supprimées** ou **ignorées**. [page:2]  
4. Corriger tous les `application-*.yml/properties` + variables d’environnement.  
5. **Retirer** la dépendance `spring-boot-properties-migrator` une fois la migration stabilisée. [page:2]  

---

## 2. Changements “Core” Spring Boot

### 2.1. Propriétés dépréciées / supprimées

- Spring Boot 3.x supprime les propriétés marquées comme dépréciées en 2.x ; la liste officielle est disponible dans “Deprecated Application Properties”. [page:1][page:2]  
- Le `properties-migrator` est le moyen recommandé pour identifier précisément les propriétés à migrer dans votre base 2.7.x. [page:2]  

### 2.2. Serveur Web : taille d’en-têtes HTTP

- `server.max-http-header-size` était gérée de manière incohérente entre serveurs embarqués (Tomcat vs Jetty/Netty/Undertow). [page:2]  
- Elle est dépréciée puis supprimée au profit de :  
  - `server.max-http-request-header-size` (appliquée uniquement aux en‑têtes **de requête**, tous serveurs). [page:2]  

**Action plateforme**  
- Rechercher `server.max-http-header-size` et remplacer par `server.max-http-request-header-size` dans tous les profils. [page:2]  

---

## 3. Actuator

### 3.1. Exposition des endpoints (HTTP/JMX)

- Par défaut, seul l’endpoint `health` est exposé via HTTP et JMX, pour alignement des comportements. [page:2]  
- Contrôle via :  
  - `management.endpoints.web.exposure.include` / `exclude`  
  - `management.endpoints.jmx.exposure.include` / `exclude`  

**Action plateforme**  
- Repasser les listes d’`include`/`exclude` pour s’assurer que :  
  - les endpoints utilisés existent encore,  
  - rien de sensible n’est exposé par défaut. [page:2]  

### 3.2. Renommage `httptrace` → `httpexchanges`

- L’endpoint `/actuator/httptrace` est renommé en `/actuator/httpexchanges`. [page:2]  
- Les classes associées sont renommées (`HttpTraceRepository` → `HttpExchangeRepository`, etc.). [page:2]  

**Action plateforme**  
- Mettre à jour :  
  - les propriétés `management.endpoints.web.exposure.include` pour utiliser `httpexchanges`,  
  - les URLs dans APIM / proxies / dashboards qui référence `/actuator/httptrace`. [page:2]  

### 3.3. Sanitization des endpoints `/env` et `/configprops`

- Les endpoints `/actuator/env` et `/actuator/configprops` contiennent des valeurs sensibles. [page:2]  
- À partir de Spring Boot 3, **toutes** les valeurs sont masquées par défaut (avant : masquage basé sur un pattern de nom de propriété). [page:2]  
- Deux propriétés pilotent le comportement :  
  - `management.endpoint.env.show-values`  
  - `management.endpoint.configprops.show-values`  
- Valeurs possibles : `NEVER` (défaut), `ALWAYS`, `WHEN_AUTHORIZED`. [page:2]  

**Action plateforme**  
- Définir une politique explicite par environnement :  
  - PROD : `NEVER`  
  - PREPROD / RECETTE interne : `WHEN_AUTHORIZED`  
  - LOCAL / DEV : éventuellement `ALWAYS` (attention à la capture de logs). [page:2]  

### 3.4. ObjectMapper Actuator isolé

- Les endpoints Actuator utilisent désormais un `ObjectMapper` isolé pour générer les réponses JSON, afin de garantir des réponses stables. [page:2]  
- Propriété de contrôle : `management.endpoints.jackson.isolated-object-mapper` (par défaut `true`). [page:2]  

**Action plateforme**  
- Si vos customisations Jackson globales sont censées impacter les réponses Actuator, deux options :  
  - réappliquer ces customisations via les mécanismes Actuator,  
  - désactiver l’isolement avec `management.endpoints.jackson.isolated-object-mapper=false`. [page:2]  

---

## 4. Metrics / Micrometer

### 4.1. Nouveau schéma de propriétés d’export

- Les propriétés d’export de metrics changent de préfixe : [page:2]  
  - **Avant** : `management.metrics.export.<backend>.*`  
  - **Après** : `management.<backend>.metrics.export.*`  

**Exemple Prometheus**  
- Avant : `management.metrics.export.prometheus.enabled=true`  
- Après : `management.prometheus.metrics.export.enabled=true` [page:2]  

**Action plateforme**  
- Renommer toutes les propriétés `management.metrics.export.*` vers `management.<backend>.metrics.export.*` (Prometheus, Graphite, Influx, etc.). [page:2]  

---

## 5. Spring Data

### 5.1. Réservation du préfixe `spring.data`

- Le préfixe `spring.data.*` est désormais réservé aux modules Spring Data. [page:2]  
- Toute propriété sous `spring.data.*` implique la présence de Spring Data sur le classpath. [page:2]  

**Action plateforme**  
- Vérifier qu’aucune propriété “maison” n’utilise `spring.data.*` comme préfixe, et si oui, les renommer (ex. `platform.data.*`). [page:2]  

### 5.2. Mouvement de propriétés Redis / Cassandra (si utilisés)

- Cassandra : les propriétés passent de `spring.data.cassandra.*` à `spring.cassandra.*`. [page:2]  
- Redis : les propriétés passent de `spring.redis.*` à `spring.data.redis.*`. [page:2]  

**Action plateforme**  
- Si Redis/Cassandra sont utilisés, appliquer les mappings ci‑dessus ou s’appuyer sur `spring-boot-properties-migrator` pour les détecter. [page:2]  

---

## 6. Spring Security

### 6.1. Dispatcher types du filtre de sécurité

- Spring Boot 3 s’appuie sur Spring Security 6, qui applique l’autorisation sur tous les `DispatcherType` par défaut. [page:2]  
- Boot configure donc le filtre de sécurité pour être appelé sur chaque `DispatcherType`. [page:2]  
- La propriété `spring.security.filter.dispatcher-types` permet de personnaliser cette liste. [page:2]  

**Action plateforme**  
- Si la plateforme dépend d’un comportement spécifique sur `FORWARD`, `ERROR`, `ASYNC`, vérifier et éventuellement fixer explicitement `spring.security.filter.dispatcher-types`. [page:2]  

### 6.2. SAML2 (si utilisé)

- Les propriétés de configuration SAML2 sous :  
  - `spring.security.saml2.relyingparty.registration.{id}.identity-provider.*`  
  sont supprimées. [page:2]  
- Elles sont remplacées par :  
  - `spring.security.saml2.relyingparty.registration.{id}.asserting-party.*` [page:2]  

**Action plateforme**  
- Rechercher toutes les propriétés SAML2 `identity-provider` et les migrer vers `asserting-party`. [page:2]  

---

## 7. Spring Batch 5 (via Spring Boot 3)

### 7.1. `@EnableBatchProcessing` vs auto-configuration Boot

- Avec Spring Batch 5 et Boot 3, `@EnableBatchProcessing` n’est plus requis pour déclencher l’auto-config Spring Boot. [page:2][web:7]  
- Si un bean `@EnableBatchProcessing` ou une classe qui étend `DefaultBatchConfiguration` est présent, Boot se met en retrait et vous prenez la main sur toute la configuration Batch. [page:2]  

**Action plateforme**  
- Choisir une stratégie claire :  
  - **Auto-config Boot** : supprimer `@EnableBatchProcessing` et laisser Boot configurer Batch.  
  - **Config custom** : garder `@EnableBatchProcessing` / `DefaultBatchConfiguration` et assumer l’intégralité de la configuration. [page:2]  

### 7.2. Propriété `spring.batch.job.name` (lancement de jobs)

- Le lancement automatique de **plusieurs** jobs au démarrage n’est plus supporté. [page:2]  
- Si plusieurs jobs sont trouvés dans le contexte, la propriété `spring.batch.job.name` doit indiquer lequel lancer. [page:2]  

**Exemple**  
```properties
spring.batch.job.enabled=true
spring.batch.job.name=myMainJob
```  

**Action plateforme**  
- Pour chaque module Batch avec plusieurs jobs définis :  
  - ajouter `spring.batch.job.name` dans la configuration,  
  - ou désactiver l’auto-start (`spring.batch.job.enabled=false`) et piloter les jobs via scheduler externe (Quartz, orchestrateur, etc.). [page:2]  

### 7.3. Migration de schéma BDD Batch

- Spring Batch 5 introduit des modifications de schéma (tables JOB/STEP, colonnes ajoutées/supprimées/modifiées). [web:13]  
- Un script de migration BDD est nécessaire pour faire évoluer la base existante en production. [web:13]  

**Action plateforme**  
- Intégrer la migration SQL Batch 4.x → 5.x dans les scripts de release, avec tests de reprise de jobs interrompus. [web:13]  

---

## 8. JMS

### 8.1. Propriétés JMS et Jakarta

- Le passage `javax.jms` → `jakarta.jms` affecte principalement le code et les dépendances, pas un grand nombre de propriétés Spring Boot spécifiques. [page:2]  
- Les propriétés d’URL, credentials, pool, etc. restent similaires, mais certaines clés peuvent apparaître dépréciées dans la doc “Deprecated Application Properties”. [page:1]  

**Action plateforme**  
- Lancer la plateforme avec `spring-boot-properties-migrator` et vérifier les diagnostics pour les propriétés JMS existantes (ActiveMQ/Artemis, etc.). [page:1][page:2]  

---

## 9. Checklist de migration des propriétés (2.7.x → 3.5.x)

1. **Monter en dernier Spring Boot 2.7.x** sur tous les modules (stabilité des dépréciations). [page:2]  
2. **Ajouter `spring-boot-properties-migrator`** en runtime et démarrer chaque module. [page:2]  
3. **Corriger toutes les propriétés** signalées (Core, Web, JMS, Data, Security, Actuator). [page:1][page:2]  
4. **Actuator** :  
   - vérifier exposition HTTP/JMX,  
   - migrer `httptrace` → `httpexchanges`,  
   - régler `management.endpoint.env.show-values` et `management.endpoint.configprops.show-values`. [page:2]  
5. **Metrics** : migrer `management.metrics.export.*` → `management.<backend>.metrics.export.*`. [page:2]  
6. **Spring Data** :  
   - ne pas utiliser `spring.data.*` pour des propriétés “maison”,  
   - appliquer les mappings Redis/Cassandra si concernés. [page:2]  
7. **Security** :  
   - vérifier `spring.security.filter.dispatcher-types`,  
   - migrer SAML2 `identity-provider` → `asserting-party` si SAML2 utilisé. [page:2]  
8. **Batch** :  
   - clarifier l’usage de `@EnableBatchProcessing`,  
   - ajouter `spring.batch.job.name` si plusieurs jobs,  
   - appliquer la migration de schéma BDD Batch. [page:2][web:13]  
9. **JMS** :  
   - contrôler qu’aucune propriété JMS n’est dépréciée/obsolète via le migrator. [page:1]  
10. **Retirer `spring-boot-properties-migrator`** une fois la configuration stabilisée. [page:2]  

---

## 10. Annexe — Tableau synthétique des principaux renommages

| Domaine      | Ancienne propriété (2.7.x)                              | Nouvelle propriété / état (3.5.x)                                         |
|-------------|----------------------------------------------------------|---------------------------------------------------------------------------|
| Serveur web | `server.max-http-header-size`                           | `server.max-http-request-header-size`                                    |
| Actuator    | Endpoint `/actuator/httptrace`                          | Endpoint `/actuator/httpexchanges`                                       |
| Actuator    | (visibilité implicite `/env`, `/configprops`)           | `management.endpoint.env.show-values`, `management.endpoint.configprops.show-values` |
| Actuator    | `management.metrics.export.<backend>.*`                 | `management.<backend>.metrics.export.*`                                  |
| Data        | Propriétés “maison” sous `spring.data.*`                | À renommer sous un préfixe applicatif (ex. `platform.*`)                 |
| Data        | `spring.data.cassandra.*`                               | `spring.cassandra.*`                                                     |
| Data        | `spring.redis.*`                                       | `spring.data.redis.*`                                                    |
| Security    | `spring.security.saml2.*.identity-provider.*`           | `spring.security.saml2.*.asserting-party.*`                              |
| Batch       | (lancement implicite de plusieurs jobs au démarrage)    | `spring.batch.job.name` obligatoire si plusieurs jobs                    |

---

## 11. Références

- Spring Boot 3.0 Migration Guide (GitHub Spring)  
  https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide [page:2]  

- Upgrading Spring Boot (doc officielle)  
  https://docs.spring.io/spring-boot/upgrading.html [web:14]  

- Deprecated Application Properties (doc officielle)  
  https://docs.spring.io/spring-boot/appendix/deprecated-application-properties/index.html [page:1]  

- Common Application Properties (liste complète des propriétés Boot)  
  https://docs.spring.io/spring-boot/appendix/application-properties/index.html [web:23]  

- Spring Batch 5.0 Migration Guide  
  https://github.com/spring-projects/spring-batch/wiki/Spring-Batch-5.0-Migration-Guide [web:13]  

- OpenRewrite – Migrate to Spring Batch 5.0 from 4.3  
  https://docs.openrewrite.org/recipes/java/spring/batch/springbatch4to5migration [web:7]  

- Baeldung – Migrate Application From Spring Boot 2 to Spring Boot 3  
  https://www.baeldung.com/spring-boot-3-migration [web:1]  

- Spring Boot Properties Migrator – Article explicatif  
  https://bootcamptoprod.com/spring-boot-properties-migrator/ [web:18]  
```

Voici une **fiche mémo courte** pour les intégrateurs, orientée “à coller dans le runbook de migration”.

```markdown
# Fiche mémo – Migration Spring Boot 2.7.x → 3.5.x  
## Focus : propriétés de configuration

---

## 1. Règles générales

- Java minimum : 17. [web:14]  
- Basculer tout ce qui est `javax.*` vers `jakarta.*` (code/dépendances). [web:1]  
- Utiliser **spring-boot-properties-migrator** pour détecter les propriétés renommées/supprimées. [web:14]  

---

## 2. Checklist intégrateur (à dérouler)

1. Monter tous les modules en **2.7.x dernière** si ce n’est pas déjà le cas. [web:14]  
2. Ajouter `spring-boot-properties-migrator` en `runtime`, démarrer chaque module, collecter les logs. [web:14]  
3. Corriger toutes les propriétés signalées (Core, Web, Data, Security, Actuator, JMS). [web:14]  
4. Adapter Actuator : exposition, endpoints renommés, sanitization (cf. §3). [web:2]  
5. Adapter les propriétés metrics : nouveau préfixe `management.<backend>.metrics.export.*`. [web:2]  
6. Vérifier préfixes Spring Data (`spring.data.*` réservé, Redis/Cassandra). [web:2]  
7. Adapter Spring Security (dispatcher-types, SAML2 le cas échéant). [web:2]  
8. Adapter Spring Batch (job name, schéma BDD, `@EnableBatchProcessing`). [web:13]  
9. Vérifier JMS via les diagnostics du migrator. [web:18]  
10. Retirer `spring-boot-properties-migrator` une fois la config stabilisée. [web:14]  

---

## 3. Points clés par domaine

### 3.1. Serveur / Web

- `server.max-http-header-size` → **remplacé par** `server.max-http-request-header-size`. [web:2]  

### 3.2. Actuator

- Exposition par défaut : seul `health` est exposé. [web:2]  
- Endpoint renommé : `/actuator/httptrace` → `/actuator/httpexchanges`. [web:2]  
- Sanitization `/env` et `/configprops` :  
  - Tout est masqué par défaut.  
  - Utiliser :  
    - `management.endpoint.env.show-values`  
    - `management.endpoint.configprops.show-values`  
    - Valeurs : `NEVER` (défaut) / `ALWAYS` / `WHEN_AUTHORIZED`. [web:2]  
- ObjectMapper spécifique Actuator :  
  - `management.endpoints.jackson.isolated-object-mapper=true` (par défaut). [web:2]  

### 3.3. Metrics

- Ancien schéma : `management.metrics.export.<backend>.*`  
- Nouveau schéma : `management.<backend>.metrics.export.*`  
  - Exemple : `management.prometheus.metrics.export.enabled=true`. [web:2]  

### 3.4. Spring Data

- Préfixe `spring.data.*` réservé à Spring Data. [web:2]  
- Ne pas l’utiliser pour des propriétés métier (à renommer en `platform.*`, etc.). [web:2]  
- Cassandra : `spring.data.cassandra.*` → `spring.cassandra.*`. [web:2]  
- Redis : `spring.redis.*` → `spring.data.redis.*`. [web:2]  

### 3.5. Spring Security

- Filtre appliqué à tous les `DispatcherType` par défaut. [web:2]  
- Propriété d’ajustement : `spring.security.filter.dispatcher-types`. [web:2]  
- SAML2 (si utilisé) :  
  - `spring.security.saml2.relyingparty.registration.{id}.identity-provider.*`  
    → `spring.security.saml2.relyingparty.registration.{id}.asserting-party.*`. [web:2]  

### 3.6. Spring Batch

- `@EnableBatchProcessing` n’est plus requis pour l’auto-config Spring Boot. [web:7]  
- Si présent, l’auto-config Boot se désactive (config Batch 100 % custom). [web:7]  
- Lancement de plusieurs jobs au démarrage non supporté :  
  - utiliser `spring.batch.job.name` pour indiquer le job à lancer. [web:13]  
- Migration schéma BDD Batch 4.x → 5.x obligatoire (scripts officiels). [web:13]  

### 3.7. JMS

- Principal impact : passage `javax.jms` → `jakarta.jms` au niveau du code/libs. [web:1]  
- Vérifier via le migrator que aucune propriété JMS n’est marquée obsolète. [web:18]  

---

## 4. Tableau mémo des propriétés à surveiller

| Domaine      | Ancienne propriété / endpoint                      | Nouvelle propriété / comportement                 |
|-------------|-----------------------------------------------------|--------------------------------------------------|
| Serveur web | `server.max-http-header-size`                      | `server.max-http-request-header-size`           |
| Actuator    | `/actuator/httptrace`                              | `/actuator/httpexchanges`                       |
| Actuator    | (valeurs `/env`, `/configprops` visibles)          | `management.endpoint.env.show-values` ; `management.endpoint.configprops.show-values` |
| Metrics     | `management.metrics.export.<backend>.*`            | `management.<backend>.metrics.export.*`         |
| Data        | Propriétés “maison” sous `spring.data.*`           | À déplacer sous un préfixe applicatif           |
| Data        | `spring.data.cassandra.*`                          | `spring.cassandra.*`                            |
| Data        | `spring.redis.*`                                   | `spring.data.redis.*`                           |
| Security    | SAML2 `...identity-provider.*`                     | `...asserting-party.*`                          |
| Batch       | Multi-jobs auto au démarrage                       | `spring.batch.job.name` obligatoire             |

```
