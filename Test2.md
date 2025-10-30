import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TypeHelperParameterizeTest {

    @Test
    @DisplayName("Should create ParameterizedType for single type argument (List<String>)")
    void shouldCreateParameterizedTypeWithSingleTypeArgument() {
        // When
        ParameterizedType result = TypeHelper.parameterize(List.class, String.class);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRawType()).isEqualTo(List.class);
        assertThat(result.getActualTypeArguments()).hasSize(1);
        assertThat(result.getActualTypeArguments()[0]).isEqualTo(String.class);
    }

    @Test
    @DisplayName("Should create ParameterizedType for multiple type arguments (Map<String, Integer>)")
    void shouldCreateParameterizedTypeWithMultipleTypeArguments() {
        // When
        ParameterizedType result = TypeHelper.parameterize(Map.class, String.class, Integer.class);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRawType()).isEqualTo(Map.class);
        assertThat(result.getActualTypeArguments()).hasSize(2);
        assertThat(result.getActualTypeArguments()[0]).isEqualTo(String.class);
        assertThat(result.getActualTypeArguments()[1]).isEqualTo(Integer.class);
    }
}
-------
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class YourClassTest {

    @Test
    @DisplayName("Should fail with message when actualType is an interface")
    void shouldFailWithMessageWhenActualTypeIsInterface() {
        // Given
        Type actualType = Runnable.class; // Exemple d'interface
        
        // When & Then
        assertThatThrownBy(() -> {
            if (TypeHelper.isInterface(actualType)) {
                failWithMessage("PewDto Collection is not an Iterable/Map interface. Please correct : %s", 
                    TypeHelper.getSimpleName(actualType));
            }
        })
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("PewDto Collection is not an Iterable/Map interface")
        .hasMessageContaining("Runnable");
    }
}
-------
