== Guide du Développeur : Activation des Logs HTTP (WebClient)

Pour des raisons de performance et de sécurité, le traçage complet des échanges HTTP (incluant les corps JSON) est conditionné par le niveau de log `TRACE`.

=== Activation via Spring Boot Admin (SBA)

L'activation à chaud est la méthode recommandée pour déboguer un flux sur un environnement spécifique sans redémarrage.

. Connectez-vous à l'interface **Spring Boot Admin**.
. Sélectionnez l'instance de l'application concernée.
. Dans le menu de gauche, allez dans **Loggers**.
. Dans le champ de recherche ("Filter by name"), saisissez le nom de la classe : `FullLoggingFilter`.
. Localisez la ligne correspondant au package : `com.votreprojet.path.to.filter.FullLoggingFilter`.
. Cliquez sur le bouton **TRACE** dans la colonne de droite.

[NOTE]
====
Une fois vos tests terminés, pensez à repasser le niveau sur **INFO** ou **OFF** pour libérer les ressources de buffering et éviter de saturer les fichiers de logs.
====

=== Format de sortie dans la console Log Viewer

Dès l'activation, les échanges apparaissent sous cette forme dans l'onglet **Logfile** de SBA :

[source,text]
----
--- WEBCLIENT EXCHANGE ---
URL         : POST http://service-externe/api/data
REQ BODY    : {"key": "value"}
RES STATUS  : 201 CREATED
RES BODY    : {"id": "ABC-123", "result": "stored"}
--------------------------
----

=== Pourquoi le niveau TRACE ?

L'implémentation repose sur un `FullLoggingFilter`. Ce filtre utilise une logique de **"Lazy Logging"** :
* Si le niveau est > `TRACE` : Le filtre laisse passer la requête normalement (overhead quasi nul).
* Si le niveau est = `TRACE` : Le filtre active la capture par `FakeClientHttpRequest` et le buffering des flux pour permettre l'affichage des corps JSON.

[CAUTION]
====
L'activation du niveau `TRACE` sur un service à très fort trafic peut augmenter légèrement la consommation CPU et mémoire (JVM) car chaque payload JSON est converti en String en mémoire.
====

Voici la section **Asciidoc** dédiée au `RestClient`. Elle suit la même structure que celle du `WebClient` pour assurer une cohérence dans ta documentation technique, tout en précisant les spécificités liées à son fonctionnement synchrone.

---

```asciidoc
== Guide du Développeur : Activation des Logs HTTP (RestClient)

Le `RestClient` (introduit comme l'alternative moderne et synchrone au WebClient) intègre un mécanisme de log détaillé via l'intercepteur `RestClientLoggingInterceptor`. 

Tout comme pour le WebClient, ce traçage est **désactivé par défaut** et nécessite une activation manuelle du niveau `TRACE`.

=== Activation via Spring Boot Admin (SBA)

Pour observer les payloads JSON circulant via le `RestClient` :

. Accédez à votre instance dans **Spring Boot Admin**.
. Allez dans la section **Loggers**.
. Recherchez la classe : `RestClientLoggingInterceptor`.
. Basculez le niveau de `INFO` (ou `OFF`) vers **TRACE**.

[TIP]
====
Si vous avez plusieurs clients HTTP dans la même application, vous pouvez filtrer par le nom du package commun pour activer tous les intercepteurs de logs d'un seul coup.
====

=== Détails techniques de l'implémentation

Contrairement aux filtres réactifs, l'intercepteur du `RestClient` fonctionne de manière impérative :

1. **Capture de la requête** : Le corps de la requête est intercepté sous forme de tableau d'octets (`byte[]`) avant l'envoi.
2. **Buffering de la réponse** : Pour permettre la lecture du corps de la réponse à la fois par le log ET par votre code métier, nous utilisons une `BufferingClientHttpRequestFactory`.
3. **Journalisation** : Le log n'est déclenché que si `log.isTraceEnabled()` est vrai, garantissant ainsi un impact CPU nul en conditions normales.

=== Exemple de sortie (Logfile)

[source,text]
----
--- RESTCLIENT EXCHANGE ---
URL         : GET http://service-metier/api/v1/check
REQ BODY    : [EMPTY]
RES STATUS  : 200 OK
RES BODY    : {"status": "UP", "details": "Service operational"}
---------------------------
----

=== Précautions d'usage

* **Consommation Mémoire** : L'utilisation de la `BufferingClientHttpRequestFactory` implique que la réponse est stockée intégralement en mémoire RAM avant d'être traitée. 
* **Fichiers volumineux** : Évitez d'activer le niveau `TRACE` si vous utilisez ce client pour télécharger des fichiers volumineux (plusieurs Mo), car cela pourrait provoquer une `OutOfMemoryError`.

[WARNING]
====
Assurez-vous que les données sensibles (mots de passe, tokens) ne sont pas logguées en clair. Si nécessaire, utilisez un masqueur de données en amont du log.
====
```

---

### Points clés ajoutés pour le RestClient :
* **Précision sur la factory** : Il est important que le développeur sache que le "Buffering" est actif, car cela explique comment on peut lire le corps de la réponse sans fermer le stream.
* **Avertissement sur les fichiers** : C'est une spécificité du mode synchrone bufferisé ; charger un gros fichier en `TRACE` est plus risqué qu'en streaming standard.
* **Formatage cohérent** : Le format de sortie est quasi identique au `WebClient`, ce qui facilite la lecture croisée pour les développeurs.
