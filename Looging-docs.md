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
