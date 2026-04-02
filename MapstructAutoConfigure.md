Le problème est clair : MapstructHelper.build() est une factory method statique appelée au moment de l'enregistrement de la BeanDefinition — à ce stade, le contexte Spring n'est pas encore complètement initialisé, donc les dépendances des mappers ne sont pas disponibles.


La solution
Il faut transformer build() en factory method lazy via FactoryBean<M> — Spring n'appellera build() qu'au moment où le bean est réellement demandé, quand toutes les dépendances sont disponibles.

```java
package com.company.starter.mapstruct;

import com.company.starter.mapstruct.MapstructMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotatedBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.InterfaceTypeFilter;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

/**
 * {@link BeanFactoryPostProcessor} responsable de l'enregistrement automatique
 * des mappers MapStruct dans le contexte Spring.
 *
 * <p>Deux conditions d'inclusion (OR) :
 * <ul>
 *   <li>Condition 1 : l'interface implémente {@link MapstructMapper}</li>
 *   <li>Condition 2 : classe générée par MapStruct — annotée {@code @Component},
 *       nom terminant par {@value #MAPSTRUCT_IMPL_SUFFIX}, héritant d'une interface
 *       du même nom sans le suffixe (convention {@code componentModel = SPRING})</li>
 * </ul>
 *
 * <p>Les beans sont enregistrés via {@link MapstructFactoryBean} pour garantir
 * une instanciation lazy après initialisation complète du contexte Spring,
 * supportant ainsi {@code InjectionStrategy.CONSTRUCTOR}.
 *
 * <p><b>Contrainte pour la condition 2</b> : les mappers doivent être annotés avec
 * {@code @Mapper(componentModel = MappingConstants.ComponentModel.SPRING)}.
 * Le suffixe {@value #MAPSTRUCT_IMPL_SUFFIX} ne doit pas être modifié
 * via {@code implementationSuffix}.
 */
public class MapstructAutoConfigure implements BeanFactoryPostProcessor {

    private static final Logger log =
            LoggerFactory.getLogger(MapstructAutoConfigure.class);

    /**
     * Suffixe conventionnel des implémentations générées par MapStruct.
     * Correspond à la valeur par défaut de {@code @Mapper(implementationSuffix)}.
     */
    private static final String MAPSTRUCT_IMPL_SUFFIX = "Impl";

    // -------------------------------------------------------------------------
    // BeanFactoryPostProcessor
    // -------------------------------------------------------------------------

    @Override
    public void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory)
            throws BeansException {

        Set<BeanDefinition> mapStructMappers = findMapstructMappers();

        if (mapStructMappers.isEmpty()) {
            log.debug("No MapStruct mappers found in classpath [{}]", MAPSTRUCT_PATH);
            return;
        }

        log.debug("Found {} MapStruct mapper(s) to register", mapStructMappers.size());

        mapStructMappers.forEach(beanDefinition ->
                registerMapper(beanFactory, beanDefinition)
        );
    }

    // -------------------------------------------------------------------------
    // Enregistrement
    // -------------------------------------------------------------------------

    /**
     * Enregistre un mapper comme {@link MapstructFactoryBean} dans le contexte Spring.
     *
     * <p>Si le bean est déjà enregistré (ex : déclaré manuellement via {@code @Bean}),
     * l'enregistrement est ignoré pour ne pas écraser la définition existante.
     */
    private void registerMapper(final ConfigurableListableBeanFactory beanFactory,
                                final BeanDefinition beanDefinition) {

        final Class<?> mapperClass = TypeHelper
                .getClassForName(beanDefinition.getBeanClassName(), MAPSTRUCT_PATH)
                .orElseThrow(() -> new BeanCreationException(
                        "Failed to resolve MapStruct mapper class: "
                                + beanDefinition.getBeanClassName()));

        final String beanName = resolveBeanName(mapperClass);

        if (beanFactory.containsBeanDefinition(beanName)) {
            log.debug("Mapper already registered, skipping: {}", beanName);
            return;
        }

        ((BeanDefinitionRegistry) beanFactory)
                .registerBeanDefinition(beanName, buildFactoryBeanDefinition(mapperClass));

        log.debug("Registered MapStruct mapper [{}] as bean [{}]",
                mapperClass.getName(), beanName);
    }

    /**
     * Construit la {@link RootBeanDefinition} du {@link MapstructFactoryBean}.
     *
     * <p>{@link MapstructFactoryBean} garantit que l'instanciation du mapper
     * est différée après initialisation complète du contexte Spring, permettant
     * la résolution correcte des dépendances constructeur générées par MapStruct
     * ({@code InjectionStrategy.CONSTRUCTOR}).
     */
    private RootBeanDefinition buildFactoryBeanDefinition(final Class<?> mapperClass) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(MapstructFactoryBean.class);
        beanDefinition.setTargetType(mapperClass);
        beanDefinition.getConstructorArgumentValues()
                      .addIndexedArgumentValue(0, mapperClass);
        return beanDefinition;
    }

    /**
     * Résout le nom du bean à partir de la classe du mapper.
     *
     * <p>Suit la convention Spring : nom simple en camelCase.
     * Ex : {@code com.company.mapper.UserMapper} → {@code userMapper}
     */
    private String resolveBeanName(final Class<?> mapperClass) {
        String simpleName = mapperClass.getSimpleName();
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }

    // -------------------------------------------------------------------------
    // Scan du classpath
    // -------------------------------------------------------------------------

    /**
     * Scanne le classpath pour détecter les mappers MapStruct éligibles.
     *
     * <p>Conditions d'inclusion (OR) :
     * <ul>
     *   <li>Condition 1 : implémente {@link MapstructMapper} via {@link InterfaceTypeFilter}</li>
     *   <li>Condition 2 : convention {@code *Impl} — voir {@link #buildImplConventionFilter()}</li>
     * </ul>
     *
     * <p>Conditions d'exclusion (évaluées après les inclusions) :
     * <ul>
     *   <li>L'interface {@link MapstructMapper} elle-même</li>
     *   <li>Les classes internes du starter ({@code XX_STARTER_MAPSTRUCT_PATH})</li>
     * </ul>
     */
    private static Set<BeanDefinition> findMapstructMappers() {
        final ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false) {
                    @Override
                    protected boolean isCandidateComponent(
                            final AnnotatedBeanDefinition beanDefinition) {
                        // Nécessaire : Spring exclut nativement les interfaces
                        // et classes abstraites. Les mappers étant des interfaces,
                        // cette surcharge est indispensable.
                        return true;
                    }
                };

        // Condition 1 (OR) — interface qui extends MapstructMapper
        // InterfaceTypeFilter Spring natif — aucune dépendance ASM requise
        scanner.addIncludeFilter(new InterfaceTypeFilter(MapstructMapper.class));

        // Condition 2 (OR) — classe générée par MapStruct via componentModel=SPRING
        scanner.addIncludeFilter(buildImplConventionFilter());

        // Exclusions — évaluées APRÈS les includeFilters (comportement Spring natif)
        scanner.addExcludeFilter(ClassExcludeFilter.build(MapstructMapper.class));
        scanner.addExcludeFilter(new RegexPatternTypeFilter(XX_STARTER_MAPSTRUCT_PATH));

        return scanner.findCandidateComponents(MAPSTRUCT_PATH);
    }

    /**
     * Construit le filtre de détection par convention de nommage MapStruct.
     *
     * <p>Un bean est accepté si les trois conditions suivantes sont toutes vraies (AND) :
     * <ol>
     *   <li>Son nom simple se termine par {@value #MAPSTRUCT_IMPL_SUFFIX}</li>
     *   <li>Il est annoté {@code @Component} (généré par {@code componentModel = SPRING})</li>
     *   <li>Il hérite d'une interface dont le nom FQN est identique au sien
     *       sans le suffixe {@value #MAPSTRUCT_IMPL_SUFFIX}</li>
     * </ol>
     *
     * <p>Exemple : {@code com.company.UserMapperImpl} est accepté si :
     * <ul>
     *   <li>nom termine par "Impl" ✅</li>
     *   <li>porte @Component ✅</li>
     *   <li>implémente {@code com.company.UserMapper} ✅</li>
     * </ul>
     */
    private static TypeFilter buildImplConventionFilter() {
        return (metadataReader, metadataReaderFactory) -> {
            ClassMetadata meta = metadataReader.getClassMetadata();
            String className = meta.getClassName();
            String simpleName = className.substring(className.lastIndexOf('.') + 1);

            // AND 1 — nom se termine par "Impl"
            if (!simpleName.endsWith(MAPSTRUCT_IMPL_SUFFIX)) {
                return false;
            }

            // AND 2 — annoté @Component
            // @Component a @Retention(RUNTIME) → AnnotationMetadata fonctionne
            if (!metadataReader.getAnnotationMetadata()
                               .hasAnnotation(Component.class.getName())) {
                return false;
            }

            // AND 3 — hérite d'une interface dont le FQN = className sans "Impl"
            String expectedInterface = className.substring(
                    0, className.length() - MAPSTRUCT_IMPL_SUFFIX.length()
            );

            return Arrays.asList(meta.getInterfaceNames())
                         .contains(expectedInterface);
        };
    }
}
```

---

### Vue d'ensemble de la logique de scan
```
findMapstructMappers()
        │
        ├── includeFilter 1 : InterfaceTypeFilter(MapstructMapper)
        │       → UserMapper extends MapstructMapper          ✅
        │
        ├── includeFilter 2 : buildImplConventionFilter()
        │       → nom *Impl                                   AND
        │       → @Component                                  AND
        │       → hérite interface même nom sans Impl
        │               → UserMapperImpl                      ✅
        │
        └── excludeFilter 1 : MapstructMapper.class elle-même ❌
            excludeFilter 2 : XX_STARTER_MAPSTRUCT_PATH       ❌```

```java
public class MapstructHelper {

    private static final Logger log = LoggerFactory.getLogger(MapstructHelper.class);

    /**
     * Ancienne signature — conservée pour compatibilité ascendante.
     * Utilisée uniquement si aucun BeanFactory n'est disponible.
     */
    public static <S, T, M extends MapstructMapper<S, T>> M build(final Class<M> mapperClazz) {
        return build(mapperClazz, null);
    }

    /**
     * Nouvelle signature — résout les dépendances du constructeur via BeanFactory.
     *
     * Ordre de résolution :
     * 1. Constructeur avec dépendances (InjectionStrategy.CONSTRUCTOR)
     *    → résolution via BeanFactory (contexte complet disponible)
     * 2. Constructeur sans argument (pas de dépendances)
     * 3. Fallback proxy si échec
     */
    public static <S, T, M extends MapstructMapper<S, T>> M build(
            final Class<M> mapperClazz,
            final BeanFactory beanFactory) {
        try {
            // Cherche l'implémentation générée par MapStruct
            // ex: UserMapper → UserMapperImpl
            Class<M> implClass = resolveImplClass(mapperClazz);

            Constructor<?>[] constructors = implClass.getDeclaredConstructors();
            Constructor<?> constructor = resolveConstructor(constructors);

            if (constructor.getParameterCount() == 0) {
                // Pas de dépendances — instanciation directe
                constructor.setAccessible(true);
                return mapperClazz.cast(constructor.newInstance());
            }

            // Constructeur avec dépendances — résolution via BeanFactory
            if (beanFactory == null) {
                log.warn("BeanFactory unavailable for {}, falling back to proxy", 
                    mapperClazz.getSimpleName());
                return buildProxy(mapperClazz);
            }

            Object[] dependencies = resolveDependencies(constructor, beanFactory);
            constructor.setAccessible(true);
            return mapperClazz.cast(constructor.newInstance(dependencies));

        } catch (RuntimeException e) {
            log.warn("Failed to build mapper {}, falling back to proxy",
                mapperClazz.getSimpleName(), e);
            return buildProxy(mapperClazz);
        } catch (Exception e) {
            log.warn("Failed to build mapper {}, falling back to proxy",
                mapperClazz.getSimpleName(), e);
            return buildProxy(mapperClazz);
        }
    }

    /**
     * Résout les dépendances du constructeur via le BeanFactory Spring.
     * À ce stade (FactoryBean.getObject()), tous les beans sont disponibles.
     */
    private static Object[] resolveDependencies(Constructor<?> constructor,
                                                 BeanFactory beanFactory) {
        return Arrays.stream(constructor.getParameterTypes())
                     .map(beanFactory::getBean)
                     .toArray();
    }

    /**
     * Sélectionne le constructeur le plus adapté :
     * - priorité au constructeur avec le plus de paramètres (généré par MapStruct)
     * - fallback sur le constructeur sans argument
     */
    private static Constructor<?> resolveConstructor(Constructor<?>[] constructors) {
        return Arrays.stream(constructors)
                     .max(Comparator.comparingInt(Constructor::getParameterCount))
                     .orElseThrow(() -> new IllegalStateException("No constructor found"));
    }

    private static <M> Class<M> resolveImplClass(Class<M> mapperClazz) {
        // Convention MapStruct : UserMapper → UserMapperImpl
        // ...implémentation existante inchangée
    }
}
```


Les trois points clés du refactoring par rapport à l'implémentation initiale sont le remplacement de la factory method statique `MapstructHelper.build()` par `MapstructFactoryBean` pour garantir l'instanciation lazy, l'ajout du guard `containsBeanDefinition` pour éviter d'écraser les beans déclarés manuellement, et l'extraction de `buildFactoryBeanDefinition()` et `resolveBeanName()` pour isoler les responsabilités et faciliter les tests unitaires.
