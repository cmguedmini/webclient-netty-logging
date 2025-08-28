// Option 1: Désactiver DenyAllSecurityConfiguration via @Profile
@Configuration
@Profile("!web") // Active uniquement si le profil 'web' n'est pas actif
public class DenyAllSecurityConfiguration {
    // Votre configuration existante
}

// Option 2: Utiliser @ConditionalOnWebApplication pour désactiver en mode batch
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class DenyAllSecurityConfiguration {
    // Cette configuration ne sera active que si c'est une application web
}

// Option 3: Configuration conditionnelle personnalisée
@Configuration
@ConditionalOnProperty(name = "app.security.web.enabled", havingValue = "true", matchIfMissing = false)
public class DenyAllSecurityConfiguration {
    // Se base sur une propriété de configuration
}

// Option 4: Remplacer par une configuration adaptée au mode batch
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class BatchSecurityConfiguration {
    
    @Bean
    @ConditionalOnMissingBean(AuthenticationManager.class)
    public AuthenticationManager authenticationManager() {
        // Votre AuthenticationManager basé sur tokens
        return new YourTokenBasedAuthenticationManager();
    }
    
    // Pas de SecurityFilterChain pour éviter les MvcRequestMatcher
    // Utilisez uniquement la sécurité au niveau méthode
}
