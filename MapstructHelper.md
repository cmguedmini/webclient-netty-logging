package com.company.starter.mapstruct;

import net.sf.cglib.proxy.Enhancer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Utilitaire d'instanciation des mappers MapStruct.
 *
 * <p>Gère deux cas selon ce que {@code MapstructAutoConfigure} a détecté :
 * <ul>
 *   <li>Condition 1 — reçoit l'interface ({@code UserMapper extends MapstructMapper}) :
 *       résout {@code UserMapperImpl} via convention de nommage, puis instancie.</li>
 *   <li>Condition 2 — reçoit l'implémentation ({@code UserMapperImpl}) directement :
 *       instancie sans résolution intermédiaire.</li>
 * </ul>
 *
 * <p>Fallback CGLIB via {@link #buildProxy(Class)} — délègue à
 * {@link MapstructMethodInterceptor} qui gère :
 * <ul>
 *   <li>les méthodes {@code default} des interfaces via {@code MethodHandles}</li>
 *   <li>l'implémentation générique {@code toArray} pour {@code ArrayMapper}</li>
 *   <li>le fallback JSON via {@code ObjectMapperHelper} si {@code to()} non implémentée</li>
 *   <li>le délégation à la super-classe pour tous les autres cas</li>
 * </ul>
 */
public class MapstructHelper {

    private static final Logger log = LoggerFactory.getLogger(MapstructHelper.class);

    /**
     * Suffixe conventionnel des implémentations MapStruct.
     * Doit rester cohérent avec {@code MapstructAutoConfigure#MAPSTRUCT_IMPL_SUFFIX}.
     */
    private static final String IMPL_SUFFIX = "Impl";

    /**
     * Intercepteur CGLIB partagé — sans état, thread-safe.
     * Délègue les appels selon la logique de {@link MapstructMethodInterceptor}.
     */
    private static final MethodInterceptor INVOKE_SUPER_INTERCEPTOR =
            new MapstructMethodInterceptor();

    private MapstructHelper() {}

    // -------------------------------------------------------------------------
    // API publique
    // -------------------------------------------------------------------------

    /**
     * Instancie un mapper MapStruct en résolvant ses dépendances via Spring.
     *
     * <p>Appelé par {@link MapstructFactoryBean#getObject()} après initialisation
     * complète du contexte Spring — toutes les dépendances sont disponibles.
     *
     * @param mapperClazz interface (Condition 1) ou implémentation (Condition 2)
     * @param beanFactory contexte Spring pour résoudre les dépendances constructeur
     * @param <S>         type source du mapper
     * @param <T>         type cible du mapper
     * @param <M>         type du mapper
     * @return            instance du mapper prête à l'emploi
     */
    public static <S, T, M extends MapstructMapper<S, T>> M build(
            final Class<M> mapperClazz,
            final BeanFactory beanFactory) {
        try {
            // Condition 1 : reçoit l'interface → résout UserMapperImpl
            // Condition 2 : reçoit UserMapperImpl directement
            Class<? extends M> implClass = mapperClazz.isInterface()
                    ? resolveImplClass(mapperClazz)
                    : mapperClazz;

            Constructor<?> constructor = resolveConstructor(implClass);

            if (constructor.getParameterCount() == 0) {
                // Aucune dépendance — instanciation directe
                constructor.setAccessible(true);
                return mapperClazz.cast(constructor.newInstance());
            }

            if (beanFactory == null) {
                // Ne devrait pas arriver via MapstructFactoryBean
                log.warn("{} can't be instantiated as mapper by mapstruct, "
                        + "provide a cglib/json impl by default",
                        TypeHelper.getSimpleName(mapperClazz));
                return buildProxy(mapperClazz);
            }

            // Résolution des dépendances constructeur via Spring
            // Contexte complet disponible — tous les beans sont accessibles
            Object[] dependencies = resolveDependencies(constructor, beanFactory);
            constructor.setAccessible(true);
            return mapperClazz.cast(constructor.newInstance(dependencies));

        } catch (RuntimeException e) {
            log.warn("{} can't be instantiated as mapper by mapstruct, "
                    + "provide a cglib/json impl by default",
                    TypeHelper.getSimpleName(mapperClazz), e);
            return buildProxy(mapperClazz);

        } catch (Exception e) {
            log.warn("{} can't be instantiated as mapper by mapstruct, "
                    + "provide a cglib/json impl by default",
                    TypeHelper.getSimpleName(mapperClazz), e);
            return buildProxy(mapperClazz);
        }
    }

    // -------------------------------------------------------------------------
    // Résolution de l'implémentation (Condition 1 uniquement)
    // -------------------------------------------------------------------------

    /**
     * Résout la classe d'implémentation générée par MapStruct à partir
     * de l'interface mapper.
     *
     * <p>Convention MapStruct : même package, nom de l'interface
     * + {@value #IMPL_SUFFIX}.
     * Ex : {@code com.company.UserMapper} → {@code com.company.UserMapperImpl}
     *
     * <p>Utilisé uniquement pour la Condition 1 ({@code extends MapstructMapper}).
     *
     * @param interfaceClass l'interface mapper
     * @param <M>            type de l'interface
     * @return               la classe d'implémentation générée
     * @throws IllegalStateException si l'implémentation n'est pas trouvée —
     *         vérifier que {@code @Mapper} est présent et que le processor
     *         MapStruct est configuré.
     */
    @SuppressWarnings("unchecked")
    private static <M> Class<? extends M> resolveImplClass(final Class<M> interfaceClass) {
        String implClassName = interfaceClass.getName() + IMPL_SUFFIX;
        try {
            Class<?> implClass = Thread.currentThread()
                                       .getContextClassLoader()
                                       .loadClass(implClassName);

            if (!interfaceClass.isAssignableFrom(implClass)) {
                throw new IllegalStateException(
                        "Class [" + implClassName + "] does not implement ["
                                + interfaceClass.getName() + "]");
            }

            return (Class<? extends M>) implClass;

        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "MapStruct implementation not found for ["
                            + interfaceClass.getName() + "]. "
                            + "Expected: [" + implClassName + "]. "
                            + "Ensure @Mapper is present and MapStruct "
                            + "annotation processor is configured.", e);
        }
    }

    // -------------------------------------------------------------------------
    // Résolution du constructeur et des dépendances
    // -------------------------------------------------------------------------

    /**
     * Sélectionne le constructeur le plus adapté de la classe d'implémentation.
     *
     * <p>Priorité au constructeur avec le plus de paramètres —
     * c'est celui généré par MapStruct pour {@code InjectionStrategy.CONSTRUCTOR}.
     * Fallback sur le constructeur sans argument si aucune dépendance.
     */
    private static Constructor<?> resolveConstructor(final Class<?> implClass) {
        return Arrays.stream(implClass.getDeclaredConstructors())
                     .max(Comparator.comparingInt(Constructor::getParameterCount))
                     .orElseThrow(() -> new IllegalStateException(
                             "No constructor found on [" + implClass.getName() + "]"));
    }

    /**
     * Résout les dépendances du constructeur via le {@link BeanFactory} Spring.
     *
     * <p>Appelé uniquement après initialisation complète du contexte —
     * tous les beans sont disponibles à ce stade.
     */
    private static Object[] resolveDependencies(final Constructor<?> constructor,
                                                 final BeanFactory beanFactory) {
        return Arrays.stream(constructor.getParameterTypes())
                     .map(beanFactory::getBean)
                     .toArray();
    }

    // -------------------------------------------------------------------------
    // Fallback proxy CGLIB
    // -------------------------------------------------------------------------

    /**
     * Construit un proxy CGLIB de secours via {@link MapstructMethodInterceptor}.
     *
     * <p>CGLIB gère aussi bien les interfaces que les classes concrètes via
     * {@code Enhancer.setSuperclass()} — pas de distinction nécessaire.
     *
     * <p>Ne devrait jamais être atteint dans un contexte Spring correctement
     * configuré avec {@code InjectionStrategy.CONSTRUCTOR}.
     * Le {@code log.warn} dans {@link #build} permet de l'identifier si c'est le cas.
     *
     * @param c interface ou classe concrète du mapper
     */
    public static <T> T buildProxy(final Class<T> c) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(c);
        enhancer.setCallback(INVOKE_SUPER_INTERCEPTOR);
        return (T) enhancer.create();
    }
}
