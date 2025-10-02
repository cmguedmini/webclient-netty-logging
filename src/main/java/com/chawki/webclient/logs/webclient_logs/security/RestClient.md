    Requête réussie

Oui, c'est vrai ! ✅

Le RestClient a été introduit dans Spring Framework 6.1 (et Spring Boot 3.2) avec pour objectif d'être le remplaçant moderne et synchrone de RestTemplate.

La relation RestClient et WebClient

Le RestClient est en réalité construit au-dessus de l'infrastructure de WebClient.

    Héritage de l'API Fluide : Le RestClient a volontairement adopté l'API fluide (méthode chaînée en utilisant des points, comme .post().uri().body().retrieve()) que WebClient a popularisée, rendant le code plus lisible et moderne que le RestTemplate (qui utilisait un modèle de "template" avec des méthodes surchargées comme postForEntity, getForObject, etc.).

    Moteur Puissant : En coulisses, le RestClient délègue les opérations de connexion et de traitement de bas niveau au même moteur que celui utilisé par WebClient.

    La Différence Clé : Le Blocage : La distinction essentielle réside dans l'exécution :

        WebClient est conçu pour l'asynchronisme et renvoie des types réactifs (Mono ou Flux).

        RestClient enveloppe cette infrastructure pour fournir un appel synchrone et bloquant (comme le faisait RestTemplate) tout en bénéficiant de la modernité et de la flexibilité du moteur de WebClient.

En d'autres termes, le RestClient est le meilleur des deux mondes pour les applications Spring MVC classiques : la simplicité synchrone de RestTemplate avec la modernité de l'API et de l'architecture de WebClient.

Pour en savoir plus sur l'introduction et la comparaison de ces clients REST dans Spring, vous pouvez regarder cette vidéo : New in Spring Framework 6.1: RestClient.

C'est une excellente question qui touche au cœur de l'architecture de ces clients HTTP ! 🧠

Pour répondre directement :

Le **`RestClient` utilise un seul thread** : le **thread appelant** (celui de votre service ou contrôleur) est bloqué jusqu'à ce que la réponse HTTP soit reçue, tout comme `RestTemplate`.

Le **`WebClient` utilisé en mode synchrone (`.block()`) implique deux threads**, ce qui est la raison principale pour laquelle le `RestClient` a été créé pour les applications synchrones.

---

## Gestion des Threads par Client

| Client | Mode d'opération | Nombre de Threads | Impact sur la Performance (MVC) | Explication |
| :--- | :--- | :--- | :--- | :--- |
| **`RestClient`** | Synchrone | **1 thread** | Équivalent à `RestTemplate`. Le thread appelant est bloqué. | Conçu pour les applications Spring MVC/bloquantes. |
| **`WebClient`** | Asynchrone | **1 thread** (non bloquant) | Idéal. Le thread appelant n'est pas utilisé pour l'attente I/O. | Le thread est libéré ; une notification est gérée par un *event loop* réactif (Project Reactor). |
| **`WebClient.block()`** | Synchrone | **2 threads** | **Coût inutile**. Bloque le thread appelant ET utilise un thread réactif en arrière-plan. | Utilisation incorrecte dans une application MVC bloquante (anti-pattern). |

---

## Le Problème de `WebClient.block()` (Le "Double Thread")

Lorsque vous utilisez `WebClient` dans une application Spring MVC (synchrone/bloquante) et que vous forcez l'appel à devenir bloquant avec `.block()`, voici ce qui se passe :

1.  **Le Thread 1 (Thread MVC)** : C'est le thread de votre contrôleur/service. Il appelle `webClient...block()`. Il est maintenant bloqué, attendant que le résultat soit disponible.
2.  **Le Thread 2 (Thread Réactif)** : `WebClient` exécute la requête de manière non-bloquante, en utilisant les *event loops* de Project Reactor (souvent sur un pool de threads différent, comme Reactor Netty). Ce thread est efficace pour l'I/O.

Le problème est que le **Thread 1 reste bloqué** alors que le **Thread 2 est mobilisé** pour attendre la réponse. Vous mobilisez donc **deux threads pour une seule opération** qui pourrait en utiliser un seul, ce qui réduit inutilement la capacité de votre serveur à gérer d'autres requêtes concurrentes. C'est ce qu'on appelle souvent un **anti-pattern** dans les applications synchrones.

## Comment `RestClient` Résout le Problème

Le **`RestClient`** utilise l'infrastructure moderne de `WebClient` mais est spécifiquement **conçu pour le mode bloquant simple**.

Lorsqu'il exécute une requête :

1.  Il utilise la puissance de `WebClient` pour la construction de la requête et la gestion des codecs/intercepteurs.
2.  Cependant, il est architecturé pour utiliser des mécanismes de client HTTP classiques (comme ceux basés sur l'API **`ClientHttpRequestFactory`** similaire à `RestTemplate`, ou une adaptation bloquante de l'infrastructure WebFlux), garantissant que **seul le thread appelant est bloqué** en attente de la réponse I/O.

En bref, **`RestClient` = API fluide + Synchrone (1 thread) + Moteur moderne**. C'est pourquoi il est la meilleure alternative à `RestTemplate` pour les applications qui ne sont pas réactives.
