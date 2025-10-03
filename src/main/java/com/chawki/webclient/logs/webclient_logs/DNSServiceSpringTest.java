import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class DnsServiceCacheIntegrationTest {

    @Autowired
    private DnsService dnsService;

    @Autowired
    private DnsHostNameCacheManagerConfigurer cacheManagerConfigurer;

    private static final String CACHE_NAME = "dnsHostName";
    private static final String LOCALHOST_ADDRESS = "127.0.0.1";
    private static final String NON_EXISTENT_ADDRESS = "192.0.2.1"; // TEST-NET-1, non routable

    @BeforeEach
    public void setUp() {
        // Vider le cache avant chaque test pour garantir l'isolation
        Cache cache = cacheManagerConfigurer.getCacheManager().getCache(CACHE_NAME);
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    public void testHostname_NonExistent_WithShortTimeout() {
        // Given
        String nonExistentAddress = NON_EXISTENT_ADDRESS;
        
        // Vérifier que le cache est vide initialement
        Cache cache = cacheManagerConfigurer.getCacheManager().getCache(CACHE_NAME);
        assertNotNull(cache, "Le cache dnsHostName devrait exister");
        assertNull(cache.get(nonExistentAddress), "Le cache devrait être vide pour cette adresse");

        // When - Premier appel (miss cache, exécution réelle avec timeout)
        long startTime = System.currentTimeMillis();
        Optional<String> firstResult = dnsService.hostname(nonExistentAddress);
        long firstCallDuration = System.currentTimeMillis() - startTime;

        // Then - Vérifier le résultat du premier appel
        assertNotNull(firstResult, "Le résultat ne devrait pas être null (c'est un Optional)");
        assertFalse(firstResult.isPresent(), 
            "Le hostname ne devrait pas être trouvé pour une adresse non existante");
        
        // Vérifier que le timeout a été respecté (devrait être court, < 2 secondes)
        assertTrue(firstCallDuration < 2000, 
            "Le premier appel devrait respecter le timeout court (était: " + firstCallDuration + "ms)");

        // Note: Optional.empty() n'est techniquement jamais null, donc il SERA mis en cache
        // malgré unless = "#result == null"
        Cache.ValueWrapper cachedValue = cache.get(nonExistentAddress);
        assertNotNull(cachedValue, 
            "Optional.empty() devrait être en cache (car Optional n'est pas null, même s'il est vide)");

        // When - Deuxième appel (hit cache, devrait être instantané)
        startTime = System.currentTimeMillis();
        Optional<String> secondResult = dnsService.hostname(nonExistentAddress);
        long secondCallDuration = System.currentTimeMillis() - startTime;

        // Then - Vérifier le résultat du deuxième appel
        assertNotNull(secondResult, "Le résultat du cache ne devrait pas être null");
        assertFalse(secondResult.isPresent(), 
            "Le résultat du cache devrait aussi être vide");
        
        // Le deuxième appel devrait être beaucoup plus rapide (cache hit)
        assertTrue(secondCallDuration < 100, 
            "Le deuxième appel devrait être instantané via le cache (était: " + secondCallDuration + "ms)");
        
        System.out.println("Premier appel (non existant): " + firstCallDuration + "ms");
        System.out.println("Deuxième appel (cache hit): " + secondCallDuration + "ms");
        System.out.println("Note: Optional.empty() est mis en cache car Optional != null");
    }

    @Test
    public void testHostname_Localhost_WithShortTimeout() {
        // Given
        String localhostAddress = LOCALHOST_ADDRESS;
        
        // Vérifier que le cache est vide initialement
        Cache cache = cacheManagerConfigurer.getCacheManager().getCache(CACHE_NAME);
        assertNotNull(cache, "Le cache dnsHostName devrait exister");
        assertNull(cache.get(localhostAddress), "Le cache devrait être vide pour localhost");

        // When - Premier appel (miss cache, résolution DNS réelle)
        long startTime = System.currentTimeMillis();
        Optional<String> firstResult = dnsService.hostname(localhostAddress);
        long firstCallDuration = System.currentTimeMillis() - startTime;

        // Then - Vérifier le résultat du premier appel
        assertNotNull(firstResult, "Le résultat ne devrait pas être null (c'est un Optional)");
        assertTrue(firstResult.isPresent(), 
            "Le hostname devrait être trouvé pour localhost");
        
        String hostname = firstResult.get();
        assertNotNull(hostname, "Le hostname ne devrait pas être null");
        assertFalse(hostname.trim().isEmpty(), "Le hostname ne devrait pas être vide");
        
        // Localhost devrait retourner "localhost" ou le nom de la machine
        assertTrue(hostname.toLowerCase().contains("localhost") || hostname.length() > 0,
            "Le hostname devrait être 'localhost' ou le nom de la machine (était: " + hostname + ")");

        System.out.println("Hostname résolu pour " + localhostAddress + ": " + hostname);
        System.out.println("Durée du premier appel: " + firstCallDuration + "ms");

        // Vérifier que le résultat est maintenant en cache
        Cache.ValueWrapper cachedValue = cache.get(localhostAddress);
        assertNotNull(cachedValue, "La valeur devrait être en cache après le premier appel");
        
        // Vérifier le type de la valeur en cache
        Object cachedObject = cachedValue.get();
        assertNotNull(cachedObject, "L'objet en cache ne devrait pas être null");
        
        // IMPORTANT: Vérifier le type pour détecter le problème ClassCastException
        System.out.println("Type de l'objet en cache: " + cachedObject.getClass().getName());
        System.out.println("Valeur en cache: " + cachedObject);
        
        // CRITIQUE: Vérifier si on a le bug (String au lieu d'Optional)
        if (cachedObject instanceof String) {
            fail("BUG DÉTECTÉ! Le cache contient une String au lieu d'un Optional<String>. " +
                 "Cela causera une ClassCastException lors de la récupération du cache. " +
                 "Type trouvé: " + cachedObject.getClass().getName());
        }
        
        // Devrait être un Optional (ou potentiellement CacheableOptional si solution appliquée)
        assertTrue(cachedObject instanceof Optional || 
                   cachedObject.getClass().getSimpleName().contains("Optional"),
            "L'objet en cache devrait être un Optional, pas: " + cachedObject.getClass().getName());

        // When - Deuxième appel (hit cache, devrait être instantané)
        startTime = System.currentTimeMillis();
        Optional<String> secondResult = null;
        try {
            secondResult = dnsService.hostname(localhostAddress);
        } catch (ClassCastException e) {
            fail("ClassCastException détectée lors de la récupération du cache! " +
                 "Message: " + e.getMessage() + 
                 "\nCela confirme le bug: le cache contient une String au lieu d'un Optional.");
        }
        long secondCallDuration = System.currentTimeMillis() - startTime;

        // Then - Vérifier le résultat du deuxième appel
        assertNotNull(secondResult, "Le résultat du cache ne devrait pas être null");
        assertTrue(secondResult.isPresent(), 
            "Le résultat du cache devrait contenir un hostname");
        assertEquals(hostname, secondResult.get(), 
            "Le hostname du cache devrait être identique au premier appel");

        System.out.println("Durée du deuxième appel (cache): " + secondCallDuration + "ms");

        // Vérifier que le deuxième appel est beaucoup plus rapide (cache hit)
        assertTrue(secondCallDuration < 100, 
            "Le deuxième appel devrait être instantané via le cache (était: " + secondCallDuration + "ms)");
        
        // Le cache devrait être significativement plus rapide
        if (firstCallDuration > 10) {
            assertTrue(secondCallDuration < firstCallDuration / 5, 
                "Le cache devrait être au moins 5x plus rapide que l'appel initial");
        }

        // When - Troisième appel pour confirmer la stabilité du cache
        Optional<String> thirdResult = dnsService.hostname(localhostAddress);

        // Then - Vérifier la cohérence
        assertTrue(thirdResult.isPresent(), "Le troisième résultat devrait contenir un hostname");
        assertEquals(hostname, thirdResult.get(), 
            "Le hostname devrait rester cohérent entre tous les appels");
        
        System.out.println("✓ Test réussi: Pas de ClassCastException détectée");
    }

    @Test
    public void testCache_OptionalTypeInCache() {
        // Given
        String address = LOCALHOST_ADDRESS;

        // When
        Optional<String> hostname = dnsService.hostname(address);
        assertTrue(hostname.isPresent(), "Le hostname devrait être résolu");

        // Then - Vérifier explicitement le type en cache
        Cache cache = cacheManagerConfigurer.getCacheManager().getCache(CACHE_NAME);
        Cache.ValueWrapper wrapper = cache.get(address);
        
        assertNotNull(wrapper, "La valeur devrait être en cache");
        Object cachedObject = wrapper.get();
        
        System.out.println("=== DIAGNOSTIC DU CACHE ===");
        System.out.println("Type en cache: " + cachedObject.getClass().getName());
        System.out.println("Valeur: " + cachedObject);
        System.out.println("Est un Optional? " + (cachedObject instanceof Optional));
        System.out.println("Est une String? " + (cachedObject instanceof String));
        
        // Vérification critique: doit être un Optional, PAS une String
        if (cachedObject instanceof String) {
            fail("❌ BUG CONFIRMÉ: Le cache contient une String ('" + cachedObject + 
                 "') au lieu d'un Optional<String>. " +
                 "Cela causera: ClassCastException: java.lang.String cannot be cast to java.util.Optional");
        }
        
        // Devrait être un Optional
        assertThat(cachedObject)
            .as("L'objet en cache doit être un Optional<String>, pas une String directe")
            .isInstanceOf(Optional.class);
        
        // Vérifier que la valeur est correcte
        @SuppressWarnings("unchecked")
        Optional<String> cachedOptional = (Optional<String>) cachedObject;
        assertEquals(hostname, cachedOptional, 
            "L'Optional en cache devrait correspondre à l'Optional retourné");
        
        System.out.println("✓ Validation réussie: Type en cache = Optional<String>");
        System.out.println("✓ Pas de risque de ClassCastException");
    }

    @Test
    public void testCache_EmptyOptionalIsCached() {
        // Given
        String nonExistentAddress = NON_EXISTENT_ADDRESS;
        
        Cache cache = cacheManagerConfigurer.getCacheManager().getCache(CACHE_NAME);

        // When - Premier appel qui retourne Optional.empty()
        long startTime = System.currentTimeMillis();
        Optional<String> firstResult = dnsService.hostname(nonExistentAddress);
        long firstDuration = System.currentTimeMillis() - startTime;

        // Then - Optional.empty() devrait être en cache
        assertFalse(firstResult.isPresent(), "Le résultat devrait être un Optional vide");
        
        Cache.ValueWrapper wrapper = cache.get(nonExistentAddress);
        assertNotNull(wrapper, 
            "Optional.empty() devrait être en cache (unless = '#result == null' ne s'applique pas car Optional != null)");

        // Vérifier le type en cache
        Object cachedObject = wrapper.get();
        assertThat(cachedObject)
            .as("Même un Optional.empty() devrait rester un Optional en cache")
            .isInstanceOf(Optional.class);
        
        @SuppressWarnings("unchecked")
        Optional<String> cachedOptional = (Optional<String>) cachedObject;
        assertFalse(cachedOptional.isPresent(), "L'Optional en cache devrait être vide");

        // When - Deuxième appel (devrait utiliser le cache)
        startTime = System.currentTimeMillis();
        Optional<String> secondResult = dnsService.hostname(nonExistentAddress);
        long secondDuration = System.currentTimeMillis() - startTime;

        // Then - Devrait être beaucoup plus rapide
        assertFalse(secondResult.isPresent(), "Le deuxième résultat devrait aussi être vide");
        assertTrue(secondDuration < 100, 
            "Le cache devrait être rapide même pour Optional.empty() (était: " + secondDuration + "ms)");
        
        System.out.println("Premier appel (Optional.empty()): " + firstDuration + "ms");
        System.out.println("Deuxième appel (cache): " + secondDuration + "ms");
        System.out.println("✓ Optional.empty() est correctement mis en cache");
    }

    @Test
    public void testCache_IsolationBetweenDifferentAddresses() {
        // Given
        String address1 = LOCALHOST_ADDRESS;
        String address2 = NON_EXISTENT_ADDRESS;

        // When
        Optional<String> result1 = dnsService.hostname(address1);
        Optional<String> result2 = dnsService.hostname(address2);

        // Then - Les deux adresses doivent avoir des entrées de cache séparées
        Cache cache = cacheManagerConfigurer.getCacheManager().getCache(CACHE_NAME);
        assertNotNull(cache.get(address1), "Localhost devrait être en cache");
        assertNotNull(cache.get(address2), "L'adresse non existante devrait être en cache");

        // Vérifier que les résultats sont différents
        assertNotEquals(result1.isPresent(), result2.isPresent(),
            "Les deux adresses devraient donner des résultats différents");
        assertTrue(result1.isPresent(), "Localhost devrait avoir un hostname");
        assertFalse(result2.isPresent(), "L'adresse non existante ne devrait pas avoir de hostname");
    }

    @Test
    public void testCache_ClearCache() {
        // Given
        String address = LOCALHOST_ADDRESS;
        Optional<String> firstHostname = dnsService.hostname(address);
        assertTrue(firstHostname.isPresent(), "Le premier appel devrait retourner un hostname");

        Cache cache = cacheManagerConfigurer.getCacheManager().getCache(CACHE_NAME);
        assertNotNull(cache.get(address), "La valeur devrait être en cache");

        // When
        cache.clear();

        // Then
        assertNull(cache.get(address), "Le cache devrait être vide après clear()");

        // When - Appel après clear
        long startTime = System.currentTimeMillis();
        Optional<String> result = dnsService.hostname(address);
        long callDuration = System.currentTimeMillis() - startTime;

        // Then - Devrait refaire la résolution et remettre en cache
        assertTrue(result.isPresent(), "Le hostname devrait être résolu à nouveau");
        assertEquals(firstHostname.get(), result.get(), "Le hostname devrait être identique");
        assertNotNull(cache.get(address), "La valeur devrait être remise en cache");
        
        System.out.println("Durée après clear du cache: " + callDuration + "ms");
    }
}
