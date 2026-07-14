= Cheat Sheet AsciiDoc
:toc:
:toclevels: 2

Cette fiche regroupe les éléments AsciiDoc les plus utilisés dans la documentation LuPIA.

== Titres

[source,adoc]
----
= Titre du document

== Section

=== Sous-section

==== Niveau 4
----

== Mise en forme

[source,adoc]
----
*gras*

_italique_

`code`

https://www.antora.org[Site Antora]
----

Résultat :

*gras*

_italique_

`code`

== Listes

=== Liste à puces

[source,adoc]
----
* Élément 1
* Élément 2
** Sous-élément
* Élément 3
----

=== Liste numérotée

[source,adoc]
----
. Étape 1
. Étape 2
. Étape 3
----

== Blocs d'information

=== Note

[source,adoc]
----
[NOTE]
====
Texte de la note.
====
----

=== Warning

[source,adoc]
----
[WARNING]
====
Information importante.
====
----

== Blocs de code

=== Java

[source,java]
----
public class HelloWorld {

    public static void main(String[] args) {
        System.out.println("Hello LuPIA");
    }

}
----

Syntaxe source :

[source,adoc]
----
[source,java]
----
public class HelloWorld {

    public static void main(String[] args) {
        System.out.println("Hello LuPIA");
    }

}
----
----

=== YAML

[source,yaml]
----
site:
  title: LuPIA Documentation
----

== Tableaux

[source,adoc]
----
[cols="1,2", options="header"]
|===
|Propriété |Description

|replay.enabled
|Activation du replay

|replay.batch-size
|Taille du lot

|===
----

Résultat :

[cols="1,2", options="header"]
|===
|Propriété |Description

|replay.enabled
|Activation du replay

|replay.batch-size
|Taille du lot

|===

== Images

=== Organisation

[source]
----
modules/ROOT/assets/images/
└── architecture/
    └── overview.png
----

=== Affichage

[source,adoc]
----
image::architecture/overview.png[Architecture globale]
----

=== Avec dimensions

[source,adoc]
----
image::architecture/overview.png[
    Architecture globale,
    width=800,
    align=center
]
----

== Liens internes (xref)

=== Page du même module

[source,adoc]
----
xref:index.adoc[Accueil]
----

=== Autre module

[source,adoc]
----
xref:backend:overview.adoc[Backend]
----

=== Autre composant

[source,adoc]
----
xref:lupia-docs::index.adoc[Documentation LuPIA]
----

== PlantUML

=== Diagramme simple

[source,adoc]
----
[plantuml]
----
@startuml

Alice -> Bob : Hello

@enduml
----
----

=== Exemple d'architecture

[source,adoc]
----
[plantuml]
----
@startuml

queue "MQ" as MQ

component "Parser" as P

database "Elastic" as ES

MQ --> P
P --> ES

@enduml
----
----

== Bonnes pratiques

* Un sujet = une page.
* Utiliser des titres courts et explicites.
* Préférer les diagrammes PlantUML aux images statiques lorsque c'est possible.
* Ajouter la documentation dans la même Merge Request que le développement.
* Utiliser les xref plutôt que des URLs internes.

== Ressources utiles

* Référence rapide : https://docs.asciidoctor.org/asciidoc/latest/syntax-quick-reference/
* Guide officiel : https://asciidoctor.org/docs/asciidoc-writers-guide/
* Documentation Antora : https://docs.antora.org/

[TIP]
====
Pour contribuer à la documentation LuPIA, 90 % des besoins se limitent à :

* Titres
* Listes
* Tableaux
* Images
* Liens xref
* Diagrammes PlantUML

Cette fiche couvre l'essentiel.
====
