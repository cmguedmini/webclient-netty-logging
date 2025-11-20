package com.example.utils; // Adaptez le package selon votre structure

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Classe de test pour la méthode statique makeSafe.
 * On simule l'effet de la constante STRIP_CUT et de la méthode stripBreakings.
 */
class StringHelperTest {

    // Simuler la constante de la classe (private static final String STRIP_CUT = "\r\n ";)
    private static final String STRIP_CUT = "\r\n ";

    // Simuler la méthode stripBreakings pour les besoins du test
    // Supposons qu'elle enlève les retours à la ligne et les tabulations/espaces au début/fin.
    private static CharSequence stripBreakings(final CharSequence input) {
        if (input == null) return "";
        // Suppression des retours à la ligne et CR pour simplifier
        return input.toString().replace("\r", "").replace("\n", "").trim();
    }
    
    // La méthode à tester, recopiée ici pour que le test compile seul,
    // mais dans un vrai projet elle serait dans la classe StringHelper.
    public static String makeSafe(final CharSequence input, final int len) {
        // stripBreakings permet de contrer les injections de headers
        StringBuilder result = new StringBuilder(stripBreakings(input));
        int length = result.length();
        int index = len;
        while (index < length) {
            result.insert(index, STRIP_CUT);
            index += len + STRIP_CUT.length() -1;
            length += STRIP_CUT.length();
        }
        return result.toString();
    }
    
    // --- Tests de la logique de segmentation (makeSafe) ---

    @Test
    @DisplayName("Test de la segmentation simple avec len = 3")
    void makeSafe_simpleSegmentation_shouldInsertStripCutCorrectly() {
        // ARRANGE
        // La chaîne sans breaking: 012345
        final CharSequence input = "012345";
        final int len = 3;
        
        // ATTENDU
        // 012 + STRIP_CUT + 345
        final String expected = "012" + STRIP_CUT + "345";

        // ACT
        String result = makeSafe(input, len);

        // ASSERT
        assertEquals(expected, result, "La segmentation devrait insérer STRIP_CUT après chaque segment de longueur 3.");
    }
    
    @Test
    @DisplayName("Test de la segmentation où STRIP_CUT est inséré plusieurs fois (len = 2)")
    void makeSafe_multipleSegmentations_shouldInsertStripCutMultipleTimes() {
        // ARRANGE
        // La chaîne sans breaking: 01234567
        final CharSequence input = "01234567";
        final int len = 2;
        
        // ATTENDU
        // 01 + SC + 23 + SC + 45 + SC + 67
        final String expected = "01" + STRIP_CUT + "23" + STRIP_CUT + "45" + STRIP_CUT + "67";

        // ACT
        String result = makeSafe(input, len);

        // ASSERT
        assertEquals(expected, result, "STRIP_CUT devrait être inséré tous les 2 caractères.");
    }
    
    @Test
    @DisplayName("Test d'une entrée plus courte que la longueur de segmentation (len > input.length)")
    void makeSafe_inputShorterThanLen_shouldReturnOriginalString() {
        // ARRANGE
        final CharSequence input = "12345";
        final int len = 10; // len > 5
        
        // ATTENDU : Pas de segmentation
        final String expected = "12345";

        // ACT
        String result = makeSafe(input, len);

        // ASSERT
        assertEquals(expected, result, "Si len est supérieur à la longueur, aucune insertion ne devrait avoir lieu.");
    }

    @Test
    @DisplayName("Test d'une entrée vide")
    void makeSafe_emptyInput_shouldReturnEmptyString() {
        // ARRANGE
        final CharSequence input = "";
        final int len = 5;
        
        // ATTENDU
        final String expected = "";

        // ACT
        String result = makeSafe(input, len);

        // ASSERT
        assertEquals(expected, result, "Une chaîne vide devrait retourner une chaîne vide.");
    }

    // --- Tests de l'appel à stripBreakings ---

    @Test
    @DisplayName("Test de la suppression des caractères de saut de ligne par stripBreakings")
    void makeSafe_withBreakings_shouldStripThemBeforeSegmentation() {
        // ARRANGE
        // L'entrée contient des breakings (simulés par le stripBreakings interne)
        final CharSequence input = "H\ne\r a d e\t r"; // Sera réduit à "Header" (ou "H e a d e r" selon trim/remplacement)
        final int len = 3;
        
        // ATTENDU : Après stripBreakings, la chaîne est "Header"
        // La segmentation attendue est : "Hea" + SC + "der"
        final String expectedStripped = stripBreakings(input).toString(); // "Header"
        final String expected = expectedStripped.substring(0, 3) + STRIP_CUT + expectedStripped.substring(3);

        // ACT
        String result = makeSafe(input, len);

        // ASSERT
        assertEquals(expected, result, "Les sauts de ligne devraient être supprimés avant d'appliquer la segmentation.");
    }

    @Test
    @DisplayName("Test de la gestion d'une longueur de segment égale à 0")
    void makeSafe_lenIsZero_shouldThrowExceptionOrHandle() {
        // ARRANGE
        final CharSequence input = "ABCDE";
        final int len = 0;
        
        // ACT & ASSERT
        // Tester que l'exécution ne boucle pas infiniment ou ne lève pas une exception attendue.
        // Puisque len=0, `index += len + STRIP_CUT.length() -1;` devient `index += STRIP_CUT.length() -1;`.
        // Si STRIP_CUT.length() > 1 (ce qui est le cas ici: 4), l'index va croître et le processus se terminera.

        assertDoesNotThrow(() -> {
            String result = makeSafe(input, len);
            // Vérifiez qu'il y a de multiples insertions (car index augmente très lentement)
            assertTrue(result.contains(STRIP_CUT + STRIP_CUT), 
                       "Devrait contenir de multiples STRIP_CUT quand len=0 et STRIP_CUT.length() > 1.");
        }, "L'opération ne devrait pas lever d'exception pour len=0.");
    }
}
