Oui, je te recommande fortement d'ajouter une section **"Préparation de l'environnement et prérequis de migration"** avant la phase de rejeu. C'est généralement attendu dans un DAT et c'est souvent là que les équipes Infra, DBA, Middleware et Exploitation vont concentrer leurs questions.

Je la placerais entre :

```text
3. Architecture cible
4. Préparation de l'environnement
5. Rejeu historique
```

***

# 4. Préparation de l'environnement

## 4.1 Objectif

Cette phase vise à préparer l'ensemble des composants techniques nécessaires à l'exécution du rejeu historique, à la capture du delta, à la restauration des données en production et à la phase de coexistence.

Aucune opération de rejeu ne doit être démarrée tant que l'ensemble des prérequis n'est pas validé.

***

# 4.2 Préparation des environnements

## Environnement PREPROD

L'environnement PREPROD servira à :

* exécuter le rejeu des 5 années d'historique ;
* construire la base Oracle cible ;
* construire l'index Elasticsearch cible ;
* effectuer les premiers contrôles de cohérence.

### Composants attendus

| Composant             | Statut     |
| --------------------- | ---------- |
| Oracle PREPROD        | Disponible |
| Elasticsearch PREPROD | Disponible |
| Control Service       | Déployé    |
| MQ Client             | Configuré  |
| Réseau                | Ouvert     |

***

## Environnement PROD

L'environnement PROD sera préparé avant le restore.

### Composants attendus

| Composant          | Statut         |
| ------------------ | -------------- |
| Oracle PROD        | Disponible     |
| Elasticsearch PROD | Disponible     |
| MQ PROD            | Disponible     |
| Backend LUPIA      | Package validé |
| Frontend LUPIA     | Package validé |
| Control Service    | Package validé |

***

# 4.3 Préparation MQ

## Création des files MQ

### Legacy Queue

Utilisée par l'application Legacy.

```text
LEGACY.QUEUE
```

### Replay Queue

Utilisée pour stocker les messages produits pendant les 10 jours de rejeu.

```text
LUPIA.REPLAY.QUEUE
```

### LUPIA Production Queue

Utilisée lors de la phase de coexistence puis de production finale.

```text
LUPIA.PROD.QUEUE
```

***

## Configuration Dispatcher

Le Dispatcher doit être capable de dupliquer les messages.

### Phase Rejeu

```text
Producer
   |
Dispatcher
   |
   +--> LEGACY.QUEUE
   |
   +--> LUPIA.REPLAY.QUEUE
```

### Phase Coexistence

```text
Producer
   |
Dispatcher
   |
   +--> LEGACY.QUEUE
   |
   +--> LUPIA.PROD.QUEUE
```

***

# 4.4 Préparation Oracle

## Contrôles préalables

* Vérification espace disque.
* Vérification tablespaces.
* Vérification logs Oracle.
* Vérification droits applicatifs.

## Capacité

L'espace disque doit permettre :

```text
Base Oracle historique complète

+

Fichiers export

+

Croissance pendant migration
```

***

# 4.5 Préparation Elasticsearch

## Prérequis

* Cluster vert (GREEN)
* Espace disque validé
* Snapshots configurés
* Politique de rétention définie

## Tests

* Création index
* Suppression index
* Exécution snapshot
* Restauration snapshot

***

# 4.6 Préparation LUPIA

## Backend

Validation :

* connexion Oracle ;
* connexion MQ ;
* connexion Keycloak ;
* connexion Elastic.

***

## Frontend

Validation :

* authentification ;
* navigation ;
* accès APIs.

***

## Control Service

Validation :

* accès MQ ;
* accès Elastic ;
* accès Oracle ;
* performances de consommation.

***

# 4.7 Paramètres de migration

Les paramètres suivants doivent être validés avant lancement.

| Paramètre                 | Valeur      |
| ------------------------- | ----------- |
| Historique à rejouer      | 5 ans       |
| Durée estimée rejeu       | 10 jours    |
| Débit moyen de production | À compléter |
| Taille moyenne message MQ | À compléter |
| Débit cible du replay     | À compléter |
| Durée restore Oracle      | À compléter |
| Durée restore Elastic     | À compléter |

***

# 4.8 Monitoring

Les tableaux de bord doivent être disponibles avant le démarrage.

## MQ

Surveiller :

* profondeur Legacy Queue ;
* profondeur Replay Queue ;
* profondeur LUPIA Queue ;
* débit de consommation.

***

## Oracle

Surveiller :

* CPU ;
* mémoire ;
* volume de données ;
* temps de réponse.

***

## Elasticsearch

Surveiller :

* taille index ;
* temps indexation ;
* santé cluster.

***

# 4.9 Critères GO / NO GO avant rejeu

Le rejeu historique ne peut commencer que si :

✅ MQ opérationnel

✅ Dispatcher configuré

✅ Oracle PREPROD validé

✅ Elasticsearch PREPROD validé

✅ LUPIA déployable

✅ Sauvegardes testées

✅ Monitoring opérationnel

✅ Procédures rollback validées

✅ Volumétrie estimée validée avec l'infrastructure

***

Cette section apportera beaucoup de valeur dans ton DAT car elle répond aux questions habituelles des équipes :

* Infrastructure
* DBA
* Middleware MQ
* Exploitation
* Production

avant même de commencer la phase de rejeu de 10 jours.
