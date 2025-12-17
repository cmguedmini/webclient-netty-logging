
## ✅ Passage à **RestClientFactory** (remplaçant recommandé de `RestTemplateHelper`)

Dans le cadre de la migration vers **Spring Boot 3.5.x / Spring Framework 6**, nous **recommandons d’utiliser `RestClientFactory`** à la place de **`RestTemplateHelper`** pour toutes les intégrations HTTP côté client. `RestClientFactory` s’appuie sur l’API **`RestClient`** (moderne et maintenue) et apporte **nativement** :

- **Timeouts centralisés** (connexion, lecture, attente de socket) via `HttpComponentsClientHttpRequestFactory`.
- **Retry transparent** (exceptions I/O & codes HTTP `5xx` / `429`) avec **backoff exponentiel** et respect de l’**idempotence**.
- **Configuration externalisée** via `application.yml` (avec **valeurs par défaut** et **validation JSR‑303**, `baseUrl` **obligatoire**).
- **Support TLS** via `SSLContext` et **intégration `ObjectMapper`** pour les conventions JSON d’entreprise.

> 📌 **Action recommandée** : pour tous les nouveaux développements et les évolutions, **remplacez `RestTemplateHelper` par `RestClientFactory`** afin de bénéficier de la configuration unifiée (timeouts/retry), des meilleures pratiques HTTP et d’un code plus simple à maintenir.

**👉 Pour la configuration détaillée, des exemples complets et les bonnes pratiques (timeouts, retry, TLS, ObjectMapper, idempotence), consultez la page dédiée :**  
**`Wiki ▸ RestClientFactory (config & utilisation)`**

---

### 🧩 Migration rapide — exemple

```java
// Avant (RestTemplateHelper)
UserDto dto = restTemplateHelper.getForObject("/v1/users/{id}", UserDto.class, id);

// Après (RestClientFactory)
RestClient client = RestClientFactory.build(props, sslContext, objectMapper);
UserDto dto = client.get()
    .uri("/v1/users/{id}", id)
    .retrieve()
   
