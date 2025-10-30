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
