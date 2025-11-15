## 🛠️ Gestion des Propriétés Dépréciées : Configuration et Stratégie d'Échec

Cette section détaille l'utilisation de la fonctionnalité de détection des propriétés dépréciées fournie par notre Spring Boot Starter personnalisé. Elle vise à garantir que votre application utilise des configurations à jour et à faciliter la transition lors des mises à jour du Starter.

-----

### 🚨 Comportement par Défaut : Mode d'Échec Rapide (**`failFast`**)

Par défaut, notre Starter est configuré en mode **`failFast`**. Si votre application utilise une propriété de configuration jugée dépréciée (qu'elle provienne du Starter lui-même ou de vos configurations personnalisées), le processus de démarrage de l'application lèvera une exception de type `IllegalArgumentException`, signalant immédiatement l'utilisation de la propriété obsolète.

La propriété interne contrôlant ce comportement est **`jef.deprecated-properties.fail-fast`**, et sa valeur par défaut est **`true`**.

| Propriété | Description | Valeur par Défaut |
| :--- | :--- | :--- |
| `jef.deprecated-properties.fail-fast` | Active l'échec immédiat (`true`) ou permet un simple enregistrement d'erreur (`false`) lors de la détection de propriétés dépréciées. | `true` |

#### **⚠️ Recommandation Forte : Maintenir `failFast` à `true`**

Il est **fortement déconseillé** de surcharger la valeur par défaut pour la mettre à `false`. La présence de propriétés dépréciées indique une **dette technique** à résoudre. Le mode `failFast` assure que cette dette est traitée immédiatement, évitant des problèmes futurs ou des comportements inattendus. La meilleure pratique est de **supprimer la propriété dépréciée de votre configuration et d'ajuster votre code** en conséquence.

Si, dans des circonstances exceptionnelles (par exemple, pour des tests très spécifiques ou une migration par étapes *extrêmement* contrôlée), vous devez temporairement désactiver ce comportement, vous pouvez le faire dans votre fichier `application.yml` :

```yaml
jef:
  deprecated-properties:
    fail-fast: false # A utiliser seulement si absolument nécessaire et temporairement!
```

**Note :** Si `fail-fast` est mis à `false`, un message d'erreur sera tout de même loggué, vous rappelant qu'il est anormal de désactiver ce mécanisme.

-----

### 📝 Gestion des Propriétés Dépréciées Personnalisées (**`customKeys`**)

La fonctionnalité vous permet d'étendre la liste des propriétés à surveiller avec celles spécifiques à votre propre projet ou module. Ces propriétés doivent être configurées sous la clé **`jef.deprecated-properties.custom-keys`**.

#### **Déclaration de Propriétés Dépréciées de Projet**

La liste `customKeys` accepte des chaînes de caractères qui peuvent contenir des **caractères génériques** (wildcards), ce qui est utile pour cibler des ensembles de propriétés.

Pour ajouter vos propres propriétés dépréciées, ajoutez la configuration suivante à votre fichier `application.yml` :

```yaml
jef:
  deprecated-properties:
    # Liste des propriétés dépréciées gérées par l'équipe de développement.
    # Supporte les caractères génériques (wildcards).
    custom-keys:
      - "mon.ancienne.cle.a.supprimer"
      - "config.legacy.*" # Cible toutes les clés commençant par config.legacy.
      - "database.old-connection"
```

Si l'une des clés listées ci-dessus est trouvée dans l'environnement (par exemple, dans `application.yml`, les variables d'environnement, etc.), elle déclenchera le mécanisme d'alerte (`log.error`) et potentiellement l'exception (`failFast: true`).

#### **Propriétés Dépréciées du Starter (`jefKeys`)**

La liste **`jef.deprecated-properties.jef-keys`** contient les clés dépréciées gérées directement par le Starter. Cette liste est **définie par notre département** et n'est généralement pas destinée à être modifiée par les équipes consommatrices. Elle est mentionnée ici pour information :

```yaml
jef:
  deprecated-properties:
    # Liste gérée par l'équipe du Starter (exemple de valeur par défaut)
    jef-keys:
      - "key1"
      - "key2"
      - "spring.jef-starter.deprecated-feature.*"
```

-----

### 📘 Référence Rapide `application.yml` (Exemple Complet)

Voici un exemple de configuration complète pour votre équipe, incluant la liste des propriétés à supprimer et la désactivation temporaire du mode `failFast` (à éviter) :

```yaml
# ===================================================================
# Configuration de la Détection de Propriétés Dépréciées
# ===================================================================
jef:
  deprecated-properties:
    # [DÉCONSEILLÉ] Temporairement désactiver l'exception, préférer la suppression des clés
    fail-fast: false
    
    # URL de la documentation pour savoir comment remplacer les clés dépréciées
    # (Valeur par défaut du Starter : https://wiki-guide.com)
    guide-url: "https://wiki-guide.com/v2/migration-guide"

    # Liste des propriétés dépréciées spécifiques à notre projet
    custom-keys:
      - "mon-app.propriete.obsolete"
      - "service.old-cache-timeout"
      - "utilisateur.ancien-format.*" # Support des wildcards
```

En utilisant ce mécanisme, vous vous assurez que les configurations de votre application restent propres et à jour.
