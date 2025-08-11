= JEF Security Configuration - Guide Développeur
:toc:
:numbered:

== Vue d'ensemble

Le framework JEF Security offre une configuration flexible des permissions d'endpoints via `JEFSecurityProperties`.

== Configuration YAML

[source,yaml]
----
jef:
  security:
    default-security:
      permit: false
      roles: ["USER"]
    endpoints:
      "/api/public/**":
        permit: true
      "/api/admin/**":
        permit: false
        roles: ["ADMIN"]
      "/api/management/**":
        permit: false
        roles: ["ADMIN", "MANAGER"]
----

== Niveaux de Sécurité

[cols="1,2,2"]
|===
|Niveau |Configuration |Comportement

|Public
|`permit: true`
|Accès libre, aucune auth

|Authentifié
|`permit: false, roles: []`
|Auth requise, tous rôles

|Rôle spécifique
|`permit: false, roles: ["ADMIN"]`
|Auth + rôle ADMIN

|Multiples rôles
|`permit: false, roles: ["ADMIN", "USER"]`
|Auth + rôle ADMIN OU USER
|===

== Exemples d'Usage

=== Code Java
[source,java]
----
// Endpoint public
JEFSecurityLevel.permitAll()

// Authentification simple
JEFSecurityLevel.authenticated()

// Rôle unique
JEFSecurityLevel.hasRole("ADMIN")

// Multiples rôles
JEFSecurityLevel.hasAnyRole("ADMIN", "USER")
----

=== Tests cURL
[source,bash]
----
# Public
curl /api/public/health

# Avec auth
curl -u user:pass /api/protected

# Admin seulement
curl -u admin:pass /api/admin/users
----

== Authentification

Deux méthodes supportées :

* *Basic Auth* : `-u username:password`
* *JWT Token* : `-H "Authorization: Bearer <token>"`

== Configuration par Défaut

La propriété `default-security` s'applique à tous les endpoints non configurés explicitement.

TIP: Utilisez `authenticated()` comme défaut pour sécuriser l'application par défaut.