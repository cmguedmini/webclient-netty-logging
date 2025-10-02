import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test unitaire pour la méthode sendReceive avec RestClient
 */
class MyServiceTest {

    private MockWebServer mockWebServer;
    private MyService myService;
    private JefContextService jefContextService;

    @BeforeEach
    void setUp() throws IOException {
        // Démarrer le serveur mock
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // Créer le RestClient pointant vers le serveur mock
        RestClient restClient = RestClient.builder()
            .baseUrl(mockWebServer.url("/").toString())
            .build();

        // Mock du JefContextService
        jefContextService = new JefContextService();

        // Créer le service à tester
        myService = new MyService(restClient, jefContextService);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void sendReceive_shouldReturnResponseBody() {
        // Given
        String expectedResponse = "{\"status\":\"success\",\"data\":\"test\"}";
        mockWebServer.enqueue(new MockResponse()
            .setBody(expectedResponse)
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json"));

        Request request = new Request();
        request.setData("test data");
        request.setType("TEST");

        // When
        String actualResponse = myService.sendReceive(request, "/api/endpoint");

        // Then
        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void sendReceive_shouldSendCorrectRequestBody() throws InterruptedException {
        // Given
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"status\":\"ok\"}")
            .setResponseCode(200));

        Request request = new Request();
        request.setData("important data");
        request.setType("CRITICAL");

        // When
        myService.sendReceive(request, "/api/endpoint");

        // Then
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getPath()).isEqualTo("/api/endpoint");
        assertThat(recordedRequest.getBody().readUtf8()).contains("important data");
    }

    @Test
    void sendReceive_shouldSendCorrectHeaders() throws InterruptedException {
        // Given
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"status\":\"ok\"}")
            .setResponseCode(200));

        Request request = new Request();
        request.setData("test");

        // When
        myService.sendReceive(request, "/api/endpoint");

        // Then
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getHeader("Content-Type"))
            .contains("application/json");
    }

    @Test
    void sendReceive_shouldHandleErrorResponse() {
        // Given
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"error\":\"Bad Request\"}")
            .setResponseCode(400));

        Request request = new Request();
        request.setData("invalid data");

        // When/Then
        try {
            myService.sendReceive(request, "/api/endpoint");
        } catch (Exception e) {
            // RestClient lance une exception pour les codes 4xx/5xx
            assertThat(e).isInstanceOf(org.springframework.web.client.HttpClientErrorException.class);
        }
    }
}

// ===== Classes de support =====

class MyService {
    private final RestClient restClient;
    private final JefContextService jefContextService;

    public MyService(RestClient restClient, JefContextService jefContextService) {
        this.restClient = restClient;
        this.jefContextService = jefContextService;
    }

    public String sendReceive(Request request, String uri) {
        return restClient.post()
            .uri(uri)
            .body(buildBody(request))
            .headers(headers -> buildHeaders(request, headers, jefContextService.getMySelfPoint()))
            .retrieve()
            .body(String.class);
    }

    private Object buildBody(Request request) {
        return request;
    }

    private void buildHeaders(Request request, HttpHeaders headers, Object mySelfPoint) {
        headers.setContentType(MediaType.APPLICATION_JSON);
    }
}

class Request {
    private String data;
    private String type;

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}

class JefContextService {
    public Object getMySelfPoint() {
        return new Object();
    }
}
