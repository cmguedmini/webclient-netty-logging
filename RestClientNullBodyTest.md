import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class RestClientNullBodyTest {

    private RestClient restClient;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        restClient = RestClient.builder()
                .baseUrl("https://api.example.com")
                .build();
        
        mockServer = MockRestServiceServer.bindTo(restClient).build();
    }

    @Test
    void testPostWithNullBody() {
        // Arrange
        String endpoint = "/api/users";
        
        mockServer.expect(requestTo("https://api.example.com" + endpoint))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string("")) // Body vide attendu
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"id\":1,\"status\":\"created\"}"));

        // Act
        String response = restClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .body(null) // Body null
                .retrieve()
                .body(String.class);

        // Assert
        mockServer.verify();
        assertThat(response).isNotNull();
        assertThat(response).contains("\"id\":1");
        assertThat(response).contains("\"status\":\"created\"");
    }

    @Test
    void testPostDynamicBodyCanBeNull() {
        // Arrange
        String endpoint = "/api/events";
        Object dynamicBody = getDynamicBody(); // Retourne null
        
        mockServer.expect(requestTo("https://api.example.com" + endpoint))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(""))
                .andRespond(withStatus(HttpStatus.OK)
                        .body("Success"));

        // Act
        String response = restClient.post()
                .uri(endpoint)
                .body(dynamicBody) // null
                .retrieve()
                .body(String.class);

        // Assert
        mockServer.verify();
        assertThat(response).isEqualTo("Success");
    }

    // Méthode helper pour simuler un body dynamique qui peut être null
    private Object getDynamicBody() {
        return null; // Simule un cas où le body est null
    }
}
