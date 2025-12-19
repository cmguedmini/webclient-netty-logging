
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class UserService {

    private final RestTemplate restTemplate = new RestTemplate();

    public ResponseEntity<User> getUser() {
        String url = "http://my.example.com/users";
        return restTemplate.getForEntity(url, User.class);
    }
}

// Exemple DTO
public class User {
    private Long id;
    private String name;
    private String email;

    // Getters / Setters / Constructeurs
}
``


## 1) GET simple — **URL absolue**

```java
public User getUserSimple() {
    return restClient.get()
            .uri("http://my.example.com/users")   // URL directe
            .retrieve()
            .body(User.class);
}
```

***

## 2) GET avec **Query Parameters**

> Exemple : `GET /users?active=true&role=admin`

```java
public User getUserWithQueryParams(boolean active, String role) {
    return restClient.get()
            .uri(uriBuilder -> uriBuilder
                    .path("/users")
                    .queryParam("active", active)
                    .queryParam("role", role)
                    .build())
            .retrieve()
            .body(User.class);
}
```

***

## 3) GET avec **Path Parameters**

> Exemple : `GET /users/{id}`

```java
public User getUserById(Long id) {
    return restClient.get()
            .uri("/users/{id}", id)   // substitution de path variable
            .retrieve()
            .body(User.class);
}
```

***

## 4) GET avec **Headers**

> Exemple : ajouter `Authorization`, `X-Trace-Id`

```java
public User getUserWithHeaders(String bearerToken, String traceId) {
    return restClient.get()
            .uri("/users")
            .header("Authorization", "Bearer " + bearerToken)
            .header("X-Trace-Id", traceId)
            .retrieve()
            .body(User.class);
}
```

***

## 5) GET avec **gestion d’erreurs `onStatus`** (4xx, 5xx, cas particulier 404)

> *   **4xx** : lever une `ClientErrorException` (ex. custom)
> *   **5xx** : lever une `ServerErrorException` (ex. custom)
> *   **404** : traiter comme un **cas particulier** (ex. retourner `null` ou une valeur par défaut)

```java
// Exemples d'exceptions custom (optionnel)
class ClientErrorException extends RuntimeException {
    ClientErrorException(String msg) { super(msg); }
}
class ServerErrorException extends RuntimeException {
    ServerErrorException(String msg) { super(msg); }
}

public User getUserWithOnStatus(Long id) {
    return restClient.get()
            .uri("/users/{id}", id)
            .retrieve()
            // Gestion 4xx générique
            .onStatus(status -> status.is4xxClientError() && status.value() != 404,
                      (req, res) -> {
                          String body = res.body(String.class); // lire la réponse si besoin
                          return new ClientErrorException("4xx received: " + res.statusCode() + " - " + body);
                      })
            // Cas particulier: 404 -> retourner null (ou une valeur par défaut)
            .onStatus(status -> status.value() == 404,
                      (req, res) -> {
                          // Ici, on court-circuite l’erreur en renvoyant une valeur alternative.
                          // La méthode 'retrieve().body(...)' lève par défaut, donc on utilise bodyOrNull().
                          // -> Solution : changer le flux pour récupérer explicitement la réponse.
                          return RestClient.ResponseSpec.createEmptyError(); // Pas d'erreur, on gère après
                      })
            // Gestion 5xx
            .onStatus(status -> status.is5xxServerError(),
                      (req, res) -> {
                          String body = res.body(String.class);
                          return new ServerErrorException("5xx received: " + res.statusCode() + " - " + body);
                      })
            // Extraction avec fallback 404 → null
            .toEntity(User.class)                // récupérer l’entité pour inspecter le code
            .map(response -> {
                if (response.getStatusCode().value() == 404) {
                    return null; // ou new User(... valeur par défaut …)
                }
                return response.getBody();
            });
}
```

> **Variante plus compacte 404 → null**  
> Si tu veux directement une méthode dédiée pour 404 → `null` :

```java
public User getUserOrNull(Long id) {
    var response = restClient.get()
            .uri("/users/{id}", id)
            .retrieve()
            .toEntity(User.class);
    return (response.getStatusCode().value() == 404) ? null : response.getBody();
}
```

***

### Notes pratiques

*   Préférer **`RestClient.Builder` injecté** plutôt que `RestClient.create()` pour bénéficier de la configuration Spring Boot (converters, partage des ressources, etc.).
*   Les paramètres d’URL :
    *   **Path params** : `uri("/users/{id}", id)`
    *   **Query params** : via `uriBuilder` (cf. snippet #2).
*   Pour des **headers** “globaux”, tu peux les configurer au **niveau du builder** (ex. `defaultHeader` si tu crées un wrapper), ou utiliser un **interceptor** si besoin de logique.
*   Pour `onStatus`, la stratégie dépend de ton cas d’usage : lever des exceptions custom, renvoyer une valeur par défaut, ou mapper vers un type `Optional<User>`.

Si tu veux, je peux te regrouper tout ça dans une **classe utilitaire** unique (`UserClient`) prête à copier-coller avec tests unitaires simples (Mockito/MockWebServer).
