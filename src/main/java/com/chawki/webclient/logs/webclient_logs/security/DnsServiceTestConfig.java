import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.jcache.JCacheCacheManager;

import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@EnableCaching  // <-- ESSENTIEL pour activer le caching
public class DnsServiceTestConfig {

    @Bean
    public javax.cache.CacheManager jCacheManager() {
        CachingProvider provider = Caching.getCachingProvider();
        CacheManager cacheManager = provider.getCacheManager();
        
        // Configuration du cache dnsHostName
        MutableConfiguration<String, Optional> config = 
            new MutableConfiguration<String, Optional>()
                .setTypes(String.class, Optional.class)
                .setStoreByValue(false)  // IMPORTANT: évite la sérialisation
                .setManagementEnabled(true)
                .setStatisticsEnabled(true);
        
        // Créer le cache s'il n'existe pas
        try {
            cacheManager.createCache("dnsHostName", config);
        } catch (CacheException e) {
            // Cache déjà existant, ignorer
            System.out.println("Cache dnsHostName déjà créé");
        }
        
        return cacheManager;
    }

    @Bean
    public org.springframework.cache.CacheManager springCacheManager(
            javax.cache.CacheManager jCacheManager) {
        // Adapter JCache pour Spring Cache
        return new JCacheCacheManager(jCacheManager);
    }

    @Bean
    public MeterRegistry meterRegistry() {
        // MeterRegistry simple pour les tests (pas de métriques réelles)
        return new SimpleMeterRegistry();
    }

    @Bean
    public DnsProperties dnsProperties() {
        DnsProperties properties = new DnsProperties();
    
        // Si DnsProperties a des setters simples
        properties.setTimeout(1000); // 1 seconde
        
        // Si vous devez créer l'objet HostNameCache manuellement
        // et que vous ne connaissez pas sa structure exacte,
        // vous pouvez utiliser des valeurs par défaut
        
        return properties;
    }

    @Bean
    public DnsHostNameCacheManagerConfigurer cacheManagerConfigurer(
            org.springframework.cache.CacheManager springCacheManager,
            MeterRegistry meterRegistry,
            DnsProperties dnsProperties) {
        
        // Instancier avec les trois paramètres du constructeur
        return new DnsHostNameCacheManagerConfigurer(
            springCacheManager,  // peut être null si @Autowired(required = false)
            meterRegistry,       // peut être null si @Autowired(required = false)
            dnsProperties
        );
    }

    @Bean
    public DnsService dnsService(
            ScheduledExecutorService executor,
            DnsProperties dnsProperties) {
        
        DnsService service = new DnsService();
        // Injecter les dépendances via setters ou ReflectionTestUtils
        // ReflectionTestUtils.setField(service, "executor", executor);
        // ReflectionTestUtils.setField(service, "dnsTimeout", dnsProperties.getTimeout());
        
        return service;
    }

    @Bean
    public ScheduledExecutorService executor() {
        return Executors.newScheduledThreadPool(2);
    }
}
