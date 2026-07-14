Excellente idée 👍

Le meilleur moyen d'évangéliser l'équipe est justement de leur montrer que la présentation elle-même est écrite en AsciiDoc, puis générée en HTML avec Antora/RevealJS.

Je te propose un fichier antora-poc.adoc que tu peux placer dans :

modules/ROOT/pages/tools/antora-poc.adoc

= POC Antora - Documentation as Code
:author: Chaouki Mguedmini
:revealjs_theme: night
:revealjs_slideNumber: true
:icons: font

== Antora & Docs-as-Code

[.lead]
Documentation centralisée, versionnée et maintenable.

* Antora
* AsciiDoc
* Git
* Multi-repos
* Documentation as Code

---

== Pourquoi Antora ?

Antora permet de :

* Générer des sites documentaires statiques
* Utiliser Git comme source unique
* Versionner la documentation
* Centraliser plusieurs dépôts documentaires
* Faciliter la contribution des développeurs

[NOTE]
====
L'objectif n'est pas uniquement de produire de la documentation mais d'intégrer celle-ci au cycle de vie du projet.
====

---

== Le problème aujourd'hui

Souvent la documentation est :

* dispersée
* difficile à maintenir
* obsolète
* stockée dans plusieurs outils

[source]
----
Code
 ↓
Release
 ↓
Documentation plus tard...
----

Résultat :

❌ Documentation non alignée avec le produit

---

== Le principe Docs-as-Code

[source]
----
Code + Documentation
           ↓
      Commit Git
           ↓
      Merge Request
           ↓
       Validation
           ↓
       Release
----

✅ Même cycle de vie que le code

✅ Même traçabilité

✅ Même gouvernance

---

== Concepts fondamentaux Antora

=== Component

Un domaine documentaire indépendant.

Exemple :

* Backend
* Frontend
* Parser Indexer

=== Module

Organisation du contenu à l'intérieur d'un composant.

=== Page

Fichier AsciiDoc individuel.

=== Playbook

Point d'entrée permettant d'agréger toutes les documentations.

---

== Architecture Multi-Repos LuPIA

[source]
----
lupia-back/docs
lupia-front/docs
parser-indexer/docs
       │
       ▼
antora-playbook.yml
       │
       ▼
      ANTORA
       │
       ▼
 Documentation LuPIA
----

---

== Agrégation Antora

[source]
----
Backend Repo ─┐
Frontend Repo ├───► Antora
Parser Repo ──┘

                │
                ▼

      Site documentaire unique
----

Chaque équipe est propriétaire de sa documentation.

Antora construit ensuite une vision documentaire unifiée.

---

== Exemple de Playbook

[source,yaml]
----
site:
  title: Documentation LuPIA

content:
  sources:
    - url: https://gitlab/.../lupia-back.git
      branches: develop
      start_path: docs

    - url: https://gitlab/.../lupia-front.git
      branches: develop
      start_path: docs

    - url: https://gitlab/.../parser-indexer.git
      branches: develop
      start_path: docs
----

---

== Structure documentaire recommandée

[source]
----
modules/
├── ROOT/
│   ├── functional/
│   ├── architecture/
│   ├── operations/
│   └── assets/images/
│
├── backend/
├── frontend/
└── parser-indexer/
----

---

== Documentation Fonctionnelle

Contenu recommandé :

* Introduction
* Base légale
* Modules métier
* Processus BPMN
* Acteurs et rôles
* Interfaces externes
* Modèle conceptuel de données

---

== Documentation Technique

Contenu recommandé :

* Vue d'ensemble de l'architecture
* Diagrammes PlantUML
* APIs
* Flux JMS
* Elasticsearch
* Déploiement
* Monitoring

---

== Gestion des images

Structure recommandée :

[source]
----
modules/ROOT/assets/images/

├── functional/
├── architecture/
└── operations/
----

Exemple :

[source,adoc]
----
image::architecture/global-overview.png[]
----

---

== Démonstration développeur

Scénario :

. Création d'une page
. Ajout dans la navigation
. Commit Git
. Génération du site
. Publication

---

== Création d'une page

Exemple :

[source,adoc]
----
= Replay Flow

== Objectif

Le replay permet
de retraiter
des messages historiques.
----

---

== Mise à jour de la navigation

[source,adoc]
----
* xref:replay-flow.adoc[Replay Flow]
----

Le menu est mis à jour automatiquement.

---

== Exemple AsciiDoc

[source,adoc]
----
= Replay Flow

== Objectif

Retraiter les messages historiques.

image::architecture/replay.svg[]

xref:index.adoc[Retour à l'accueil]
----

---

== Workflow Git → Antora

[source]
----
Développeur
    │
    ▼
Fichiers .adoc
    │
    ▼
Commit Git
    │
    ▼
Merge Request
    │
    ▼
Pipeline CI/CD
    │
    ▼
Build Antora
    │
    ▼
Site HTML
----

---

== Publication locale

[source,bash]
----
npm install

npm run build:dev:fetch
----

Résultat :

[source]
----
build/site/index.html
----

Ouverture immédiate dans le navigateur.

---

== Ressources utiles

=== Référence rapide

https://docs.asciidoctor.org/asciidoc/latest/syntax-quick-reference/

=== Guide complet

https://asciidoctor.org/docs/asciidoc-writers-guide/

=== Tutoriel développeur

https://www.vogella.com/tutorials/AsciiDoc/article.html

---

== Règles d'équipe proposées

Pour chaque Merge Request :

* Code revu
* Tests passés
* Documentation mise à jour

Une fonctionnalité n'est pas terminée sans documentation.

---

== Bénéfices

* Documentation toujours à jour
* Historique Git
* Navigation centralisée
* Contribution simplifiée
* Onboarding plus rapide
* Réduction de la dette documentaire

---

== Conclusion

[.lead]
Une fonctionnalité est terminée lorsque le code *et* la documentation sont livrés ensemble.

Merci !

Questions ?

Bonus

Je te conseille d'ajouter ensuite dans la documentation LuPIA un menu :

== Outils

* xref:tools/antora-poc.adoc[POC Antora]
* xref:tools/asciidoc-cheatsheet.adoc[Cheat Sheet AsciiDoc]


puis une page asciidoc-cheatsheet.adoc de 2 pages maximum (titres, listes, tableaux, images, xref, PlantUML). C'est généralement celle que les développeurs utilisent au quotidien.
