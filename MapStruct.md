Voici la structure complète et finale de la solution.

Il y a trois fichiers à créer : la classe de filtre dédié `MapstructTypeFilter`, la classe principale `MapstructAutoConfigure`, et l'organisation globale des packages.---

### `filter/MapstructTypeFilter.java`

```java
package com.company.starter.mapstruct.filter;

import com.company.starter.mapstruct.MapstructMapper;
import org.mapstruct.Mapper;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.InterfaceTypeFilter;
import org.springframework.core.type.filter.TypeFilter;

import java.io.IOException;

/**
 * Filtre composite (OR) qui accepte un bean si :
 * <ul>
 *   <li>il implémente l'interface {@link MapstructMapper} (contrat custom du starter)</li>
 *   <li>OU s'il est annoté avec {@link org.mapstruct.Mapper}</li>
 * </ul>
 *
 * <p>Conçu pour être utilisé avec
 * {@link org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider}.
 */
public class MapstructTypeFilter implements TypeFilter {

    /**
     * Détecte les classes/interfaces qui implémentent MapstructMapper.
     */
    private final TypeFilter byInterface = new InterfaceTypeFilter(MapstructMapper.class);

    /**
     * Détecte les classes/interfaces annotées avec @Mapper.
     * - checkInherited = true  : remonte la hiérarchie (super-interfaces, classes parentes)
     * - considerMetaAnnotations = true : prend en compte les méta-annotations
     *   (ex: annotation custom qui porte elle-même @Mapper)
     */
    private final TypeFilter byAnnotation = new AnnotationTypeFilter(
            Mapper.class,
            true,   // checkInherited
            true    // considerMetaAnnotations
    );

    @Override
    public boolean match(MetadataReader metadataReader,
                         MetadataReaderFactory metadataReaderFactory) throws IOException {
        return byInterface.match(metadataReader, metadataReaderFactory)
            || byAnnotation.match(metadataReader, metadataReaderFactory);
    }
}
```

---

### `MapstructAutoConfigure.java`

```java
package com.company.starter.mapstruct;

import com.company.starter.mapstruct.filter.MapstructTypeFilter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.AnnotatedBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

import java.util.Set;

public class MapstructAutoConfigure implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
            throws BeansException {
        Set<BeanDefinition> mapStructMappers = findMapstructMappers();
        // ... reste de la logique inchangée
    }

    private static Set<BeanDefinition> findMapstructMappers() {

        final ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false) {
                    @Override
                    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                        // Nécessaire : Spring exclut nativement les interfaces et classes
                        // abstraites. Les mappers MapStruct étant des interfaces, cette
                        // surcharge est indispensable pour que les deux filtres fonctionnent.
                        return true;
                    }
                };

        // Filtre composite OR : interface MapstructMapper OU annotation @Mapper
        scanner.addIncludeFilter(new MapstructTypeFilter());

        // Exclusions : le starter lui-même et la classe de base de l'interface
        // Les excludeFilters sont évalués APRÈS les includeFilters,
        // donc l'exclusion du starter reste efficace même si ses classes portent @Mapper.
        scanner.addExcludeFilter(ClassExcludeFilter.build(MapstructMapper.class));
        scanner.addExcludeFilter(new RegexPatternTypeFilter(XX_STARTER_MAPSTRUCT_PATH));

        return scanner.findCandidateComponents(MAPSTRUCT_PATH);
    }
}
```

---

### Structure des packages

```
com.company.starter.mapstruct/
├── filter/
│   └── MapstructTypeFilter.java      ← nouveau : logique de détection isolée
├── MapstructAutoConfigure.java       ← refactorisé : utilise MapstructTypeFilter
└── MapstructMapper.java              ← inchangé : interface custom du starter
```

---

### Points de vigilance

**Ordre d'évaluation des filtres** — Spring évalue d'abord les `includeFilter`, puis les `excludeFilter`. L'exclusion via `XX_STARTER_MAPSTRUCT_PATH` continuera donc bien à masquer les classes internes du starter, même si elles portent `@Mapper`.

**`isCandidateComponent` surchargée** — sans cette surcharge, `ClassPathScanningCandidateComponentProvider` rejette silencieusement toutes les interfaces (ce qui ferait manquer 100% des mappers MapStruct). C'est le comportement Spring par défaut, à ne surtout pas oublier lors d'une future refactorisation de la classe.

**Testabilité de `MapstructTypeFilter`** — l'extraction dans une classe dédiée permet de la tester unitairement avec un `SimpleMetadataReaderFactory` sans avoir à instancier un `ApplicationContext` complet.
