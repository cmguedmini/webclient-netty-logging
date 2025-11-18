package com.example.utils; // Adaptez le package selon votre structure

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Classe de test pour les méthodes de compilation insensibles à la casse de RegexHelper.
 */
class RegexHelperTest {

    private static final String INSENSITIVE_WORD = "Test";
    private static final String WORD_TO_MATCH = "tEsT"; // Casse différente pour le test

    // Supposons que votre classe RegexHelper a les méthodes statiques suivantes :
    // public static Pattern compileInsensitive(final String regex)
    // public static Pattern[] compileInsensitive(final Collection<String> regex)
    // public static Pattern[] compileInsensitive(final String... regex)

    // Note : Pour que ces tests compilent et s'exécutent,
    // vous devez avoir implémenté PatternFlag.CASE_INSENSITIVE
    // ou utiliser directement Pattern.CASE_INSENSITIVE.
    // Dans cet exemple, je suppose que l'implémentation est correcte
    // et que PatternFlag.CASE_INSENSITIVE est équivalent à Pattern.CASE_INSENSITIVE (2).

    // --- Test pour compileInsensitive(String regex) ---

    @Test
    @DisplayName("Test de la compilation d'un Pattern unique insensible à la casse")
    void compileInsensitive_singleRegex_shouldBeCaseInsensitive() {
        // ACT
        Pattern pattern = RegexHelper.compileInsensitive(INSENSITIVE_WORD);

        // ASSERT
        // 1. Vérifier que le drapeau CASE_INSENSITIVE est bien activé (Pattern.CASE_INSENSITIVE est 2)
        assertTrue((pattern.flags() & Pattern.CASE_INSENSITIVE) != 0, 
                   "Le drapeau CASE_INSENSITIVE devrait être activé.");

        // 2. Vérifier que l'expression régulière matche une chaîne de casse différente
        assertTrue(pattern.matcher(WORD_TO_MATCH).matches(), 
                   "Le pattern devrait matcher la chaîne indépendamment de la casse.");

        // 3. Vérifier qu'un pattern sensible à la casse (pour comparaison) ne matcherait pas
        Pattern caseSensitivePattern = Pattern.compile(INSENSITIVE_WORD);
        assertFalse(caseSensitivePattern.matcher(WORD_TO_MATCH).matches(), 
                    "Un pattern sensible à la casse ne devrait pas matcher.");
    }

    @Test
    @DisplayName("Test de la compilation d'un Pattern unique avec une chaîne vide")
    void compileInsensitive_emptyString_shouldCompile() {
        // ACT
        Pattern pattern = RegexHelper.compileInsensitive("");

        // ASSERT
        assertNotNull(pattern, "Le pattern compilé ne devrait pas être null.");
        // Un pattern vide (Pattern.compile("")) matche toujours une chaîne vide
        assertTrue(pattern.matcher("").matches(), "Le pattern vide devrait matcher une chaîne vide.");
        assertFalse(pattern.matcher("a").matches(), "Le pattern vide ne devrait pas matcher une chaîne non vide.");
    }

    // --- Test pour compileInsensitive(Collection<String> regex) ---

    @Test
    @DisplayName("Test de la compilation d'une Collection de Patterns insensibles à la casse")
    void compileInsensitive_collectionOfRegex_shouldReturnCaseInsensitivePatterns() {
        // ARRANGE
        Collection<String> regexList = Arrays.asList(INSENSITIVE_WORD, "AnOther");

        // ACT
        Pattern[] patterns = RegexHelper.compileInsensitive(regexList);

        // ASSERT
        assertNotNull(patterns, "Le tableau de patterns ne devrait pas être null.");
        assertEquals(2, patterns.length, "Le nombre de patterns devrait être égal au nombre d'expressions fournies.");

        // Vérifier le premier pattern
        assertTrue((patterns[0].flags() & Pattern.CASE_INSENSITIVE) != 0, 
                   "Le premier pattern devrait être insensible à la casse.");
        assertTrue(patterns[0].matcher(WORD_TO_MATCH).matches(), 
                   "Le premier pattern devrait matcher une casse différente.");
                   
        // Vérifier le second pattern (par exemple avec une autre chaîne)
        assertTrue((patterns[1].flags() & Pattern.CASE_INSENSITIVE) != 0, 
                   "Le second pattern devrait être insensible à la casse.");
        assertTrue(patterns[1].matcher("aNoThEr").matches(), 
                   "Le second pattern devrait matcher une casse différente.");
    }
    
    @Test
    @DisplayName("Test de la compilation d'une Collection vide")
    void compileInsensitive_emptyCollection_shouldReturnEmptyArray() {
        // ARRANGE
        Collection<String> emptyList = Arrays.asList();

        // ACT
        Pattern[] patterns = RegexHelper.compileInsensitive(emptyList);

        // ASSERT
        assertNotNull(patterns, "Le tableau de patterns ne devrait pas être null.");
        assertEquals(0, patterns.length, "Le tableau devrait être vide.");
    }

    // --- Test pour compileInsensitive(String... regex) ---

    @Test
    @DisplayName("Test de la compilation d'un tableau d'arguments de Patterns insensibles à la casse")
    void compileInsensitive_varargsOfRegex_shouldReturnCaseInsensitivePatterns() {
        // ARRANGE
        String[] regexArray = {INSENSITIVE_WORD, "FinAl"};

        // ACT
        Pattern[] patterns = RegexHelper.compileInsensitive(regexArray);

        // ASSERT
        assertNotNull(patterns, "Le tableau de patterns ne devrait pas être null.");
        assertEquals(2, patterns.length, "Le nombre de patterns devrait être égal au nombre d'expressions fournies.");

        // Vérifier le premier pattern
        assertTrue((patterns[0].flags() & Pattern.CASE_INSENSITIVE) != 0, 
                   "Le premier pattern (varargs) devrait être insensible à la casse.");
        assertTrue(patterns[0].matcher(WORD_TO_MATCH).matches(), 
                   "Le premier pattern (varargs) devrait matcher une casse différente.");
                   
        // Vérifier le second pattern
        assertTrue((patterns[1].flags() & Pattern.CASE_INSENSITIVE) != 0, 
                   "Le second pattern (varargs) devrait être insensible à la casse.");
        assertTrue(patterns[1].matcher("fInAl").matches(), 
                   "Le second pattern (varargs) devrait matcher une casse différente.");
    }

    @Test
    @DisplayName("Test de la compilation d'un tableau d'arguments vide (varargs)")
    void compileInsensitive_emptyVarargs_shouldReturnEmptyArray() {
        // ACT
        Pattern[] patterns = RegexHelper.compileInsensitive();

        // ASSERT
        assertNotNull(patterns, "Le tableau de patterns ne devrait pas être null.");
        assertEquals(0, patterns.length, "Le tableau devrait être vide.");
    }
}
