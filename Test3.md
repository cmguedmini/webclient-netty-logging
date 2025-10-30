@ExtendWith(MockitoExtension.class)
public class TypeValidatorTest {

    private final TypeValidator validator = new TypeValidator();
    private final Type mockArrayType = mock(Type.class); // Simule le type de tableau (e.g., String[])
    private final Type mockItemType = mock(Type.class);  // Simule le type de composant (e.g., String)
    private final String validationMode = "LENIENT";

    /**
     * Teste que isPewDto est appelé sur le type de composant lorsque le type est un tableau.
     */
    @Test
    void shouldCallIsPewDtoOnComponentTypeWhenTypeIsArray() {
        // ARRANGE (Préparation des Mocks Statiques)
        try (
            MockedStatic<TypeHelper> mockedTypeHelper = mockStatic(TypeHelper.class);
            MockedStatic<JefAssertionHelper> mockedAssertionHelper = mockStatic(JefAssertionHelper.class)
        ) {
            // 1. Simuler l'entrée dans le bloc 'if': TypeHelper.isArray(actualType) -> true
            mockedTypeHelper.when(() -> TypeHelper.isArray(mockArrayType))
                            .thenReturn(true);

            // 2. Simuler la récupération du type de composant
            mockedTypeHelper.when(() -> TypeHelper.getArrayComponentType(mockArrayType))
                            .thenReturn(mockItemType);

            // 3. Simuler la chaîne d'assertion (le Helper d'Assertion d'instance)
            JefAssertionHelper mockAssertion = mock(JefAssertionHelper.class);

            // Simuler l'appel statique à assertThatClass, qui retourne le mock d'instance
            mockedAssertionHelper.when(() -> JefAssertionHelper.assertThatClass(mockItemType))
                                 .thenReturn(mockAssertion);

            // ACT (Exécution)
            validator.validateCollectionItemType(mockArrayType, validationMode);

            // ASSERT (Vérification)
            
            // 1. Vérifier que TypeHelper.getArrayComponentType a été appelé
            mockedTypeHelper.verify(() -> TypeHelper.getArrayComponentType(mockArrayType), times(1));

            // 2. Vérifier que la méthode finale isPewDto a été appelée
            verify(mockAssertion, times(1)).isPewDto(validationMode);

            // 3. (Optionnel) Vérifier qu'il n'y a pas d'autres appels sur l'assertion
            verifyNoMoreInteractions(mockAssertion);

        } // Les mocks statiques sont automatiquement fermés ici
    }
}

Explication du Test

    Mocks Statiques (MockedStatic) : Indispensable pour contrôler les méthodes statiques de TypeHelper et JefAssertionHelper.

    Simulation du Chemin Vrai :

        mockedTypeHelper.when(() -> TypeHelper.isArray(mockArrayType)).thenReturn(true); permet au test d'entrer dans le bloc if.

        mockedTypeHelper.when(() -> TypeHelper.getArrayComponentType(mockArrayType)).thenReturn(mockItemType); simule la récupération du type d'élément, qui est crucial pour l'étape suivante.

    Vérification de la Chaîne d'Assertion :

        Nous créons un mock d'instance (mockAssertion).

        Nous faisons en sorte que l'appel statique JefAssertionHelper.assertThatClass(mockItemType) retourne ce mock d'instance.

        La vérification finale verify(mockAssertion, times(1)).isPewDto(validationMode); confirme que la méthode isPewDto a été appelée exactement une fois sur l'objet retourné, avec le mode de validation correct.

Ceci est un test simple, puissant, et isolé qui couvre exactement la logique du bloc de code fourni.

Voulez-vous que j'ajoute un test pour le cas négatif, où le type n'est pas un tableau ?
----------------
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Classe Simuléé contenant le constructeur et la méthode equals à tester.
 * Les champs 'name' et 'value' sont ajoutés pour la comparaison par réflexion.
 */
class PojoSetterAndGetterTester {
    private String name;
    private int value;

    // Champs pour le constructeur, même s'ils ne sont pas utilisés dans equals,
    // ils doivent être initialisés pour la compilation de la classe réelle.
    private final boolean checkGet;
    private final boolean checkSet;
    
    // Le constructeur fourni par l'utilisateur
    public <T> PojoSetterAndGetterTester(final boolean checkGet, final boolean checkSet) {
        this.checkGet = checkGet;
        this.checkSet = checkSet;
        // Initialisation des champs de données pour le test d'égalité
        this.name = null;
        this.value = 0;
    }
    
    // Méthode pour configurer les champs de données de l'instance pour les besoins du test
    public void configureData(String name, int value) {
        this.name = name;
        this.value = value;
    }

    // Le bloc de code à tester
    @Override
    public boolean equals(final Object otherObject) {
       return EqualsBuilder.reflectionEquals(this, otherObject);
    }
}

public class PojoSetterAndGetterTesterTest {

    private final boolean DONT_CHECK = false;
    private final boolean CHECK = true;

    /**
     * Teste la méthode equals pour les cas de réflexivité, d'égalité et de non-égalité.
     */
    @Test
    void testEqualsCoverageWithNewConstructor() {
        // ARRANGE
        
        // Création des instances en utilisant le constructeur fourni
        PojoSetterAndGetterTester instanceA = new PojoSetterAndGetterTester(CHECK, DONT_CHECK);
        PojoSetterAndGetterTester instanceB = new PojoSetterAndGetterTester(CHECK, DONT_CHECK);
        PojoSetterAndGetterTester instanceC = new PojoSetterAndGetterTester(DONT_CHECK, CHECK);

        // Configuration des champs pour la comparaison par EqualsBuilder
        instanceA.configureData("Test", 10);
        instanceB.configureData("Test", 10); // Instance B est un duplicata de A
        instanceC.configureData("Different", 20); // Instance C est différente

        // ACT & ASSERT

        // 1. Réflexivité : Un objet doit être égal à lui-même.
        assertTrue(instanceA.equals(instanceA), "La réflexivité doit être vraie.");

        // 2. Égalité : Deux objets avec les mêmes valeurs (y compris les booléens du constructeur) doivent être égaux.
        assertTrue(instanceA.equals(instanceB), "L'égalité avec un objet identique doit être vraie.");
        
        // 3. Non-égalité : Deux objets avec des valeurs de données différentes ne doivent pas être égaux.
        assertFalse(instanceA.equals(instanceC), "L'égalité avec un objet différent doit être fausse (différence sur les données ou les booléens).");
        
        // 4. Comparaison avec null.
        assertFalse(instanceA.equals(null), "La comparaison avec null doit être fausse.");
    }
}
