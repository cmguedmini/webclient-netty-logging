## Dates et heures

### Rappel OpenAPI

Dans notre contrat OpenAPI, les dates et dates‑heures restent décrites avec le type `string` et un `format` spécifique :

- `format: date`  → date au format ISO‑8601 `yyyy‑MM‑dd` (ex. `2017-07-21`)  
- `format: date-time` → date‑heure au format ISO‑8601 `yyyy‑MM‑dd'T'HH:mm:ssXXX` avec fuseau horaire (ex. `2017-07-21T17:32:28Z`)  

Lorsque le fournisseur n’indique pas de `format` pour un champ de type `string` contenant une date, la spécification doit être complétée pour clarifier le type de donnée attendu et faciliter la génération du code.

***

## Types Java générés (configuration actuelle)

Avec `openapi-generator-maven-plugin` (v. 7.19.0) et la configuration mise à jour :

```xml
<dateLibrary>custom</dateLibrary>
<typeMappings>
  <typeMapping>DateTime=Instant</typeMapping>
  <typeMapping>Date=LocalDate</typeMapping>
  <typeMapping>duration=Duration</typeMapping>
</typeMappings>
```

les types Java générés sont :

| Spécification OpenAPI           | Type Java généré | Package              |
|--------------------------------|------------------|----------------------|
| `type: string`, `format: date-time` | `Instant`       | `java.time`         |
| `type: string`, `format: date`      | `LocalDate`     | `java.time`         |
| `type: string`, `format: duration`  | `Duration`      | `java.time`         |

Ces types servent dans les modèles et DTO générés par le plugin, aussi bien côté client que côté serveur.

***

## Conséquences pour les développeurs

### Champs `date-time` → `Instant`

- Utiliser `Instant` pour toute donnée horodatée avec fuseau ou en UTC.  
- Pour la sérialisation JSON (Jackson), le format attendu reste ISO‑8601 en UTC (ex. `2025‑12‑31T23:59:59Z`).  
- Pour travailler avec un fuseau horaire, convertir vers/depuis `ZonedDateTime` ou `OffsetDateTime` dans la couche métier, par exemple :  
  - `ZonedDateTime zdt = instant.atZone(ZoneId.of("Europe/Luxembourg"));`  
  - `Instant instant = zdt.toInstant();`

### Champs `date` → `LocalDate`

- Les dates « calendaires » (sans heure) sont envoyées en JSON au format ISO‑8601 `yyyy‑MM‑dd` et mappées nativement sur `java.time.LocalDate`.  
- Sérialisation/désérialisation parfaite avec Jackson/Spring Boot (support natif `@DateTimeFormat`).  
- Manipulation directe : `localDate.plusDays(1)`, `localDate.isBefore(otherDate)`, `localDate.withYear(2026)`.

### Champs `duration` → `Duration`

- Les durées sont représentées par `java.time.Duration`.  
- Le format JSON associé doit être précisé dans la spécification OpenAPI (ex. `PT15M` pour 15 minutes, conforme ISO‑8601).  
- Conversion facile : `Duration.ofMinutes(30)`, `duration.toMinutes()`.

**Note** : Tous les types utilisent désormais `java.time.*` pour une cohérence parfaite (immutabilité, thread-safety).

***

## Dates dans un format personnalisé

Pour les champs qui ne respectent pas ISO‑8601 (ex. `yyyyMMdd`), utiliser un `type: string` sans `format` standard, mais avec un `pattern` et une description explicite :

```yaml
customDate:
  type: string
  pattern: '^\\d{4}([01][0-9]|1[012])([0-2][0-9]|3[01])$'
  description: Date au format yyyyMMdd
  example: '20210130'
```

Dans ce cas :

- Le code généré utilisera un `String`.  
- Convertir manuellement vers `LocalDate` (ex. `LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyyMMdd"))`) et valider en métier.

***

## Bonnes pratiques recommandées

- **Toujours** utiliser `format: date` / `date-time` pour garantir `LocalDate` / `Instant`.  
- Limiter les formats personnalisés (`pattern`) aux APIs legacy externes.  
- Centraliser les utilitaires de conversion (ex. `Instant` ↔ `ZonedDateTime`) dans un package partagé.  
- Tester la génération avec `mvn clean generate-sources` et valider JSON round-trip.
