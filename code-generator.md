Pour refléter une approche **API-First** complète et structurée, le titre global doit souligner que le document est le point d'entrée unique pour la synchronisation des contrats d'interface.

Voici une proposition de structure harmonisée pour votre document Markdown :

# 🛠️ Guide de Génération de Code (Multi-langages) via OpenAPI

Ce document centralise les procédures de génération automatique de code à partir de nos spécifications OpenAPI. L'utilisation du `openapi-generator-maven-plugin` garantit que toutes nos couches (Backend, Frontend et Scripts) partagent les mêmes modèles de données.

---

## ☕ 1. Générer des classes Java depuis une spécification OpenAPI

*(Insérez ici votre contenu initial existant sur la partie Java)*

---

## 🌐 2. Génération des Clients TypeScript et Python

Ces utilitaires s'appuient sur la **même structure de projet** et le **même POM** que la partie Java. Ils permettent de consommer l'API de manière typée dans d'autres écosystèmes tout en respectant strictement le contrat d'interface source.

### 🛠️ Commandes de génération

La génération s'effectue via l'activation de profils Maven dédiés :

* **Client TypeScript**
* **Commande** : `mvn compile -Pcodegen-ts-openapi`
* **Générateur** : `typescript-fetch`
* **Usage** : Intégration dans les applications Frontend.


* **Client Python**
* **Commande** : `mvn compile -Pcodegen-python-openapi`
* **Usage** : Scripts d'automatisation, Data Science ou tests d'intégration.



### 🏗️ Structure et Cohérence

Pour maintenir la cohérence avec la génération Java :

* Les sources sont générées dans des dossiers séparés sous `target/generated-sources/openapi/`.
* Toute modification de la structure des données doit être faite dans le fichier YAML source et non dans les dossiers `target`.

---

> [!TIP]
> **Le saviez-vous ?**
> L'utilisation de ces utilitaires garantit qu'un changement de nom de champ dans le YAML fera échouer la compilation du Frontend ou des scripts Python si ceux-ci ne sont pas mis à jour, évitant ainsi des erreurs en production.

Souhaitez-vous que je rajoute une **table des matières** au début du document pour faciliter la navigation entre les différentes sections ?

---

## 🌐 Génération des Clients TypeScript et Python

En complément de la génération des classes Java, nous mettons à disposition des utilitaires pour générer des clients dans d'autres langages. Ces outils partagent la **même structure de projet** et la **même configuration Maven** que la partie Java, garantissant une stricte cohérence avec le contrat d'interface (YAML).

### 🛠️ Commandes de génération

Pour générer les clients, utilisez les profils Maven dédiés via la phase `compile` :

* **Client TypeScript** :
```bash
mvn compile -Pcodegen-ts-openapi

```


* **Générateur utilisé** : `typescript-fetch` (pour une compatibilité maximale sans dépendances lourdes).
* **Cible** : Idéal pour les intégrations Frontend modernes.


* **Client Python** :
```bash
mvn compile -Pcodegen-python-openapi

```


* **Cible** : Utilisé pour les scripts d'automatisation, les tests ou les services de Data Science.



### 🏗️ Structure et Emplacement

Ces utilitaires respectent l'arborescence standard du projet définie dans le `pom.xml`. Les sources sont générées dans des répertoires isolés au sein de `target/generated-sources/openapi/` (au même niveau que le code Java généré), évitant ainsi toute pollution du code source versionné.

> [!IMPORTANT]
> Tout comme pour le code Java, ces fichiers sont éphémères. Toute modification doit être effectuée dans la spécification OpenAPI source et non directement dans les fichiers générés.

---

Souhaitez-vous que je rédige une courte section sur la manière d'**importer** le client `typescript-fetch` dans un projet front-end pour aider les développeurs à démarrer ?
