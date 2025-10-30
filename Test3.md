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
