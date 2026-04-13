Pour tester le `MapstructHelper`, l'enjeu est de valider la logique d'instanciation dynamique sans avoir besoin de générer réellement des classes MapStruct durant le test. Nous allons utiliser des classes internes (statiques) pour simuler les implémentations et vérifier que le Helper choisit le bon constructeur et résout bien les dépendances.

Voici une classe de test complète utilisant **JUnit 5** et **Mockito**.

---

### Classe de Test : `MapstructHelperTest.java`

```java
package lu.x.starter.mapstruct;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ListableBeanFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MapstructHelperTest {

    @Mock
    private ListableBeanFactory beanFactory;

    private MapstructHelper helper;

    @BeforeEach
    void setUp() {
        // MapstructHelper est souvent une classe utilitaire ou un composant simple
        helper = new MapstructHelper();
    }

    @Test
    void shouldBuildMapperWithDependencies() {
        // Préparation des mocks pour les dépendances du constructeur
        String mockDependency = "ServiceDépendance";
        when(beanFactory.getBean(String.class)).thenReturn(mockDependency);

        // Exécution : On demande au helper de construire le mapper à partir de l'interface
        // Le helper doit trouver "FakeMapperImpl" automatiquement
        FakeMapper result = MapstructHelper.build(FakeMapper.class, beanFactory);

        // Vérifications
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(FakeMapperImpl.class);
        assertThat(((FakeMapperImpl) result).getDependency()).isEqualTo(mockDependency);
        
        // Vérifie que resolveDependencies a bien sollicité la beanFactory
        verify(beanFactory).getBean(String.class);
    }

    @Test
    void shouldFallbackToProxyWhenImplNotFound() {
        // Test de la branche resolveImplClass en cas d'erreur
        // On passe une interface qui n'a pas d'implémentation "MissingMapperImpl"
        MissingMapper proxy = MapstructHelper.build(MissingMapper.class, beanFactory);

        assertThat(proxy).isNotNull();
        // Vérifie que c'est un proxy (CGLIB ou JDK) et non l'implémentation réelle
        assertThat(proxy.getClass().getName()).contains("Proxy");
    }

    @Test
    void shouldSelectConstructorWithMostParameters() {
        // On utilise une classe avec plusieurs constructeurs pour valider resolveConstructor
        MultiConstructorMapper result = MapstructHelper.build(MultiConstructorMapper.class, beanFactory);
        
        assertThat(result).isNotNull();
        // On vérifie que c'est le constructeur à 2 arguments qui a été appelé (le "max")
        assertThat(((MultiConstructorMapperImpl) result).getUsedConstructor())
                .isEqualTo("ConstructorWithTwoParams");
    }

    // --- Classes de support pour les tests ---

    public interface FakeMapper extends MapstructMapper<Object, Object> {}
    
    // Simule l'implémentation générée par MapStruct
    public static class FakeMapperImpl implements FakeMapper {
        private final String dependency;
        public FakeMapperImpl(String dependency) { this.dependency = dependency; }
        public String getDependency() { return dependency; }
    }

    public interface MissingMapper extends MapstructMapper<Object, Object> {}

    public interface MultiConstructorMapper extends MapstructMapper<Object, Object> {}
    
    public static class MultiConstructorMapperImpl implements MultiConstructorMapper {
        private final String usedConstructor;
        
        public MultiConstructorMapperImpl() { 
            this.usedConstructor = "Default"; 
        }
        public MultiConstructorMapperImpl(Integer one, String two) { 
            this.usedConstructor = "ConstructorWithTwoParams"; 
        }
    }
}
```

---

### Points clés de la couverture Sonar dans ce test :

1.  **`resolveImplClass`** : 
    * Couvert par `shouldBuildMapperWithDependencies` (cas nominal).
    * Couvert par `shouldFallbackToProxyWhenImplNotFound` (cas d'erreur/exception), ce qui sécurise la branche `catch` et le déclenchement de `buildProxy`.

2.  **`resolveConstructor`** :
    * Couvert par `shouldSelectConstructorWithMostParameters`. L'utilisation de `max(Comparator.comparingInt(...))` est testée en s'assurant que le constructeur le plus long est choisi, même si un constructeur par défaut existe.

3.  **`resolveDependencies`** :
    * Le mock de `beanFactory.getBean(type)` simule l'injection réelle. Si le test passe, cela prouve que le Helper a correctement introspecté les types des paramètres du constructeur.

4.  **Suppression de `setAccessible`** :
    * En utilisant des classes `public static` dans le test, vous validez que le Helper n'a plus besoin de forcer les accès. Si vous passiez les constructeurs en `private`, le test échouerait, ce qui confirme la sécurité de votre implémentation finale.



### Conseil technique
Pour garantir que le `resolveImplClass` fonctionne dans le test, les classes `FakeMapperImpl` et `MultiConstructorMapperImpl` doivent respecter la convention de nommage (`InterfaceName + Impl`) et être accessibles dans le même package (ou via le classloader utilisé). L'utilisation de classes statiques internes dans le test est la méthode la plus fiable pour isoler ce test unitaire.
