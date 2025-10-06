import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.cache.Caching;
import javax.cache.spi.CachingProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
public class DnsHostNameCacheIntegrationTest {

    @Autowired
    private MyCacheableService service;

    @Autowired
    private MyDependency dependencyMock;

    @Test
    void cacheableMethod_shouldUseJCache_andBeCalledOnlyOnce() {
        String key = "localhost";
        String expectedValue = "127.0.0.1";

        // 1. Définir le comportement du mock : il est appelé la première fois
        when(dependencyMock.fetch(key)).thenReturn(expectedValue);

        // 2. Premier appel : exécute la méthode et met en cache
        String result1 = service.resolveHost(key);
        assertEquals(expectedValue, result1);

        // 3. Deuxième appel : utilise le cache JSR-107
        String result2 = service.resolveHost(key);
        assertEquals(expectedValue, result2);

        // 4. Vérification : La méthode de la dépendance n'a été appelée qu'une seule fois.
        verify(dependencyMock, times(1)).fetch(key);
    }

    // -------------------------------------------------------------------------
    // Configuration Spring : JSR-107 Cache (javax.cache)
    // -------------------------------------------------------------------------

    @Configuration
    @EnableCaching // Active le mécanisme de proxy de cache de Spring
    static class TestConfig {

        // --- 1. Infrastructure JSR-107 (javax.cache) ---
        
        @Bean // Fournit le CacheManager JSR-107 (la dépendance exacte de votre classe)
        public javax.cache.CacheManager jCacheManager() {
            // Utilise l'API de base JSR-107 pour obtenir un fournisseur de cache
            CachingProvider cachingProvider = Caching.getCachingProvider();
            
            // Crée le gestionnaire de cache JSR-107 (il dépend d'une implémentation sur le classpath, 
            // ex: Ehcache, Hazelcast)
            javax.cache.CacheManager manager = cachingProvider.getCacheManager();

            // Crée le cache "dnsCache" requis par @Cacheable("dnsCache")
            if (manager.getCache("dnsCache") == null) {
                 manager.createCache("dnsCache", new javax.cache.config.MutableConfiguration<>());
            }
            return manager;
        }

        @Bean // Fournit le CacheManager de Spring qui enveloppe le JSR-107
        public CacheManager cacheManager(javax.cache.CacheManager jCacheManager) {
            // JCacheCacheManager est le pont entre Spring Cache Abstraction et javax.cache
            return new JCacheCacheManager(jCacheManager);
        }

        // --- 2. Configuration de vos classes ---

        @Bean // Mock pour la dépendance de service
        public MyDependency myDependencyMock() {
            return mock(MyDependency.class);
        }

        @Bean // Le service contenant @Cacheable
        public MyCacheableService myCacheableService(MyDependency dependencyMock) {
            return new MyCacheableService(dependencyMock);
        }

        @Bean // Votre DnsHostNameCacheManagerConfigurer
        public DnsHostNameCacheManagerConfigurer dnsHostNameCacheManagerConfigurer(
                javax.cache.CacheManager jCacheManager, // Reçu de jCacheManager()
                MeterRegistry meterRegistry, 
                DnsProperties properties) {
            
            // Simule MeterRegistry
            MeterRegistry mockMeterRegistry = mock(MeterRegistry.class);
            
            // Instanciation de votre classe avec les dépendances résolues
            return new DnsHostNameCacheManagerConfigurer(
                    jCacheManager, 
                    mockMeterRegistry, 
                    properties
            );
        }
        
        @Bean // Les propriétés requises
        public DnsProperties dnsProperties() {
            return new DnsProperties();
        }
        
        @Bean // Mock pour MeterRegistry
        public MeterRegistry meterRegistry() {
            return mock(MeterRegistry.class);
        }
    }
}
