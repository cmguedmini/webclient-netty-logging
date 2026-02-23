C'est une excellente initiative. Pour que ce document soit réellement utile aux développeurs et aux intégrateurs, il doit être structuré par **impact opérationnel** plutôt que par simple liste alphabétique.

Voici une version restructurée, conçue comme un **Guide de Migration Technique**.

---

# 📘 Guide de Migration : Spring Boot 2.7 ➔ 3.x

Ce document récapitule les changements critiques pour nos environnements. **Prérequis impératif :** Java 17 minimum et passage au namespace `jakarta.*`.

---

## 1. Changements Globaux (Le "Breaking Change" n°1)

Le passage de Java EE à **Jakarta EE 9/10** est le changement le plus impactant pour l'intégration.

* **Namespace :** Remplacer tous les `import javax.persistence.*`, `import javax.servlet.*`, et `import javax.jms.*` par `jakarta.*`.
* **Dépendances :** Les bibliothèques tierces (ex: Hibernate, QueryDSL) doivent être mises à jour vers leurs versions compatibles Jakarta.

---

## 2. Spring Batch 5.0 : Refonte de la Configuration

Spring Batch est le module qui subit le plus de changements structurels.

### 🛑 Ce qui disparaît / change :

* **`@EnableBatchProcessing`** : N'ajoutez plus cette annotation si vous voulez l'auto-configuration de Spring Boot. Si elle est présente, vous devez configurer manuellement le `TransactionManager` et le `JobRepository`.
* **`DefaultBatchConfigurer`** : Supprimé. Utilisez l'interface `BatchConfigurer` ou l'auto-configuration.

### 📝 Nouvelles propriétés :

| Domaine | Ancienne (2.x) | Nouvelle (3.x) |
| --- | --- | --- |
| **Exécution** | `spring.batch.job.enabled` | `spring.batch.job.enabled` (Déprécié, mais conservé pour l'instant) |
| **Schéma** | `spring.batch.jdbc-schema-selector` | *Supprimé* (Détection automatique via dialecte) |
| **Table** | `spring.batch.initialize-schema` | `spring.batch.jdbc.initialize-schema` |

> **Action Intégrateur :** Les scripts SQL de création des tables Batch ont changé. Les colonnes `TIMESTAMP` sont plus précises. Un script de migration des métadonnées est nécessaire pour les jobs existants.

---

## 3. Spring Security 6 : Sécurité par Composants

Fini l'héritage de classe, place à l'injection de Beans.

### 🛑 Suppression de `WebSecurityConfigurerAdapter`

Les développeurs doivent migrer vers une configuration de type `SecurityFilterChain`.

**Exemple de transformation :**

```java
// AVANT (2.x)
@Override
protected void configure(HttpSecurity http) throws Exception {
    http.authorizeRequests().antMatchers("/admin/**").hasRole("ADMIN");
}

// APRÈS (3.x)
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth.requestMatchers("/admin/**").hasRole("ADMIN"));
    return http.build();
}

```

---

## 4. Spring Data & Persistance (Hibernate 6)

Le moteur SQL a été réécrit pour générer un SQL plus moderne.

* **Identifiants :** La génération automatique d'ID (`SequenceStyleGenerator`) est désormais le standard.
* **Propriétés de log :**
* *Ancien :* `logging.level.org.hibernate.type.descriptor.sql=trace`
* *Nouveau :* `logging.level.org.hibernate.orm.results=debug` (plus lisible).



---

## 5. Actuator & Observabilité

Le monitoring change de nom de domaine dans les fichiers de configuration.

| Catégorie | Ancienne Clé (2.x) | Nouvelle Clé (3.x) |
| --- | --- | --- |
| **Traces** | `management.trace.http` | `management.otlp.tracing` (Standard OpenTelemetry) |
| **Metrics** | `management.metrics.export.prometheus` | `management.prometheus.metrics.export` |
| **Endpoints** | `management.endpoints.web.exposure.include` | Identique, mais vérifiez les nouveaux endpoints `/health/liveness` |

---

## 6. JMS (Java Messaging Service)

Le module JMS suit la règle du renommage Jakarta.

* **Artifactory :** Vérifiez que vos drivers (ActiveMQ Artermis, etc.) utilisent les versions `jakarta-client`.
* **Propriétés :** Les préfixes restent globalement identiques (`spring.jms.*`), mais la classe de base `JmsTemplate` attend désormais des objets `jakarta.jms.Destination`.

---

## 💡 Conseil pour les Développeurs : Le mode "Pont"

Pour faciliter la transition, ajoutez le **Properties Migrator** dans votre `pom.xml` :

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-properties-migrator</artifactId>
    <scope>runtime</scope>
</dependency>

```

*Ce module affichera dans la console au démarrage toutes les propriétés que vous avez oubliées de renommer.*

---
