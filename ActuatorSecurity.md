C'est noté. Vous souhaitez une documentation pour les développeurs basée sur la structure la plus simple que vous avez fournie, sans la complexité des environnements ou des méthodes HTTP.

Voici la documentation adaptée à cette **structure simplifiée** :

-----

# 🛡️ Sécurité Simplifiée des Endpoints Actuator (Mode Standard)

L'utilitaire de sécurité Actuator permet désormais de définir des règles d'accès claires et simples pour chaque endpoint, basées uniquement sur l'URL et les rôles requis.

## 1\. Structure de Configuration YAML

La configuration de la sécurité est définie dans votre fichier de configuration (`application.yml` ou équivalent) sous le préfixe `jef.actuator`.

```yaml
jef:
  actuator:
    security: enabled # Optionnel, par défaut 'true'
    endpoints:
      - url: "/actuator/health"
        authenticated: false # Aucune authentification requise
      
      - url: "/actuator/loggers"
        authenticated: true  # Authentification obligatoire
        roles:
          - ROLE_ACTUATOR_ADMIN # Nécessite ce rôle spécifique
```

-----

## 2\. 📝 Propriétés des Endpoints

Chaque règle d'accès est définie par les trois propriétés suivantes, appliquées à toutes les méthodes HTTP (GET, POST, PUT, etc.) :

| Propriété | Type | Description |
| :--- | :--- | :--- |
| `url` | `String` | Le **chemin Actuator** à sécuriser (ex: `/actuator/info`, `/actuator/metrics/**`). |
| `authenticated` | `Boolean` | Si `true`, la requête **doit être authentifiée**. Si `false`, l'accès est public (équivalent à `permitAll()`). |
| `roles` | `List<String>` | **(Optionnel)** La liste des **rôles requis** pour accéder à l'URL. N'est prise en compte que si `authenticated` est à `true`. |

### Comportement de la Règle

| `authenticated` | `roles` | Résultat de la Règle |
| :--- | :--- | :--- |
| `false` | (Omis ou vide) | **Accès Public** (équivalent à Spring Security `permitAll()`). |
| `true` | (Omis ou vide) | **Authentification simple** requise (équivalent à `authenticated()`). |
| `true` | `["ROLE_X", "ROLE_Y"]` | **Autorisation par Rôle** (équivalent à `hasAnyRole("ROLE_X", "ROLE_Y")`). |

-----

## 3\. 💡 Exemple Pratique et Rôles Recommandés

### Utilisation

Le mode simplifié s'applique à **toutes les méthodes HTTP** pour un `url` donné.

```yaml
jef:
  actuator:
    security: enabled
    endpoints:
      # 1. Accès public pour les vérifications de base
      - url: "/actuator/health"
        authenticated: false 

      # 2. Accès réservé aux utilisateurs authentifiés, pour la lecture des données
      - url: "/actuator/metrics/**"
        authenticated: true

      # 3. Accès strictement réservé aux administrateurs pour les opérations sensibles
      - url: "/actuator/shutdown"
        authenticated: true
        roles:
          - ROLE_ACTUATOR_ADMIN
```

### Rôles Recommandés

Dans ce mode, la distinction entre lecture et écriture doit être faite au niveau du rôle lui-même :

  * `ROLE_ACTUATOR_VIEWER`: Pour les endpoints de **lecture** (`/info`, `/metrics`, `/beans`).
  * `ROLE_ACTUATOR_ADMIN`: Pour les endpoints de **modification** (`/loggers`, `/shutdown`, `/env`).

Cette structure est la plus simple à configurer et à maintenir pour un contrôle d'accès de base.

Voulez-vous que je vous fournisse des exemples de configuration pour des endpoints Actuator courants utilisant cette structure simplifiée ?
