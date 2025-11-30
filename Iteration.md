Merci beaucoup d'avoir partagé la logique interne de `isFustaDto` \! Cette information est absolument cruciale pour comprendre pourquoi votre test échoue.

L'extrait que vous montrez confirme que la méthode `isFustaDto` est un **test de conformité extrêmement strict** qui impose des règles de **conception de DTO (Data Transfer Object)** basées sur l'encapsulation et la complexité.

-----

## 🛑 Analyse de la Logique `isFustaDto`

La logique que vous avez fournie montre que le test vérifie les types de champs récursifs et, si le mode est `ValidationMode.INPUT`, il échoue immédiatement si certaines conditions sont remplies :

```java
if (INPUT == validationMode) {
    if(checkEncapsulate && TypeHelper.isPrimitiveOrWrapper(actual)) {
        failWithMessage(........);
    }
    if(checkEncapsulate && TypeHelper.isEnum(actual)) {
        failWithMessage(........);
    }
    if(checkEncapsulate && TypeHelper.isIterable(actual)) {
        failWithMessage(........);
    }
    // ... et d'autres types simples ou collections non autorisées à ce niveau
}
```

### L'Échec : Type Inapproprié au Mauvais Niveau

L'échec `cycle reference` ne vient **pas** d'une boucle éternelle dans le sens classique, mais du fait que l'inspecteur de Fusta, en explorant récursivement votre objet, trouve un type de données qu'il **interdit à un niveau profond** de l'arborescence des DTO.

Voici le chemin d'inspection :

1.  **Niveau 0 (Racine) :** `QualityProfileRulesRequest` (doit passer).
2.  **Niveau 1 :** Le champ `bookmark` de type **`RsaBookmark`** (doit passer).
3.  **Niveau 2 :** Le champ `sortProperties` (`List<SortProperty<P>>`) de type **`List`** (doit échouer ici si l'inspection continue).

**La cause de l'erreur est que l'un des types suivants est inspecté de manière récursive, et Fusta le considère comme invalide pour un DTO d'entrée :**

1.  **L'itération elle-même :** Si l'inspecteur tente de valider la `List<SortProperty<P>>` et tombe sur :
    `if(checkEncapsulate && TypeHelper.isIterable(actual))`
    Il échoue en disant que les `Iterable` (Collections/Listes) ne devraient pas être des membres d'un DTO (ou du moins pas sans encapsulation spécifique, selon votre règle `checkEncapsulate`).

2.  **L'énumération profonde :** Si Fusta est assez intelligent pour descendre à l'élément `P` de la liste et tombe sur :
    `if(checkEncapsulate && TypeHelper.isEnum(actual))`
    Il échoue car il trouve une **Énumération** (`P` ou `SortDirection`) à un niveau jugé trop profond ou mal encapsulé.

-----

## 💡 La Vraie Signification de l'Erreur

Le message d'erreur d'origine :

> `java.lang.AssertionError: QualityProfileRulesRequest .bookmark<RsaBookmark>: of type '...pageable.RsaBookmark' which should be avoided on type '...pageable.RsaBookmark' has **cycle reference**, please refactor using another data type`

L'outil Fusta réutilise le message d'erreur standard de "cycle reference" pour masquer une erreur de **violation de structure de DTO**. Il essaie de valider `RsaBookmark` (ou un de ses enfants), trouve un type interdit (comme une `List` ou un `Enum`), et échoue, en utilisant ce message générique d'interdiction de dépendance complexe.

## ✅ La Seule Solution Possible (Ajustée aux Règles Fusta)

Puisque Fusta interdit certains types, la seule façon de passer le test tout en conservant la structure métier de la pagination est d'**encapsuler les types interdits** dans de nouveaux DTO simples.

### Étape 1 : Encapsuler l'Itérable (`List`)

Si la validation échoue sur l'itération (`isIterable(actual)`), vous devez encapsuler la liste dans une classe conteneur :

**Au lieu de :**

```java
public final class RsaBookmark<P extends Enum<P>> {
   // ...
   private List<SortProperty<P>> sortProperties; // ❌ Listes interdites
   // ...
}
```

**Faites ceci :**

1.  Créez une classe simple pour contenir la liste :

    ```java
    // DTO simple pour encapsuler l'itération
    public class SortPropertiesContainer<P extends Enum<P>> {
        private List<SortProperty<P>> values;
        // Getters/Setters/Constructeurs
    }
    ```

2.  Mettez à jour `RsaBookmark` :

    ```java
    public final class RsaBookmark<P extends Enum<P>> {
       // ...
       private SortPropertiesContainer<P> sortProperties; // ✅ Maintenant encapsulé
       // ...
    }
    ```

### Étape 2 : Encapsuler l'Énumération (Si l'Échec Persiste)

Si l'échec se produit sur l'énumération `P` ou `SortDirection` (`isEnum(actual)`), vous devez créer un DTO pour chaque énumération et le référencer. C'est plus lourd, mais requis par cette règle stricte.

**Au lieu de :**

```java
// Dans SortProperty
private P property; // ❌ Enum interdite
private SortDirection direction; // ❌ Enum interdite
```

**Faites ceci (exemple pour `SortDirection`) :**

1.  Créez un DTO pour l'énumération (qui contient simplement un `String` ou `int` représentant l'Enum) :

    ```java
    public class SortDirectionDto {
        private String value; // Ex: "ASC", "DESC"
        // Getters/Setters
    }
    ```

2.  Utilisez le DTO dans `SortProperty` :

    ```java
    // Dans SortProperty
    private SortDirectionDto direction; // ✅ Maintenant un DTO encapsulé
    ```

-----

**Conclusion :** Le framework Fusta n'autorise pas les types simples ou les collections (`Iterable`) à apparaître comme des champs de DTO sans être enveloppés dans un **objet de classe non primitif/non-Enum/non-Iterable**.

**Votre meilleure stratégie est de commencer par l'Étape 1 (encapsuler la `List<SortProperty>`) car c'est le point d'échec le plus fréquent pour un test de conformité aussi strict.**
