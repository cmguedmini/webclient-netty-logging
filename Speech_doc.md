Excellente idée 👍

Pour un POC de 15-20 minutes, je te conseille un discours simple, naturel et orienté bénéfices. L'objectif n'est pas d'expliquer tous les détails d'Antora mais de montrer :

"Comment l'équipe peut maintenir la documentation aussi facilement que le code."

Slide 1 - Antora & Docs-as-Code
Speech

Bonjour à tous.

Aujourd'hui je vais vous présenter un POC autour d'Antora.

L'objectif n'est pas de parler uniquement de l'outil mais surtout de montrer une approche Docs-as-Code.

L'idée est simple : gérer la documentation comme du code source, dans Git, avec le même cycle de vie que les développements.

Nous verrons comment organiser la documentation, comment la mettre à jour et comment la publier automatiquement.

Slide 2 - Pourquoi Antora ?
Speech

Dans la plupart des projets, la documentation est dispersée entre différents outils : Wiki, SharePoint, Word ou PDF.

Le problème est qu'elle devient rapidement obsolète.

Antora permet de générer un site documentaire à partir de sources Git.

Les développeurs travaillent donc avec des fichiers texte, versionnés dans le dépôt, au même titre que le code.

On gagne en traçabilité, en cohérence et en maintenabilité.

Slide 3 - Concepts fondamentaux
Speech

Avant d'aller plus loin, il est utile de comprendre quelques concepts Antora.

Un composant représente une documentation métier ou technique.

Un module permet d'organiser les pages par thème.

Une page est simplement un fichier AsciiDoc.

Enfin, le playbook est le fichier qui agrège l'ensemble des documentations pour générer le site final.

Ce sont vraiment les quatre notions principales à retenir.

Slide 4 - Architecture Multi-Repos LuPIA
Speech

Dans notre contexte LuPIA, nous avons plusieurs projets :

Backend
Frontend
Parser & Indexer

Chaque projet peut contenir sa propre documentation.

Antora va récupérer ces différentes sources et construire une documentation unique.

Cela permet à chaque équipe de conserver son autonomie tout en offrant un point d'entrée centralisé.

Slide 5 - Agrégation Antora
Speech

Ici nous voyons le rôle du playbook.

Le playbook connaît les différents dépôts Git à agréger.

Lors de la génération du site, Antora récupère le contenu documentaire de chaque dépôt.

Le résultat est une navigation unique, même si la documentation provient de plusieurs référentiels distincts.

C'est l'un des points forts d'Antora.

Slide 6 - Exemple de Playbook
Speech

Voici un exemple simplifié de playbook.

On y retrouve les différents dépôts Git et le dossier dans lequel se trouve la documentation.

C'est ce fichier qui indique à Antora quelles documentations doivent être agrégées.

Si demain nous ajoutons un nouveau composant, il suffit généralement d'ajouter une nouvelle source dans ce playbook.

Slide 7 - Structure recommandée LuPIA
Speech

Pour faciliter la maintenance, il est important d'adopter une structure commune.

Nous avons choisi de centraliser la partie transverse dans ROOT :

Fonctionnel
Architecture
Exploitation

Puis d'avoir des espaces dédiés pour les composants Backend, Frontend et Parser.

Ainsi chacun sait où trouver et où mettre une information.

Slide 8 - Documentation Fonctionnelle
Speech

Cette partie est destinée principalement aux analystes, architectes et développeurs.

On y retrouve :

le contexte métier,
les processus,
les acteurs,
les interfaces,
le modèle de données.

L'objectif n'est pas de remplacer les spécifications détaillées mais de proposer une vision synthétique et facilement navigable.

Slide 9 - Documentation Technique
Speech

Cette partie est davantage orientée implémentation.

On y retrouve :

les diagrammes d'architecture,
les flux,
les APIs,
les mécanismes de déploiement,
les aspects d'exploitation.

Cette documentation est particulièrement utile lors de l'onboarding de nouveaux développeurs.

Slide 10 - Démonstration Développeur
Speech

Nous allons maintenant regarder comment un développeur contribue à la documentation.

Le scénario est simple :

Nous ajoutons une nouvelle fonctionnalité.

Nous créons une page AsciiDoc, nous mettons à jour la navigation, puis nous générons le site.

Exactement comme pour le code, les modifications sont versionnées dans Git.

Slide 11 - Exemple AsciiDoc
Speech

Voici un exemple très simple de page AsciiDoc.

La syntaxe est volontairement légère et facile à lire.

Elle permet d'écrire du texte, d'insérer des images, des tableaux, du code ou des diagrammes.

Même sans être expert, un développeur peut rapidement produire ou mettre à jour une documentation.

Slide 12 - Workflow Git → Antora
Speech

Cette slide résume le workflow complet.

Un développeur modifie sa documentation.

Les changements sont commités et passent dans une Merge Request.

Le pipeline génère ensuite le site Antora.

Le site est alors publié et immédiatement accessible à tous.

On garde ainsi la documentation synchronisée avec les développements.

Slide 13 - Publication Locale
Speech

Avant même de pousser ses changements, un développeur peut générer le site localement.

Cela lui permet de vérifier immédiatement le rendu de sa documentation.

En quelques secondes il dispose d'une prévisualisation complète du site.

C'est très pratique pour corriger les erreurs ou ajuster le contenu avant la Merge Request.

Slide 14 - Bénéfices
Speech

Les bénéfices sont nombreux :

documentation toujours à jour,
historique complet dans Git,
contribution simplifiée,
meilleure collaboration entre équipes,
et surtout une source documentaire unique et centralisée.

C'est généralement ce qui manque le plus dans les projets de longue durée.

Slide 15 - Conclusion
Speech

Pour conclure, Antora n'est pas seulement un générateur de documentation.

C'est avant tout une façon de traiter la documentation comme un véritable artefact du projet.

L'objectif est que chaque fonctionnalité soit livrée avec son code, ses tests et sa documentation.

En d'autres termes :

une fonctionnalité n'est terminée que lorsque sa documentation est à jour.

Merci pour votre attention. Place maintenant à la démonstration. 🚀

💡 Je te conseille de faire une démo live de 5 minutes après la slide 10 :

Création d'un fichier replay-flow.adoc
Ajout dans nav.adoc
npm run build:dev:fetch
Rafraîchissement du navigateur

C'est généralement la partie qui convainc le plus les développeurs.
