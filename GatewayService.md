Passer de `WebClient` bloquant à `RestClient` est une excellente initiative. Introduit dans Spring Framework 6.1 (et Spring Boot 3.2), le **RestClient** offre une API fluide et synchrone qui élimine la complexité de Project Reactor (`block()`, `Schedulers`, etc.) tout en conservant une syntaxe moderne.

Voici comment refactoriser votre code pour migrer vers `RestClient`.

### 1. Remplacement de la structure de base

Le `ResponseSpec` de WebClient n'est pas compatible avec RestClient. Nous allons utiliser l'interface `RestClient.RequestBodySpec` et `RestClient.ResponseSpec`.

```java
private final RestClient restClient;

// Injection via constructeur recommandée
public MyService(RestClient.Builder restClientBuilder) {
    this.restClient = restClientBuilder.build();
}

public RestClient.ResponseSpec retrieve(final HttpMethod httpMethod, final String uri, final String apiName, final HttpHeaders headers, final Object body, final Object... uriVariables) {
    return restClient.method(httpMethod)
            .uri(uri, uriVariables)
            .headers(h -> h.addAll(headers))
            .body(body)
            .retrieve();
}

public RestClient.ResponseSpec retrieve(final HttpMethod httpMethod, final String uri, final String apiName, final HttpHeaders headers, final Object body, final Map<String, ?> uriVariables) {
    return restClient.method(httpMethod)
            .uri(uri, uriVariables)
            .headers(h -> h.addAll(headers))
            .body(body)
            .retrieve();
}

```

---

### 2. Migration de la méthode `toExchange`

Le plus gros avantage ici est la disparition de la gestion des Threads (`subscribeOn`) et de l'appel à `.block()`. La gestion des erreurs se fait désormais via `.onStatus()`.

```java
private static <T> ResponseEntity<T> toExchange(final RestClient.ResponseSpec rcResponse, final ParameterizedTypeReference<T> responseType) {
    return rcResponse
            .onStatus(HttpStatusCode::isError, (request, response) -> {
                // RestClient lève des exceptions spécifiques (RestClientResponseException)
                // On peut ici mapper vers votre exception personnalisée
                throw GatewayService.toRestTemplateException(response);
            })
            .toEntity(responseType);
}

```

---

### Comparaison des Flux

| Caractéristique | WebClient + `.block()` | RestClient |
| --- | --- | --- |
| **Modèle** | Réactif (détourné en bloquant) | Synchrone natif |
| **Complexité** | Haute (Gestion des Schedulers) | Faible (Direct) |
| **Performance** | Overhead dû à l'infrastructure Flux | Optimisé pour les appels bloquants |
| **Dépendances** | Requiert `spring-boot-starter-webflux` | Inclus dans `spring-boot-starter-web` |

### Points d'attention pour votre migration

* **Exceptions :** `WebClient` lève des `WebClientResponseException`. `RestClient` lève des `RestClientResponseException` (similaires à celles de `RestTemplate`). Vérifiez que votre méthode `toRestTemplateException` accepte bien ces nouveaux types ou les types génériques de `ClientHttpResponse`.
* **Intercepteurs :** Si vous aviez des `ExchangeFilterFunction` sur votre WebClient, vous devrez les convertir en `ClientHttpRequestInterceptor` pour RestClient.
* **Configuration :** Assurez-vous d'injecter `RestClient.Builder` pour bénéficier de la configuration automatique de Spring Boot (sérialiseurs JSON, etc.).

Souhaitez-vous que je vous aide à adapter spécifiquement votre méthode `toRestTemplateException` pour qu'elle soit compatible avec les réponses du `RestClient` ?

## RestClient Filter Interceptor
// Dans votre méthode de build ou de configuration
public ClientHttpRequestInterceptor oauthInterceptor(OAuth2AuthorizedClientManager authorizedClientManager, String registrationId) {
    
    return (request, body, execution) -> {
        // 1. Création de la requête d'autorisation
        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId(registrationId)
                .principal(SecurityContextHolder.getContext().getAuthentication())
                .build();

        // 2. Récupération du client autorisé (synchrone)
        OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);

        // 3. Ajout du token Bearer aux headers
        if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
            request.getHeaders().setBearerAuth(authorizedClient.getAccessToken().getTokenValue());
        }

        return execution.execute(request, body);
    };
}

## buildRestClient

public RestClient buildRestClient(final String baseUrl,
                                  final @Valid SslProperties ssl,
                                  final Duration connectionTimeout, 
                                  final Duration responseTimeout, 
                                  final DataSize maxInMemorySize, 
                                  final HttpHeaders defaultHeaders, 
                                  final List<ClientHttpRequestInterceptor> interceptors, // Changement ici
                                  final ConnectionProperties connectionProvider) {

    // 1. On garde votre logique Netty existante
    final HttpClient nettyHttpClient = buildNettyHttpClient(connectionTimeout, responseTimeout, ssl, connectionProvider);

    // 2. On encapsule Netty dans une Factory compatible RestClient
    final ReactorClientHttpRequestFactory requestFactory = new ReactorClientHttpRequestFactory(nettyHttpClient);

    // 3. Construction du RestClient
    return RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(requestFactory) // Utilise Netty en dessous
            .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                throw toRestTemplateException(response);
            })
            .defaultHeaders(h -> {
                h.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                if (defaultHeaders != null) h.addAll(defaultHeaders);
            })
            // Équivalent du maxInMemorySize (via le message converter)
            .messageConverters(converters -> {
                 converters.stream()
                    .filter(MappingJackson2HttpMessageConverter.class::isInstance)
                    .map(MappingJackson2HttpMessageConverter.class::cast)
                    .forEach(c -> c.setObjectMapper(objectMapper));
            })
            // Remplacement des filters par des interceptors
            .interceptors(actualInterceptors -> {
                if (interceptors != null) actualInterceptors.addAll(interceptors);
            })
            .build();
}
