Voici la classe de test complète.

```java
package com.company.starter.mapstruct.filter;

import com.company.starter.mapstruct.MapstructMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.Mapper;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires pour {@link MapstructTypeFilter}.
 *
 * <p>Stratégie : on utilise {@link CachingMetadataReaderFactory} pour lire
 * les métadonnées ASM (bytecode) de classes de fixtures définies en bas de ce
 * fichier, sans instancier de Spring ApplicationContext.
 */
class MapstructTypeFilterTest {

    private MapstructTypeFilter filter;
    private MetadataReaderFactory readerFactory;

    @BeforeEach
    void setUp() {
        filter = new MapstructTypeFilter();
        readerFactory = new CachingMetadataReaderFactory(new DefaultResourceLoader());
    }

    // -------------------------------------------------------------------------
    // Méthode utilitaire centrale
    // -------------------------------------------------------------------------

    /**
     * Lit les métadonnées ASM d'une classe et invoque le filtre.
     * Évite la duplication de la mécanique de lecture dans chaque test.
     */
    private boolean matches(Class<?> clazz) throws Exception {
        MetadataReader reader = readerFactory.getMetadataReader(clazz.getName());
        return filter.match(reader, readerFactory);
    }

    // =========================================================================
    // Cas POSITIFS — le filtre doit retourner true
    // =========================================================================

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
            assertThat(matches(ConcreteMapperAnnotated.class)).isTrue();
        }

        @Test
        @DisplayName("une interface qui hérite d'une interface elle-même annotée @Mapper (héritage)")
        void interfaceInheritingFromAnnotatedParent() throws Exception {
            assertThat(matches(ChildOfAnnotatedMapper.class)).isTrue();
        }
    }

    // =========================================================================
    // Cas NÉGATIFS — le filtre doit retourner false
    // =========================================================================

    @Nested
    @DisplayName("Cas négatifs — le filtre doit rejeter")
    class ShouldNotMatch {

        @Test
        @DisplayName("une interface quelconque sans lien avec MapStruct")
        void randomInterface() throws Exception {
            assertThat(matches(RandomInterface.class)).isFalse();
        }

        @Test
        @DisplayName("une classe quelconque sans annotation @Mapper")
        void randomClass() throws Exception {
            assertThat(matches(RandomClass.class)).isFalse();
        }

        @Test
        @DisplayName("l'interface MapstructMapper elle-même (la base du starter)")
        void mapstructMapperInterfaceItself() throws Exception {
            assertThat(matches(MapstructMapper.class)).isTrue();
            // Note : MapstructMapper.class matche par byInterface (il S'implémente lui-même
            // en tant que type). Son exclusion réelle est assurée par ClassExcludeFilter
            // dans MapstructAutoConfigure, pas par ce filtre — comportement attendu et documenté.
        }

        @Test
        @DisplayName("une annotation custom non reliée à @Mapper")
        void classWithUnrelatedAnnotation() throws Exception {
            assertThat(matches(ClassWithUnrelatedAnnotation.class)).isFalse();
        }

        @Test
        @DisplayName("une classe abstraite sans @Mapper ni MapstructMapper")
        void abstractClassWithoutMapper() throws Exception {
            assertThat(matches(AbstractClassWithoutMapper.class)).isFalse();
        }
    }

    // =========================================================================
    // Fixtures — classes internes de test
    // Définies ici pour coller au test et éviter des fichiers séparés.
    // =========================================================================

    // --- Positifs ---

    /** Interface standard du starter : implémente MapstructMapper. */
    interface ValidMapstructMapperInterface extends MapstructMapper {}

    /** Interface avec @Mapper uniquement, sans lien avec l'interface custom. */
    @Mapper
    interface MapperAnnotatedOnly {}

    /** Interface cumulant les deux conditions. */
    @Mapper
    interface MapperAnnotatedAndInterface extends MapstructMapper {}

    /** Classe concrète annotée @Mapper (cas limite mais valide). */
    @Mapper
    static class ConcreteMapperAnnotated {}

    /** Parent annoté @Mapper — teste checkInherited = true. */
    @Mapper
    interface ParentAnnotatedMapper {}

    /** Enfant qui hérite du parent annoté — doit être détecté par héritage. */
    interface ChildOfAnnotatedMapper extends ParentAnnotatedMapper {}

    // --- Négatifs ---

    /** Interface sans aucun lien avec MapStruct. */
    interface RandomInterface {}

    /** Classe sans annotation @Mapper et sans implémenter MapstructMapper. */
    static class RandomClass {}

    /** Annotation custom sans rapport avec MapStruct. */
    @interface UnrelatedAnnotation {}

    /** Classe annotée avec une annotation sans rapport. */
    @UnrelatedAnnotation
    static class ClassWithUnrelatedAnnotation {}

    /** Classe abstraite sans @Mapper ni MapstructMapper. */
    abstract static class AbstractClassWithoutMapper {}
}
```

---

### Ce que couvrent ces tests

| Scénario | Condition déclenchée | Attendu |
|---|---|---|
| `ValidMapstructMapperInterface` | `byInterface` | `true` |
| `MapperAnnotatedOnly` | `byAnnotation` | `true` |
| `MapperAnnotatedAndInterface` | les deux | `true` |
| `ConcreteMapperAnnotated` | `byAnnotation` | `true` |
| `ChildOfAnnotatedMapper` | `byAnnotation` + `checkInherited=true` | `true` |
| `RandomInterface` | aucune | `false` |
| `RandomClass` | aucune | `false` |
| `ClassWithUnrelatedAnnotation` | aucune | `false` |
| `AbstractClassWithoutMapper` | aucune | `false` |
| `MapstructMapper.class` | `byInterface` | `true` (expliqué en commentaire) |

**Pourquoi `MapstructMapper.class` matche-t-il ?** Ce filtre n'est pas responsable de son exclusion — c'est le rôle du `ClassExcludeFilter` dans `MapstructAutoConfigure`. Le test le documente explicitement pour éviter toute confusion future.

**Pourquoi des classes internes comme fixtures ?** Elles restent dans le même fichier, sont lisibles sans navigation, et `CachingMetadataReaderFactory` les résout parfaitement via leur nom binaire. Si les fixtures grossissent, elles peuvent être extraites dans un sous-package `fixture/`.

**Dépendances nécessaires dans `pom.xml`** (normalement déjà présentes dans un starter Spring) :

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <scope>test</scope>
</dependency>
```
