package com.company.starter.mapstruct;

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
 *   <li>Condition 1 : l'interface implémente {@link MapstructMapper}
 *       — détectée via {@link InterfaceTypeFilter} (Spring natif, sans ASM).</li>
 *   <li>Condition 2 : classe générée par MapStruct via convention de nommage
 *       — annotée {@code @Component}, nom terminant par {@value #MAPSTRUCT_IMPL_SUFFIX},
 *       héritant d'une interface du même nom sans le suffixe.</li>
 * </ul>
 *
 * <p>Les beans sont enregistrés via {@link MapstructFactoryBean} pour garantir
 * une instanciation lazy après initialisation complète du contexte Spring,
 * supportant ainsi {@code InjectionStrategy.CONSTRUCTOR}.
 *
 * <p><b>Contraintes pour la Condition 2</b> :
 * <ul>
 *   <li>{@code @Mapper(componentModel = MappingConstants.ComponentModel.SPRING)} obligatoire</li>
 *   <li>{@code implementationSuffix} ne doit pas être modifié
 *       (valeur par défaut {@value #MAPSTRUCT_IMPL_SUFFIX} requise)</li>
 * </ul>
 */
public class MapstructAutoConfigure implements BeanFactoryPostProcessor {

    private static final Logger log =
            LoggerFactory.getLogger(MapstructAutoConfigure.class);

    /**
     * Suffixe conventionnel des implémentations générées par MapStruct.
     * Correspond à la valeur par défaut de {@code @Mapper(implementationSuffix)}.
     * Doit rester cohérent avec {@code MapstructHelper#IMPL_SUFFIX}.
     */
    private static final String MAPSTRUCT_IMPL_SUFFIX = "Impl";

    // -------------------------------------------------------------------------
    // BeanFactoryPostProcessor
    // -------------------------------------------------------------------------

    @Override
    public void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory)
            throws BeansException {

        final Set<BeanDefinition> mapStructMappers = findMapstructMappers();

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

        log.debug("Registered MapStruct mapper [{}] as bean [{}]",
                mapperClass.getName(), beanName);
    }

    /**
     * Construit la {@link RootBeanDefinition} du {@link MapstructFactoryBean}.
     *
     * <p>L'utilisation de {@link MapstructFactoryBean} garantit que
     * {@link MapstructHelper#build(Class, org.springframework.beans.factory.BeanFactory)}
     * est appelé uniquement après initialisation complète du contexte Spring,
     * permettant la résolution correcte des dépendances constructeur
     * ({@code InjectionStrategy.CONSTRUCTOR}).
     *
     * @param mapperClass interface (Condition 1) ou implémentation (Condition 2)
     * @return            la définition du bean prête à être enregistrée
     */
    private RootBeanDefinition buildFactoryBeanDefinition(final Class<?> mapperClass) {
        final RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(MapstructFactoryBean.class);
        beanDefinition.setTargetType(mapperClass);
        beanDefinition.getConstructorArgumentValues()
                      .addIndexedArgumentValue(0, mapperClass);
        return beanDefinition;
    }

    /**
     * Résout le nom du bean à partir de la classe du mapper.
     *
     * <p>Suit la convention Spring : nom simple de la classe en camelCase.
     * Ex : {@code com.company.mapper.UserMapper} → {@code userMapper}
     * Ex : {@code com.company.mapper.UserMapperImpl} → {@code userMapperImpl}
     *
     * @param mapperClass la classe du mapper
     * @return            le nom du bean à enregistrer
     */
    private String resolveBeanName(final Class<?> mapperClass) {
        final String simpleName = mapperClass.getSimpleName();
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }

    // -------------------------------------------------------------------------
    // Scan du classpath
    // -------------------------------------------------------------------------

    /**
     * Scanne le classpath pour détecter les mappers MapStruct éligibles.
     *
     * <p>Conditions d'inclusion (OR — comportement natif Spring
     * {@link ClassPathScanningCandidateComponentProvider}) :
     * <ul>
     *   <li>Condition 1 : implémente {@link MapstructMapper}
     *       via {@link InterfaceTypeFilter}</li>
     *   <li>Condition 2 : convention {@code *Impl}
     *       via {@link #buildImplConventionFilter()}</li>
     * </ul>
     *
     * <p>Conditions d'exclusion (évaluées APRÈS les inclusions) :
     * <ul>
     *   <li>L'interface {@link MapstructMapper} elle-même</li>
     *   <li>Les classes internes du starter ({@code XX_STARTER_MAPSTRUCT_PATH})</li>
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
                        // Les mappers MapStruct étant des interfaces (Condition 1),
                        // cette surcharge est indispensable.
                        return true;
                    }
                };

        // Condition 1 (OR) — interface qui extends MapstructMapper
        // InterfaceTypeFilter Spring natif — aucune dépendance ASM requise.
        // MapstructTypeFilter supprimé — plus nécessaire avec cette approche.
        scanner.addIncludeFilter(new InterfaceTypeFilter(MapstructMapper.class));

        // Condition 2 (OR) — classe *Impl générée par MapStruct (componentModel=SPRING)
        scanner.addIncludeFilter(buildImplConventionFilter());

        // Exclusions — évaluées APRÈS les includeFilters (comportement Spring natif).
        // Le starter lui-même est exclu même si ses classes satisfont les conditions.
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
     *   <li>Il est annoté {@code @Component}
     *       (généré par {@code componentModel = SPRING}) —
     *       {@code @Component} ayant {@code @Retention(RUNTIME)},
     *       {@code AnnotationMetadata.hasAnnotation()} fonctionne sans ASM.</li>
     *   <li>Il hérite d'une interface dont le FQN est identique au sien
     *       sans le suffixe {@value #MAPSTRUCT_IMPL_SUFFIX}</li>
     * </ol>
     *
     * <p>Exemple — {@code com.company.UserMapperImpl} est accepté si :
     * <ul>
     *   <li>nom termine par "Impl" ✅</li>
     *   <li>porte {@code @Component} ✅</li>
     *   <li>implémente {@code com.company.UserMapper} ✅</li>
     * </ul>
     *
     * @return le {@link TypeFilter} de détection par convention
     */
    private static TypeFilter buildImplConventionFilter() {
        return (metadataReader, metadataReaderFactory) -> {
            final ClassMetadata meta = metadataReader.getClassMetadata();
            final String className = meta.getClassName();
            final String simpleName = className.substring(className.lastIndexOf('.') + 1);

            // AND 1 — nom se termine par "Impl"
            if (!simpleName.endsWith(MAPSTRUCT_IMPL_SUFFIX)) {
                return false;
            }

            // AND 2 — annoté @Component (@Retention RUNTIME → AnnotationMetadata OK)
            if (!metadataReader.getAnnotationMetadata()
                               .hasAnnotation(Component.class.getName())) {
                return false;
            }

            // AND 3 — hérite d'une interface dont le FQN = className sans "Impl"
            // Garantit qu'on ne détecte que les implémentations MapStruct
            // et non n'importe quelle classe *Impl annotée @Component
            final String expectedInterface = className.substring(
                    0, className.length() - MAPSTRUCT_IMPL_SUFFIX.length()
            );

            return Arrays.asList(meta.getInterfaceNames())
                         .contains(expectedInterface);
        };
    }
}
