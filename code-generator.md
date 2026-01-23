C'est entendu. Voici une version concise et directe à intégrer dans votre documentation, mettant l'accent sur la cohérence avec la structure Java existante et l'utilisation du générateur spécifique pour TypeScript.

---

## 🌐 Génération des Clients TypeScript et Python

En complément de la génération des classes Java, nous mettons à disposition des utilitaires pour générer des clients dans d'autres langages. Ces outils partagent la **même structure de projet** et la **même configuration Maven** que la partie Java, garantissant une stricte cohérence avec le contrat d'interface (YAML).

### 🛠️ Commandes de génération

Pour générer les clients, utilisez les profils Maven dédiés via la phase `compile` :

* **Client TypeScript** :
```bash
mvn compile -Pcodegen-ts-openapi

```


* **Générateur utilisé** : `typescript-fetch` (pour une compatibilité maximale sans dépendances lourdes).
* **Cible** : Idéal pour les intégrations Frontend modernes.


* **Client Python** :
```bash
mvn compile -Pcodegen-python-openapi

```


* **Cible** : Utilisé pour les scripts d'automatisation, les tests ou les services de Data Science.



### 🏗️ Structure et Emplacement

Ces utilitaires respectent l'arborescence standard du projet définie dans le `pom.xml`. Les sources sont générées dans des répertoires isolés au sein de `target/generated-sources/openapi/` (au même niveau que le code Java généré), évitant ainsi toute pollution du code source versionné.

> [!IMPORTANT]
> Tout comme pour le code Java, ces fichiers sont éphémères. Toute modification doit être effectuée dans la spécification OpenAPI source et non directement dans les fichiers générés.

---

Souhaitez-vous que je rédige une courte section sur la manière d'**importer** le client `typescript-fetch` dans un projet front-end pour aider les développeurs à démarrer ?
