Le problème est clair : MapstructHelper.build() est une factory method statique appelée au moment de l'enregistrement de la BeanDefinition — à ce stade, le contexte Spring n'est pas encore complètement initialisé, donc les dépendances des mappers ne sont pas disponibles.


La solution
Il faut transformer build() en factory method lazy via FactoryBean<M> — Spring n'appellera build() qu'au moment où le bean est réellement demandé, quand toutes les dépendances sont disponibles.

```java
package com.company.starter.mapstruct;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.config.DependencyDescriptor;

/**
 * FactoryBean lazy — instancie le mapper MapStruct uniquement quand Spring
 * en a besoin, après initialisation complète du contexte.
 *
 * Supporte InjectionStrategy.CONSTRUCTOR : les dépendances du constructeur
 * généré par MapStruct sont résolues via le BeanFactory au moment de
 * la création effective du bean.
 */
public class MapstructFactoryBean<M extends MapstructMapper<?, ?>>
        implements FactoryBean<M>, BeanFactoryAware {

    private final Class<M> mapperClass;
    private BeanFactory beanFactory;

    public MapstructFactoryBean(Class<M> mapperClass) {
        this.mapperClass = mapperClass;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    /**
     * Appelé par Spring APRÈS initialisation du contexte.
     * À ce stade, toutes les dépendances du constructeur sont disponibles.
     */
    @Override
    public M getObject() {
        return MapstructHelper.build(mapperClass, beanFactory);
    }

    @Override
    public Class<M> getObjectType() {
        return mapperClass;
    }

    @Override
    public boolean isSingleton() {
        return true; // les mappers sont sans état
    }
}
```

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

Voici l'implémentation complète et finale de `MapstructAutoConfigure` :

```java
package com.company.starter.mapstruct;

import com.company.starter.mapstruct.filter.MapstructTypeFilter;
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
import org.springframework.core.type.filter.RegexPatternTypeFilter;

import java.util.Set;

/**
 * {@link BeanFactoryPostProcessor} responsable de l'enregistrement automatique
 * des mappers MapStruct dans le contexte Spring.
 *
 * <p>Détecte les mappers via {@link MapstructTypeFilter} (interface
 * {@link MapstructMapper} OU annotation {@code @org.mapstruct.Mapper}),
 * puis les enregistre comme {@link MapstructFactoryBean} pour garantir
 * une instanciation lazy — après initialisation complète du contexte Spring.
 *
 * <p>Supporte {@code InjectionStrategy.CONSTRUCTOR} : les dépendances
 * du constructeur généré par MapStruct sont résolues via le {@code BeanFactory}
 * au moment de la création effective du bean, et non au démarrage.
 */
public class MapstructAutoConfigure implements BeanFactoryPostProcessor {

    private static final Logger log =
            LoggerFactory.getLogger(MapstructAutoConfigure.class);

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

    /**
     * Enregistre un mapper comme {@link MapstructFactoryBean} dans le contexte Spring.
     *
     * <p>Si le bean est déjà enregistré (ex: déclaré manuellement via {@code @Bean}),
     * l'enregistrement est ignoré pour ne pas écraser la définition existante.
     *
     * @param beanFactory    le BeanFactory Spring
     * @param beanDefinition la définition du mapper détecté par le scanner
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

        log.debug("Registered MapStruct mapper [{}] as [{}]",
                mapperClass.getName(), beanName);
    }

    /**
     * Construit la {@link RootBeanDefinition} du {@link MapstructFactoryBean}.
     *
     * <p>L'utilisation d'un {@link MapstructFactoryBean} garantit que
     * {@link MapstructHelper#build(Class, org.springframework.beans.factory.BeanFactory)}
     * est appelé uniquement après initialisation complète du contexte Spring,
     * résolvant ainsi les dépendances du constructeur MapStruct correctement.
     *
     * @param mapperClass la classe de l'interface mapper
     * @return la définition du bean prête à être enregistrée
     */
    private RootBeanDefinition buildFactoryBeanDefinition(final Class<?> mapperClass) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition();

        // MapstructFactoryBean instancie le mapper de façon lazy,
        // après que le contexte Spring soit complètement initialisé.
        // Remplace l'ancienne factory method statique MapstructHelper.build()
        // qui était appelée trop tôt (dépendances non encore disponibles).
        beanDefinition.setBeanClass(MapstructFactoryBean.class);
        beanDefinition.setTargetType(mapperClass);

        // Passe la classe du mapper au constructeur de MapstructFactoryBean
        beanDefinition.getConstructorArgumentValues()
                      .addIndexedArgumentValue(0, mapperClass);

        return beanDefinition;
    }

    /**
     * Résout le nom du bean à partir de la classe du mapper.
     *
     * <p>Suit la convention Spring : nom simple de la classe en camelCase.
     * Ex: {@code com.company.mapper.UserMapper} → {@code userMapper}
     *
     * @param mapperClass la classe du mapper
     * @return le nom du bean à enregistrer
     */
    private String resolveBeanName(final Class<?> mapperClass) {
        String simpleName = mapperClass.getSimpleName();
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }

    /**
     * Scanne le classpath pour détecter les mappers MapStruct éligibles.
     *
     * <p>Critères d'inclusion (OR) :
     * <ul>
     *   <li>implémente {@link MapstructMapper}</li>
     *   <li>porte l'annotation {@code @org.mapstruct.Mapper}</li>
     * </ul>
     *
     * <p>Critères d'exclusion :
     * <ul>
     *   <li>l'interface {@link MapstructMapper} elle-même</li>
     *   <li>les classes internes du starter ({@code XX_STARTER_MAPSTRUCT_PATH})</li>
     * </ul>
     *
     * @return l'ensemble des {@link BeanDefinition} des mappers détectés
     */
    private static Set<BeanDefinition> findMapstructMappers() {
        final ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false) {
                    @Override
                    protected boolean isCandidateComponent(
                            final AnnotatedBeanDefinition beanDefinition) {
                        // Spring exclut nativement les interfaces et classes abstraites.
                        // Les mappers MapStruct étant des interfaces, cette surcharge
                        // est indispensable pour que les deux filtres fonctionnent.
                        return true;
                    }
                };

        // Filtre composite OR : interface MapstructMapper OU annotation @Mapper.
        // Utilise Spring ASM (org.springframework.asm) pour lire le bytecode
        // directement — fonctionne avec @Retention(CLASS) de @org.mapstruct.Mapper.
        scanner.addIncludeFilter(new MapstructTypeFilter());

        // Exclusions évaluées APRÈS les includeFilters.
        // Le starter lui-même est exclu même si ses classes portent @Mapper.
        scanner.addExcludeFilter(ClassExcludeFilter.build(MapstructMapper.class));
        scanner.addExcludeFilter(new RegexPatternTypeFilter(XX_STARTER_MAPSTRUCT_PATH));

        return scanner.findCandidateComponents(MAPSTRUCT_PATH);
    }
}
```

---

### Vue d'ensemble des interactions entre classes

```
MapstructAutoConfigure          MapstructFactoryBean         MapstructHelper
        │                               │                          │
postProcessBeanFactory()                │                          │
        │                               │                          │
        ├── findMapstructMappers()       │                          │
        │   └── MapstructTypeFilter     │                          │
        │                               │                          │
        └── registerMapper()            │                          │
            └── buildFactoryBeanDefinition()                       │
                └── RootBeanDefinition(MapstructFactoryBean) ──────┤
                                        │                          │
                    [contexte Spring complètement initialisé]      │
                                        │                          │
                    Premier appel au mapper (injection)            │
                                        │                          │
                                  getObject()                      │
                                        └── build(class, factory) ─┤
                                                └── resolveDependencies()
                                                    └── constructor.newInstance(deps)
```

Les trois points clés du refactoring par rapport à l'implémentation initiale sont le remplacement de la factory method statique `MapstructHelper.build()` par `MapstructFactoryBean` pour garantir l'instanciation lazy, l'ajout du guard `containsBeanDefinition` pour éviter d'écraser les beans déclarés manuellement, et l'extraction de `buildFactoryBeanDefinition()` et `resolveBeanName()` pour isoler les responsabilités et faciliter les tests unitaires.
