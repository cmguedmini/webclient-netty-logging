# Spring Boot 3 + Spring Security - AuthenticationManagerResolver Demo

Ce projet démontre l'utilisation d'un `AuthenticationManagerResolver` avec Spring Boot 3 et Spring Security pour gérer différents types d'authentification selon les chemins.

## Architecture

### AuthenticationManagerResolver
- **Actuator paths** (`/actuator/**`) : Utilise `TokenAuthenticationProvider` + `BasicAuthenticationProvider`
- **Autres paths** (`/api/**`) : Utilise uniquement `BasicAuthenticationProvider`

### Providers d'authentification

1. **TokenAuthenticationProvider** : Authentification par token via headers
   - Header `X-API-Token` ou `Authorization: Bearer <token>`
   - Tokens valides : `admin-token-123` (ROLE_ADMIN), `manager-token-456` (ROLE_MANAGER)

2. **BasicAuthenticationProvider** : Authentification HTTP Basic
   - Utilisateurs : admin/adminpass (ROLE_ADMIN), manager/managerpass (ROLE_MANAGER), user/userpass (ROLE_USER)

## Configuration des endpoints Actuator

Via `application.yml`, configuration flexible des endpoints :

```yaml
jef:
  security:
    actuator:
      enabled: true
      endpoints:
        - url: "/actuator/health"
          authenticated: false
        - url: "/actuator/beans"
          authenticated: true
          roles:
            - "ADMIN"
            - "MANAGER"
```

## Démarrage

```bash
mvn spring-boot:run
```

L'application démarre sur http://localhost:8080

## Tests des endpoints

### 1. Endpoint public
```bash
curl http://localhost:8080/
```

### 2. Actuator Health (public)
```bash
curl http://localhost:8080/actuator/health
```

### 3. Actuator Beans avec Basic Auth
```bash
curl -u admin:adminpass http://localhost:8080/actuator/beans
curl -u manager:managerpass http://localhost:8080/actuator/beans
```

### 4. Actuator Beans avec Token
```bash
curl -H "X-API-Token: admin-token-123" http://localhost:8080/actuator/beans
curl -H "Authorization: Bearer manager-token-456" http://localhost:8080/actuator/beans
```

### 5. API sécurisée avec Basic Auth
```bash
curl -u admin:adminpass http://localhost:8080/api/secure
curl -u user:userpass http://localhost:8080/api/secure
```

### 6. API sécurisée avec Token (ne fonctionne pas - uniquement Basic Auth)
```bash
curl -H "X-API-Token: admin-token-123" http://localhost:8080/api/secure
# Retourne 401 car les tokens ne sont supportés que pour /actuator/**
```

## Structure du projet

```
src/main/java/com/example/
├── SpringSecurityAuthResolverApplication.java
├── config/
│   ├── SecurityConfig.java
│   └── SecurityProperties.java
├── security/
│   ├── CustomAuthenticationManagerResolver.java
│   ├── TokenAuthenticationProvider.java
│   ├── BasicAuthenticationProvider.java
│   └── TokenAuthenticationFilter.java
└── controller/
    └── TestController.java
```

## Points clés

- **AuthenticationManagerResolver** permet de choisir le bon `AuthenticationManager` selon le contexte
- Les endpoints `/actuator/**` supportent les tokens ET l'authentification Basic
- Les autres endpoints supportent uniquement l'authentification Basic
- Configuration flexible via `application.yml` pour les règles d'accès aux endpoints Actuator
- Utilisation de Records Java pour les propriétés de configuration (Spring Boot 3)

## Dépendances principales

- Spring Boot 3.2.0
- Spring Security 6
- Spring Boot Actuator
- Java 17