# Guide d'Exécution des Tests - Configuration Sécurité Actuator

## Vue d'Ensemble

Cette suite de tests valide la configuration de sécurité des endpoints Actuator avec support des méthodes HTTP et des environnements. Elle comprend des tests unitaires, d'intégration, de performance et de sécurité.

## Types de Tests

### 1. Tests Unitaires (`@Tag("unit")`)
- **ActuatorSecurityServiceTest** : Test du service de résolution des règles
- **ActuatorSecurityPropertiesTest** : Test du mapping des propriétés YAML
- Exécution rapide, pas de contexte Spring complet

### 2. Tests d'Intégration (`@Tag("integration")`)
- **ActuatorSecurityIntegrationTest** : Test complet avec MockMvc
- **ActuatorSecurityDatabaseIntegrationTest** : Test avec base de données
- Contexte Spring Boot complet, test des interactions

### 3. Tests de Performance (`@Tag("performance")`)
- **ActuatorSecurityPerformanceTest** : Test de charge et de temps de réponse
- **JMeter Integration** : Tests de charge externes

### 4. Tests de Sécurité
- **ActuatorSecurityAdvancedTest** : Tests de sécurité avancés
- **ActuatorSecurityRegressionTest** : Tests de non-régression

## Commandes d'Exécution

### Tests Unitaires Uniquement
```bash
mvn test -P unit-tests
# ou
mvn test -Dgroups=unit
```

### Tests d'Intégration
```bash
mvn verify -P integration-tests
# ou  
mvn verify -Dgroups=integration
```

### Tests de Performance
```bash
mvn verify -P performance-tests
# ou
mvn verify -Dgroups=performance
```

### Tous les Tests
```bash
mvn verify -P all-tests
# ou
mvn verify -Dgroups="unit,integration"
```

### Tests pour CI/CD
```bash
mvn verify -P ci
```

## Configuration par Environnement

### Test en Environnement DEVELOP
```bash
mvn test -Dspring.profiles.active=develop
```

### Test en Environnement QA
```bash
mvn test -Dspring.profiles.active=qa
```

### Test en Environnement PROD
```bash
mvn test -Dspring.profiles.active=prod
```

## Tests JMeter

### Exécution des Tests de Charge
```bash
mvn jmeter:jmeter -Djmeter.testfiles=actuator-security-load-test.jmx
```

### Génération du Rapport JMeter
```bash
mvn jmeter:results
```

## Couverture de Code

### Génération du Rapport JaCoCo
```bash
mvn jacoco:report
```

### Vérification des Seuils de Couverture
```bash
mvn jacoco:check
```

Le rapport sera disponible dans : `target/site/jacoco/index.html`

## Tests de Mutation (Pitest)

### Exécution des Tests de Mutation
```bash
mvn org.pitest:pitest-maven:mutationCoverage
```

Le rapport sera disponible dans : `target/pit-reports/index.html`

## Structure des Fichiers de Test

```
src/test/
├── java/
│   ├── com/yourcompany/security/actuator/
│   │   ├── ActuatorSecurityIntegrationTest.java
│   │   ├── ActuatorSecurityServiceTest.java
│   │   ├── ActuatorSecurityPropertiesTest.java
│   │   ├── ActuatorSecurityPerformanceTest.java
│   │   ├── ActuatorSecurityAdvancedTest.java
│   │   ├── ActuatorSecurityRegressionTest.java
│   │   └── ActuatorTestUtils.java
│   └── config/
│       └── TestSecurityConfiguration.java
├── resources/
│   ├── application-test.yml
│   ├── test-data.sql
│   └── logback-test.xml
└── jmeter/
    └── actuator-security-load-test.jmx
```

## Variables d'Environnement pour Tests

### Variables Principales
```bash
export SPRING_PROFILES_ACTIVE=test
export MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=*
export APP_SECURITY_ACTUATOR_DEBUG=true
```

### Variables pour Tests de Performance
```bash
export JMETER_THREADS=50
export JMETER_RAMP_UP=10
export JMETER_DURATION=60
```

## Tests par Scénarios

### Scénario 1 : Validation Configuration DEVELOP
```bash
mvn test -Dtest=ActuatorSecurityIntegrationTest$DevelopEnvironmentTests -Dspring.profiles.active=develop
```

### Scénario 2 : Validation Configuration QA
```bash
mvn test -Dtest=ActuatorSecurityIntegrationTest$QAEnvironmentTests -Dspring.profiles.active=qa
```

### Scénario 3 : Validation Configuration PROD
```bash
mvn test -Dtest=ActuatorSecurityIntegrationTest$ProdEnvironmentTests -Dspring.profiles.active=prod
```

### Scénario 4 : Test de Sécurité Avancé
```bash
mvn test -Dtest=ActuatorSecurityAdvancedTest
```

## Debugging des Tests

### Activation des Logs de Debug
```bash
