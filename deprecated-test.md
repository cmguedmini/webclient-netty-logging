C'est une excellente approche si vous souhaitez éviter les Mocks **et** l'initialisation complète de Spring. Cependant, les classes **`ConfigurableEnvironment`** (souvent une instance de `StandardEnvironment`) et **`DeprecatedProperties`** (une classe `@ConfigurationProperties`) sont difficiles à instancier "manuellement" et à configurer pour un test unitaire isolé, car elles dépendent fortement du contexte Spring.

  * **`ConfigurableEnvironment`** : Ses constructeurs sont publics, mais il est complexe de simuler ses sources de propriétés sans les classes utilitaires de Spring (comme `MapPropertySource`).
  * **`DeprecatedProperties`** : C'est une classe de configuration. L'initialisation manuelle de ses champs (comme `customKeys` ou `failFast`) nécessite soit d'appeler les *setters* si vous les rendez publics, soit d'utiliser une bibliothèque de réflexion (ce qui est souvent plus complexe qu'un mock).

Puisque votre objectif est de tester la **logique métier du validateur** (`afterSingletonsInstantiated`) de la manière la plus simple et réaliste possible :

1.  Nous allons **instancier `ConfigurableEnvironment` et `DeprecatedProperties`** manuellement.
2.  Nous utiliserons les classes utilitaires de Spring pour configurer l'`Environment`.
3.  Nous utiliserons les *setters* (qui existent déjà dans votre code grâce à `@Setter`) pour configurer `DeprecatedProperties`.

Voici les tests unitaires sans utiliser de Mocks pour les dépendances :

## 🧪 Tests Unitaires `failFast` (Instanciation Manuelle)

```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class DeprecatedPropertiesValidatorManualTest {

    private StandardEnvironment environment;
    private DeprecatedProperties deprecatedProperties;
    private DeprecatedPropertiesValidator validator;

    private static final String DEPRECATED_KEY = "mon-app.propriete.obsolete";

    @BeforeEach
    void setUp() {
        // 1. Instanciation Manuelle de l'Environment (sans Mock)
        environment = new StandardEnvironment();

        // 2. Instanciation Manuelle de DeprecatedProperties (sans Mock)
        deprecatedProperties = new DeprecatedProperties();
        
        // La classe DeprecatedProperties a besoin d'avoir des listes initialisées
        // pour que la logique de Stream dans le validateur ne lance pas de NullPointerException
        
        // 3. Configuration des clés dépréciées (via les setters, car @Setter est utilisé)
        deprecatedProperties.setJefKeys(List.of("jef.key.*"));
        deprecatedProperties.setCustomKeys(List.of(DEPRECATED_KEY));
        deprecatedProperties.setGuideUrl("https://wiki-guide.com"); // Requis par @NotNull

        // 4. Configuration de l'Environment pour contenir la propriété dépréciée
        Map<String, Object> properties = Map.of(
            DEPRECATED_KEY, "valeur-a-supprimer", // Clé dépréciée détectée
            "une.autre.propriete", "valeur-ok"
        );
        MapPropertySource testPropertySource = new MapPropertySource("testSource", properties);
        environment.getPropertySources().addFirst(testPropertySource);
        
        // 5. Instanciation du Validateur avec les dépendances réelles
        validator = new DeprecatedPropertiesValidator(environment, deprecatedProperties);
    }

    // --- Méthode 1 : Test du comportement d'échec rapide (failFast = true) ---
    
    @Test
    void whenDeprecatedPropertyIsUsedAndFailFastIsTrue_thenThrowsIllegalArgumentException() {
        // GIVEN: failFast est true (valeur par défaut, mais on peut la définir explicitement)
        deprecatedProperties.setFailFast(true);

        // WHEN & THEN: L'appel à la méthode doit lancer une IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            validator.afterSingletonsInstantiated();
        }, "L'exception IllegalArgumentException aurait dû être levée car 'failFast' est à true.");
    }
    
    // --- Méthode 2 : Test du comportement d'enregistrement d'erreur (failFast = false) ---

    @Test
    void whenDeprecatedPropertyIsUsedAndFailFastIsFalse_thenNoExceptionIsThrown() {
        // GIVEN: Configuration failFast à false
        deprecatedProperties.setFailFast(false);

        // WHEN & THEN: L'appel à la méthode ne doit lever aucune exception
        // Le log.error est déclenché, mais l'exception est évitée.
        assertDoesNotThrow(() -> {
            validator.afterSingletonsInstantiated();
        }, "Le validateur ne devrait pas lancer d'exception lorsque 'failFast' est à false.");
    }
}
```

### Explication

1.  **Initialisation Manuelle** : Nous instancions directement `StandardEnvironment` (une implémentation de `ConfigurableEnvironment`) et `DeprecatedProperties`.
2.  **Configuration des Dépendances** :
      * Nous utilisons les **setters** de `DeprecatedProperties` pour simuler le chargement des valeurs de configuration (notamment `setCustomKeys()` qui contient notre clé dépréciée de test, et `setFailFast()` pour contrôler le scénario).
      * Nous utilisons la classe utilitaire **`MapPropertySource`** de Spring pour insérer manuellement la clé dépréciée (`mon-app.propriete.obsolete`) dans l'`environment`. Cela simule la lecture de la clé depuis un fichier `application.yml`.
3.  **Tests** :
      * **Test 1** : `setFailFast(true)` est appliqué, et **`assertThrows`** confirme que l'`IllegalArgumentException` est bien lancée.
      * **Test 2** : `setFailFast(false)` est appliqué, et **`assertDoesNotThrow`** confirme que la méthode s'exécute jusqu'à la fin sans lever d'exception.
