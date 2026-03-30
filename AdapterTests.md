## Tests pour `LocalDateAdapter`

```java
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LocalDateAdapterTest {

    @Test
    @DisplayName("parse should return LocalDate when value is not null")
    void parse_shouldReturnLocalDateWhenValueIsNotNull() {
        assertEquals(LocalDate.of(2026, 3, 30), LocalDateAdapter.parse("2026-03-30"));
    }

    @Test
    @DisplayName("parse should return null when value is null")
    void parse_shouldReturnNullWhenValueIsNull() {
        assertNull(LocalDateAdapter.parse(null));
    }

    @Test
    @DisplayName("print should return ISO string when value is not null")
    void print_shouldReturnIsoStringWhenValueIsNotNull() {
        assertEquals("2026-03-30", LocalDateAdapter.print(LocalDate.of(2026, 3, 30)));
    }

    @Test
    @DisplayName("print should return null when value is null")
    void print_shouldReturnNullWhenValueIsNull() {
        assertNull(LocalDateAdapter.print(null));
    }
}
```

## Tests pour `LocalDateTimeAdapter`

```java
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LocalDateTimeAdapterTest {

    @Test
    @DisplayName("parse should return LocalDateTime when value is not null")
    void parse_shouldReturnLocalDateTimeWhenValueIsNotNull() {
        assertEquals(LocalDateTime.of(2026, 3, 30, 12, 9, 0),
                LocalDateTimeAdapter.parse("2026-03-30T12:09:00"));
    }

    @Test
    @DisplayName("parse should return null when value is null")
    void parse_shouldReturnNullWhenValueIsNull() {
        assertNull(LocalDateTimeAdapter.parse(null));
    }

    @Test
    @DisplayName("print should return ISO string when value is not null")
    void print_shouldReturnIsoStringWhenValueIsNotNull() {
        assertEquals("2026-03-30T12:09",
                LocalDateTimeAdapter.print(LocalDateTime.of(2026, 3, 30, 12, 9)));
    }

    @Test
    @DisplayName("print should return null when value is null")
    void print_shouldReturnNullWhenValueIsNull() {
        assertNull(LocalDateTimeAdapter.print(null));
    }
}
```

## Variante plus compacte

Si tu veux alléger les tests et maximiser la lisibilité, tu peux aussi utiliser des tests paramétrés pour les cas non nuls. JUnit 5 supporte ce style pour réduire la duplication. [petrikainulainen](https://www.petrikainulainen.net/programming/testing/junit-5-tutorial-writing-parameterized-tests/)

Si tu veux, je peux aussi te générer :
- une version avec `@ParameterizedTest`,
- une version avec AssertJ,
- ou directement les fichiers `src/test/java/...` prêts à copier-coller.
