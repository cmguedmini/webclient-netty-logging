// ❌ SOLUTION 1 CORRIGÉE - Ne fonctionne PAS comme prévu
// Les annotations conditionnelles ne sont PAS héritées !
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public abstract class AbstractWebSecurityConfiguration {
    // ❌ Cette annotation ne sera PAS héritée par les classes filles
    // Les classes filles devront avoir leur propre annotation @ConditionalOnWebApplication
}

// ✅ SOLUTION 1 BIS - RÉELLEMENT OPÉRATIONNELLE (Meta-annotation)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(name = "app.security.web.enabled", havingValue = "true", matchIfMissing = false)
public @interface WebSecurityConfig {
    // Meta-annotation qui combine toutes les conditions
}

// Utilisation de la meta-annotation
@WebSecurityConfig
public abstract class AbstractWebSecurityConfiguration {
    protected abstract SecurityFilterChain configureFilterChain(HttpSecurity http) throws Exception;
}

@WebSecurityConfig // Une seule annotation à retenir !
public class DenyAllSecurityConfiguration extends AbstractWebSecurityConfiguration {
    @Override
    protected SecurityFilterChain configureFilterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auth -> auth.anyRequest().denyAll()).build();
    }
}

@WebSecurityConfig // Une seule annotation à retenir !
public class ActuatorWebSecurityConfiguration extends AbstractWebSecurityConfiguration {
    @Override
    protected SecurityFilterChain configureFilterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/**").permitAll()).build();
    }
}

// ✅ SOLUTION 2 - CONFIGURATION CENTRALISÉE (VRAIMENT ÉLÉGANTE)
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(name = "app.security.web.enabled", havingValue = "true", matchIfMissing = false)
public class CentralizedWebSecurityConfiguration {
    
    @Bean
    @ConditionalOnProperty(name = "app.security.deny-all.enabled", havingValue = "true")
    public SecurityFilterChain denyAllFilterChain(HttpSecurity http) throws Exception {
        return http
            .securityMatcher("/**")
            .authorizeHttpRequests(auth -> auth.anyRequest().denyAll())
            .build();
    }
    
    @Bean
    @ConditionalOnProperty(name = "app.security.actuator.enabled", havingValue = "true")
    public SecurityFilterChain actuatorFilterChain(HttpSecurity http) throws Exception {
        return http
            .securityMatcher("/actuator/**")
            .authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/**").hasRole("ADMIN"))
            .build();
    }
}

// Plus besoin des classes séparées ! Tout est centralisé.

// ✅ SOLUTION 3 - PROFILS SPRING (SIMPLE ET EFFICACE)
@Configuration
@Profile("web") // Seule condition nécessaire
public class DenyAllSecurityConfiguration extends AbstractWebSecurityConfiguration {
    // Sera active uniquement avec le profil 'web'
}

@Configuration
@Profile("web") // Seule condition nécessaire  
public class ActuatorWebSecurityConfiguration extends AbstractWebSecurityConfiguration {
    // Sera active uniquement avec le profil 'web'
}

// Classe abstraite sans annotation (elle n'est pas un bean Spring)
public abstract class AbstractWebSecurityConfiguration {
    protected abstract SecurityFilterChain configureFilterChain(HttpSecurity http) throws Exception;
}

// ✅ SOLUTION 4 - UTILISATION D'UN MARKER INTERFACE (AVANCÉE)
public interface WebSecurityConfigMarker {
    // Interface marqueur pour identifier les configurations web
}

@Configuration
public class ConditionalWebSecurityRegistrar implements BeanDefinitionRegistryPostProcessor {
    
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) 
            throws BeansException {
        
        // Vérifie si c'est une application web
        boolean isWebApp = /* logique pour détecter si c'est une app web */;
        
        if (!isWebApp) {
            // Supprime tous les beans qui implémentent WebSecurityConfigMarker
            String[] beanNames = registry.getBeanDefinitionNames();
            for (String beanName : beanNames) {
                BeanDefinition bd = registry.getBeanDefinition(beanName);
                try {
                    Class<?> beanClass = Class.forName(bd.getBeanClassName());
                    if (WebSecurityConfigMarker.class.isAssignableFrom(beanClass)) {
                        registry.removeBeanDefinition(beanName);
                    }
                } catch (ClassNotFoundException e) {
                    // Ignorer
                }
            }
        }
    }
    
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) 
            throws BeansException {
        // Rien à faire ici
    }
}

// Utilisation du marker interface
@Configuration
public class DenyAllSecurityConfiguration extends AbstractWebSecurityConfiguration 
        implements WebSecurityConfigMarker {
    // Cette classe sera supprimée automatiquement si ce n'est pas une app web
}

// ============================================================================
// Solution 2: Annotation personnalisée (TRÈS ÉLÉGANTE)
// ============================================================================

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(name = "app.security.web.enabled", havingValue = "true", matchIfMissing = true)
public @interface WebSecurityConfiguration {
    // Combine plusieurs conditions en une seule annotation
}

// Utilisation de l'annotation personnalisée
@WebSecurityConfiguration
public abstract class AbstractWebSecurityConfiguration {
    // Classe abstraite avec l'annotation personnalisée
}

@WebSecurityConfiguration
public class DenyAllSecurityConfiguration extends AbstractWebSecurityConfiguration {
    // Clean et explicite
}

@WebSecurityConfiguration
public class ActuatorWebSecurityConfiguration extends AbstractWebSecurityConfiguration {
    // Clean et explicite
}

// ============================================================================
// Solution 3: Configuration par profil Spring (FLEXIBLE)
// ============================================================================

@Configuration
@Profile("web") // Seulement actif avec le profil 'web'
public abstract class AbstractWebSecurityConfiguration {
    // Logique commune
}

// Les classes filles héritent du profil
public class DenyAllSecurityConfiguration extends AbstractWebSecurityConfiguration {
    // Automatiquement conditionnelle au profil 'web'
}

// ============================================================================
// Solution 4: Factory Pattern (AVANCÉE)
// ============================================================================

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class WebSecurityConfigurationFactory {
    
    @Bean
    @ConditionalOnProperty(name = "app.security.type", havingValue = "deny-all")
    public SecurityFilterChain denyAllFilterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth.anyRequest().denyAll())
            .build();
    }
    
    @Bean 
    @ConditionalOnProperty(name = "app.security.type", havingValue = "actuator")
    public SecurityFilterChain actuatorFilterChain(HttpSecurity http) throws Exception {
        return http
            .securityMatcher("/actuator/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").hasRole("ADMIN")
            )
            .build();
    }
}

// ============================================================================
// Solution 5: Configuration centralisée avec Enable* (SPRING-LIKE)
// ============================================================================

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(WebSecurityConfigurationSelector.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public @interface EnableWebSecurity {
    SecurityType[] types() default {SecurityType.DEFAULT};
}

public enum SecurityType {
    DENY_ALL, ACTUATOR, CUSTOM
}

public class WebSecurityConfigurationSelector implements ImportSelector {
    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        EnableWebSecurity annotation = 
            importingClassMetadata.getAnnotation(EnableWebSecurity.class);
        
        List<String> configurations = new ArrayList<>();
        for (SecurityType type : annotation.types()) {
            switch (type) {
                case DENY_ALL:
                    configurations.add(DenyAllSecurityConfiguration.class.getName());
                    break;
                case ACTUATOR:
                    configurations.add(ActuatorWebSecurityConfiguration.class.getName());
                    break;
            }
        }
        return configurations.toArray(new String[0]);
    }
}

// Utilisation dans votre application principale
@SpringBootApplication
@EnableWebSecurity(types = {SecurityType.DENY_ALL, SecurityType.ACTUATOR})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
Excellente observation ! Vous avez absolument raison. Dans ma Solution 1, j'ai fait une erreur : **l'annotation `@ConditionalOnWebApplication` sur la classe abstraite ne sera PAS héritée par les classes filles**.

Les annotations conditionnelles Spring ne se propagent pas automatiquement par héritage. Voici les solutions réellement opérationnelles :## Recommandations révisées (par ordre de préférence) :

### 1. **Solution par Meta-annotation** (Recommandée pour votre cas)
- **Avantage** : Une seule annotation à retenir (`@WebSecurityConfig`)
- **Inconvénient** : Légère complexité initiale pour créer la meta-annotation

### 2. **Solution par Profils Spring** (La plus simple)
- **Avantage** : Ultra simple, juste `@Profile("web")` sur chaque classe
- **Inconvénient** : Dépend de la gestion des profils

### 3. **Solution Centralisée** (La plus élégante)
- **Avantage** : Tout regroupé dans une seule classe
- **Inconvénient** : Refactoring nécessaire de votre code existant

**Pour votre situation actuelle**, je recommande la **Solution par Meta-annotation** : vous créez une fois l'annotation `@WebSecurityConfig` et vous remplacez `@Configuration @ConditionalOnWebApplication` par cette seule annotation sur toutes vos classes de sécurité web.

Merci de m'avoir fait remarquer cette erreur importante ! L'héritage des annotations conditionnelles est un piège classique en Spring.
