= Starter MapStruct Maison
:toc: left
:sectnums:
:icons: font

== Introduction
Ce starter automatise l'instanciation et l'injection des mappers MapStruct dans le contexte Spring. Il offre un mécanisme de "fallback" robuste via des proxys CGLIB si l'implémentation générée est manquante, garantissant que l'application démarre même en cas de configuration partielle.

== Prérequis de configuration (Obligatoire)

Pour que le starter puisse détecter et instancier vos mappers, vous *devez* respecter scrupuleusement la configuration suivante.

[IMPORTANT]
====
Le non-respect de ces conventions (package, nommage ou visibilité) empêchera le `MapstructHelper` de localiser les implémentations, déclenchant ainsi le mode dégradé (Proxy CGLIB).
====

=== 1. Emplacement et Package (Scan)
Le starter est configuré pour scanner uniquement un périmètre spécifique. Vos interfaces de mapping **doivent** être situées dans le package suivant (ou l'un de ses sous-packages) :

* **Pattern de scan :** `lu.x.**.mapstruct.**`

[TIP]
====
Si votre mapper est placé en dehors de cette arborescence (par exemple dans `lu.x.service.user.mapper`), il ne sera pas détecté par l'auto-configuration.
====

=== 2. Modèle de composant Spring
Le starter s'appuie sur l'injection de dépendances native de Spring pour résoudre les services dont vos mappers pourraient avoir besoin.

[source,java]
----
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper extends MapstructMapper<UserEntity, UserDto> {
    // ...
}
----

=== 3. Convention de nommage (Suffixe)
Le starter utilise une résolution de classe par réflexion basée sur le suffixe standard de MapStruct. 

* **Règle :** Ne modifiez JAMAIS la propriété `implementationSuffix` dans vos annotations `@Mapper`.
* **Valeur attendue :** `Impl` (valeur par défaut requise par le `MapstructHelper`).

=== 4. Visibilité de l'interface
Pour satisfaire aux contraintes de sécurité et permettre l'instanciation sans forcer l'accès (conformité Sonar), le chargement des classes est strict :

* **Visibilité :** Vos interfaces de mapping doivent impérativement être **`public`**.

== Exemple de Mapper standard

[source,java]
----
package lu.x.monprojet.mapstruct.mapper; // Respecte le pattern de scan

import lu.x.starter.mapstruct.MapstructMapper;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MyMapper extends MapstructMapper<Source, Target> {
    // Votre logique de mapping ici
}
----

== Fonctionnement interne (Expert)

Le starter opère en trois étapes :
1. **Scan :** Repère les interfaces du package `lu.x.**.mapstruct.**` étendant `MapstructMapper`.
2. **Résolution :** Localise la classe `Impl` correspondante via le ClassLoader de l'interface (plus robuste pour les environnements de test JUnit).
3. **Instanciation :** Utilise la `BeanFactory` pour injecter les dépendances via le constructeur le plus complet, garantissant un code "Clean" sans accès réflexifs détournés.

=== 2. Modèle de composant et Stratégie d'injection
Le starter s'appuie sur l'injection par constructeur pour garantir l'immuabilité des mappers et une résolution propre des dépendances via la `BeanFactory`.

[source,java]
----
@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR // <1>
)
public interface UserMapper extends MapstructMapper<UserEntity, UserDto> {
    // ...
}
----
<1> **Obligatoire** : Cette stratégie force MapStruct à générer un constructeur public contenant toutes les dépendances. C'est ce constructeur que le starter utilise pour instancier le bean.

[WARNING]
====
Si vous omettez `InjectionStrategy.CONSTRUCTOR`, MapStruct peut générer un constructeur sans argument. Le starter ne pourra alors pas injecter vos dépendances (Services, autres Mappers) lors de l'instanciation manuelle.
====
---
_Dernière mise à jour : Avril 2026_
