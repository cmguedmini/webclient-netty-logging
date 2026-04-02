## Diagnostic

L'erreur vient du fait que Jackson ne sait plus sérialiser/désérialiser les types Joda-Time par défaut. En Java 8 avec une ancienne version de Spring Boot, ce module était parfois auto-configuré ou inclus transitivement. En Spring Boot 2.7.x, ce n'est plus le cas.

## Solution

Tu as **deux options** :

---

### Option 1 — Enregistrer le module Joda manuellement (recommandé si tu gardes Joda)

**1. Ajoute la dépendance Maven :**

```xml
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-joda</artifactId>
</dependency>
```
> La version est gérée automatiquement par le BOM de Spring Boot 2.7.x.

**2. Enregistre le module sur ton `ObjectMapper` :**

```java
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JodaModule());
        // Optionnel mais recommandé : désactive les timestamps, utilise ISO-8601
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
```

Ou si tu utilises `Jackson2ObjectMapperBuilderCustomizer` pour ne pas écraser la config Spring Boot :

```java
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jodaCustomizer() {
        return builder -> builder
                .modulesToInstall(new JodaModule())
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
```

---

### Option 2 — Migrer vers `java.time` (recommandé sur le long terme)

Joda-Time est en maintenance, et `java.time` (JSR-310) est son successeur direct, disponible nativement depuis Java 8.

**1. Remplace les types dans tes DTOs :**

```java
// Avant
import org.joda.time.LocalDateTime;

// Après
import java.time.LocalDateTime;
```

**2. Ajoute le module JSR-310 (souvent déjà actif via Spring Boot) :**

```xml
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
</dependency>
```

**3. Configure Jackson :**

```java
@Bean
public Jackson2ObjectMapperBuilderCustomizer jsr310Customizer() {
    return builder -> builder
            .modulesToInstall(new JavaTimeModule())
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
}
```

---

## Recommandation

| Situation | Choix |
|---|---|
| Peu de classes avec Joda, refacto possible | ✅ **Option 2** — migre vers `java.time` |
| Beaucoup de code Joda, migration risquée | ✅ **Option 1** — enregistre `JodaModule` |

Dans tous les cas, **l'Option 1 est le correctif rapide** pour débloquer la situation immédiatement.
