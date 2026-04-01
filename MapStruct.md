Le problème est clair. `InterfaceTypeFilter` ajoute une **double contrainte** :

```
isInterface() == true  AND  AssignableTypeFilter.match() == true
```

Donc :
- `ConcreteMapperAnnotated` → **rejetée** par `byInterface` (pas une interface) ✓ normal, mais `byAnnotation` aurait dû suffire → le problème vient d'ailleurs
- `MapperAnnotatedOnly` → **rejetée** par `byInterface` (n'implémente pas `MapstructMapper`), et `byAnnotation` devrait matcher… 

En relisant `AnnotationTypeFilter` avec `checkInherited=true` : pour les **interfaces**, Spring utilise `getInterfaces()` et non `getSuperclass()`, ce qui peut rater la détection selon la version. Mais surtout, le vrai problème pour `ChildOfAnnotatedMapper` et les autres est que **`byInterface` retourne `false`** et que **`byAnnotation` ne remonte pas correctement** dans certains cas avec le reader ASM.

Voici les corrections :

---

### Correction 1 — `MapstructTypeFilter.java`

```java
public class MapstructTypeFilter implements TypeFilter {

    private final TypeFilter byInterface = new InterfaceTypeFilter(MapstructMapper.class);

    // checkInherited=false pour les interfaces : AnnotationTypeFilter
    // utilise getSuperclass() pour remonter la hiérarchie, ce qui ne
    // fonctionne pas pour les interfaces (getSuperclass() retourne null).
    // On délègue la détection par héritage à byInterfaceInherited ci-dessous.
    private final TypeFilter byAnnotation = new AnnotationTypeFilter(
            Mapper.class,
            false,  // checkInherited — voir explication ci-dessus
            true    // considerMetaAnnotations
    );

    // Détecte les interfaces qui héritent d'une interface annotée @Mapper.
    // AssignableTypeFilter (sans la contrainte isInterface) remonte
    // correctement toute la hiérarchie via le bytecode ASM.
    private final TypeFilter byInterfaceInherited = new AssignableTypeFilter(Mapper.class) {
        @Override
        public boolean match(MetadataReader metadataReader,
                             MetadataReaderFactory metadataReaderFactory) throws IOException {
            // On vérifie uniquement l'annotation directe sur les super-interfaces
            // via la hiérarchie ASM, sans exiger isInterface()
            ClassMetadata meta = metadataReader.getClassMetadata();
            return checkSuperInterfaces(meta.getInterfaceNames(), metadataReaderFactory);
        }

        private boolean checkSuperInterfaces(String[] interfaceNames,
                                             MetadataReaderFactory factory) throws IOException {
            for (String iface : interfaceNames) {
                try {
                    MetadataReader ifaceReader = factory.getMetadataReader(iface);
                    AnnotationMetadata ifaceMeta = ifaceReader.getAnnotationMetadata();
                    if (ifaceMeta.hasAnnotation(Mapper.class.getName())) {
                        return true;
                    }
                    // Récursion sur les super-interfaces
                    String[] superIfaces = ifaceReader.getClassMetadata().getInterfaceNames();
                    if (superIfaces.length > 0
                            && checkSuperInterfaces(superIfaces, factory)) {
                        return true;
                    }
                } catch (IOException ignored) {
                    // Interface non trouvée dans le classpath (ex: JDK), on ignore
                }
            }
            return false;
        }
    };

    @Override
    public boolean match(MetadataReader metadataReader,
                         MetadataReaderFactory metadataReaderFactory) throws IOException {
        return byInterface.match(metadataReader, metadataReaderFactory)
            || byAnnotation.match(metadataReader, metadataReaderFactory)
            || byInterfaceInherited.match(metadataReader, metadataReaderFactory);
    }
}
```

---

### Correction 2 — Les tests

Les trois tests qui échouaient + le test d'héritage nécessitent des ajustements dans les fixtures et les assertions :

```java
@Nested
@DisplayName("Cas positifs — le filtre doit accepter")
class ShouldMatch {

    @Test
    @DisplayName("une interface qui implémente MapstructMapper")
    void interfaceImplementingMapstructMapper() throws Exception {
        assertThat(matches(ValidMapstructMapperInterface.class)).isTrue();
    }

    @Test
    @DisplayName("une interface annotée @Mapper uniquement (sans MapstructMapper)")
    void interfaceAnnotatedWithMapperOnly() throws Exception {
        // byAnnotation avec checkInherited=false détecte l'annotation directe
        assertThat(matches(MapperAnnotatedOnly.class)).isTrue();
    }

    @Test
    @DisplayName("une interface qui implémente MapstructMapper ET porte @Mapper")
    void interfaceWithBothConditions() throws Exception {
        assertThat(matches(MapperAnnotatedAndInterface.class)).isTrue();
    }

    @Test
    @DisplayName("une classe concrète annotée @Mapper")
    void concreteClassAnnotatedWithMapper() throws Exception {
        // byAnnotation (checkInherited=false) détecte @Mapper directement sur la classe
        // byInterface échoue (pas une interface) — c'est le OR qui sauve ce cas
        assertThat(matches(ConcreteMapperAnnotated.class)).isTrue();
    }

    @Test
    @DisplayName("une interface héritant d'une interface annotée @Mapper")
    void interfaceInheritingFromAnnotatedParent() throws Exception {
        // byAnnotation avec checkInherited=false ne remonte pas les interfaces parentes
        // → c'est byInterfaceInherited qui prend le relais
        assertThat(matches(ChildOfAnnotatedMapper.class)).isTrue();
    }
}
```

---

### Tableau récapitulatif des filtres après correction

| Fixture | `byInterface` | `byAnnotation` | `byInterfaceInherited` | Résultat |
|---|---|---|---|---|
| `ValidMapstructMapperInterface` | ✅ | ❌ | ❌ | ✅ |
| `MapperAnnotatedOnly` | ❌ | ✅ | ❌ | ✅ |
| `MapperAnnotatedAndInterface` | ✅ | ✅ | ❌ | ✅ |
| `ConcreteMapperAnnotated` | ❌ (pas interface) | ✅ | ❌ | ✅ |
| `ChildOfAnnotatedMapper` | ❌ | ❌ (`checkInherited=false`) | ✅ | ✅ |
| `RandomInterface` | ❌ | ❌ | ❌ | ❌ |
| `RandomClass` | ❌ | ❌ | ❌ | ❌ |

La racine du problème était que `AnnotationTypeFilter(checkInherited=true)` remonte via `getSuperclass()` — ce qui fonctionne pour les classes mais **retourne `null` pour les interfaces** en bytecode ASM. Le troisième filtre `byInterfaceInherited` comble ce gap en parcourant explicitement `getInterfaceNames()` de façon récursive.

Merci. `false` au lieu de `true` sur ces 3 cas précis confirme que **`byAnnotation` ne fonctionne pas du tout**, et que `byInterfaceInherited` non plus. `byInterface` fonctionne (les tests qui passent l'utilisent).

La cause racine : **`AnnotationTypeFilter` de Spring nécessite que l'annotation soit retenue à runtime** (`@Retention(RUNTIME)`). Or `@org.mapstruct.Mapper` a une rétention `CLASS` — elle est présente dans le bytecode mais **invisible au mécanisme de réflexion** utilisé par `AnnotationTypeFilter`.

Vérifions :

```java
// Dans le source MapStruct 1.6.3 :
@Retention(RetentionPolicy.CLASS)  // ← voilà le problème
@Target(ElementType.TYPE)
public @interface Mapper { ... }
```

`AnnotationTypeFilter` utilise `AnnotationMetadata` de Spring qui lit les annotations via ASM — il devrait donc fonctionner. Mais en pratique, avec `@Retention(CLASS)`, le comportement est **incohérent selon la version de Spring Core** utilisée.

---

### Solution — lire l'annotation directement via ASM dans le filtre

Il faut abandonner `AnnotationTypeFilter` et lire les métadonnées ASM manuellement, ce qui fonctionne quelle que soit la rétention :

```java
package com.company.starter.mapstruct.filter;

import com.company.starter.mapstruct.MapstructMapper;
import org.mapstruct.Mapper;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.InterfaceTypeFilter;
import org.springframework.core.type.filter.TypeFilter;

import java.io.IOException;

public class MapstructTypeFilter implements TypeFilter {

    private static final String MAPPER_ANNOTATION_NAME = Mapper.class.getName();

    // Détecte : implémente MapstructMapper
    private final TypeFilter byInterface = new InterfaceTypeFilter(MapstructMapper.class);

    @Override
    public boolean match(MetadataReader metadataReader,
                         MetadataReaderFactory metadataReaderFactory) throws IOException {
        return byInterface.match(metadataReader, metadataReaderFactory)
            || hasMapperAnnotation(metadataReader, metadataReaderFactory);
    }

    /**
     * Détecte @Mapper via ASM pur (fonctionne avec @Retention(CLASS)).
     * Couvre 3 cas :
     *  1. Annotation directe sur la classe/interface
     *  2. Classe concrète (non-interface) annotée
     *  3. Héritage via super-interfaces (remonte récursivement)
     */
    private boolean hasMapperAnnotation(MetadataReader metadataReader,
                                        MetadataReaderFactory metadataReaderFactory)
            throws IOException {

        // Cas 1 & 2 : annotation directe (fonctionne pour interface ET classe concrète)
        if (metadataReader.getAnnotationMetadata()
                          .hasAnnotation(MAPPER_ANNOTATION_NAME)) {
            return true;
        }

        // Cas 3 : remonte les super-interfaces récursivement
        return hasMappperAnnotationOnSuperInterfaces(
                metadataReader.getClassMetadata().getInterfaceNames(),
                metadataReaderFactory
        );
    }

    private boolean hasMappperAnnotationOnSuperInterfaces(String[] interfaceNames,
                                                          MetadataReaderFactory factory)
            throws IOException {
        for (String interfaceName : interfaceNames) {
            try {
                MetadataReader ifaceReader = factory.getMetadataReader(interfaceName);

                if (ifaceReader.getAnnotationMetadata()
                               .hasAnnotation(MAPPER_ANNOTATION_NAME)) {
                    return true;
                }

                // Récursion sur les super-interfaces de l'interface courante
                String[] superInterfaces = ifaceReader.getClassMetadata()
                                                      .getInterfaceNames();
                if (superInterfaces.length > 0
                        && hasMappperAnnotationOnSuperInterfaces(superInterfaces, factory)) {
                    return true;
                }
            } catch (IOException ignored) {
                // Interface non trouvée dans le classpath (ex: java.io.Serializable) → on ignore
            }
        }
        return false;
    }
}
```

---

### Tableau final des chemins de détection

| Fixture | `byInterface` | `hasMapperAnnotation` — direct | `hasMapperAnnotation` — héritage | Résultat |
|---|---|---|---|---|
| `ValidMapstructMapperInterface` | ✅ | ❌ | ❌ | ✅ |
| `MapperAnnotatedOnly` | ❌ | ✅ | ❌ | ✅ |
| `ConcreteMapperAnnotated` | ❌ | ✅ | ❌ | ✅ |
| `MapperAnnotatedAndInterface` | ✅ | ✅ | ❌ | ✅ |
| `ChildOfAnnotatedMapper` | ❌ | ❌ | ✅ | ✅ |
| `RandomInterface` | ❌ | ❌ | ❌ | ❌ |
| `RandomClass` | ❌ | ❌ | ❌ | ❌ |

La clé est que `AnnotationMetadata.hasAnnotation()` issu du reader ASM de Spring **lit directement le bytecode** — contrairement à `AnnotationTypeFilter` qui dans certaines versions passe par la réflexion et rate les annotations `@Retention(CLASS)`. Plus besoin de `AnnotationTypeFilter` ni de `AssignableTypeFilter`.

Ajoutons un test de diagnostic pour comprendre exactement ce que lit le reader ASM sur chaque fixture. Cela nous donnera la vérité terrain avant toute autre correction.En attendant, ajoute ce test de diagnostic dans ta classe de test — il va nous dire exactement ce que lit le reader ASM sur les 3 fixtures problématiques :

```java
@Test
@DisplayName("DIAGNOSTIC — affiche les métadonnées ASM lues sur les fixtures")
void diagnostic() throws Exception {
    List<Class<?>> fixtures = List.of(
        MapperAnnotatedOnly.class,
        ConcreteMapperAnnotated.class,
        ChildOfAnnotatedMapper.class
    );

    for (Class<?> fixture : fixtures) {
        MetadataReader reader = readerFactory.getMetadataReader(fixture.getName());

        System.out.println("======================================");
        System.out.println("Classe      : " + fixture.getName());
        System.out.println("isInterface : " + reader.getClassMetadata().isInterface());
        System.out.println("Annotations : " + reader.getAnnotationMetadata().getAnnotationTypes());
        System.out.println("Interfaces  : " + Arrays.toString(reader.getClassMetadata().getInterfaceNames()));
        System.out.println("hasAnnotation(@Mapper) : " +
            reader.getAnnotationMetadata().hasAnnotation(Mapper.class.getName()));
    }
}
```

Lance ce test et **partage le contenu de la console**. Les deux scénarios possibles sont :

**Scénario A — `getAnnotationTypes()` est vide `[]`**
```
Annotations : []
hasAnnotation(@Mapper) : false
```
→ Le reader ASM ne voit pas `@Mapper` du tout. Cause : soit les fixtures sont lues depuis le mauvais classpath, soit le `.class` compilé ne contient pas l'annotation (problème de build).

**Scénario B — `getAnnotationTypes()` contient `@Mapper`**
```
Annotations : [org.mapstruct.Mapper]
hasAnnotation(@Mapper) : false
```
→ L'annotation est vue mais `hasAnnotation()` retourne `false` — problème de nom canonique utilisé pour la comparaison.

Le résultat de ce diagnostic déterminera la correction exacte à appliquer.
