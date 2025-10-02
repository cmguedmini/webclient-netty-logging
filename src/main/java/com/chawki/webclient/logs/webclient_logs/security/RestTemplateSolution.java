// Il faut que RestTemplate soit injecté ou créé (par exemple via RestTemplateBuilder)
RestTemplate restTemplate = ...; 
// Ou utiliser les méthodes qui prennent l'URL complète

// 1. Définir le corps de la requête (Request Entity)
// La méthode postForEntity permet de poster un objet et de spécifier le type de retour
// Il faut d'abord créer les entêtes dans un objet HttpHeaders
HttpHeaders headers = new HttpHeaders();
// Logique pour construire les headers:
buildHeaders(request, headers, jefContextService.getMySelfPoint());

// 2. Créer l'entité de requête complète (headers + body)
HttpEntity<Object> requestEntity = new HttpEntity<>(buildBody(request), headers);

// 3. Exécuter l'appel POST synchrone
// Note : Le RestTemplate utilise une approche plus "templatisée"
ResponseEntity<String> responseEntity = restTemplate.postForEntity(
    uri,                // L'URI de la requête
    requestEntity,      // L'objet contenant le corps et les entêtes
    String.class        // Le type de retour attendu pour le corps de la réponse
);

// 4. Récupérer le corps de la réponse
String response = responseEntity.getBody();

// Note : Avec RestTemplate, il est plus courant d'utiliser la méthode exchange() 
// si vous avez besoin d'une flexibilité maximale sur les entêtes et la méthode HTTP.
