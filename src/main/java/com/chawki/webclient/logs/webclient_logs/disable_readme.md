Le problème vient de votre classe `DenyAllSecurityConfiguration` qui utilise probablement `MvcRequestMatcher` ou des méthodes comme `.requestMatchers()` qui nécessitent des composants web.

Dans une application non-web avec Spring Batch, vous devez complètement désactiver cette configuration de sécurité web. Voici la solution ajustée :**Solution complémentaire - Configuration Java pour désactiver `DenyAllSecurityConfiguration` :****Ajout dans application.yml pour contrôler la configuration :****Résumé de la solution :**

1. **application.yml** : Désactive toutes les auto-configurations web et de sécurité web
2. **Modification de `DenyAllSecurityConfiguration`** : Utilisez une des annotations conditionnelles pour qu'elle ne s'active pas en mode batch
3. **Profil batch** : Active le profil 'batch' pour éviter les configurations web

Cette approche empêchera complètement l'instanciation de `SecurityFilterChain` et l'utilisation de `MvcRequestMatcher` qui cause l'exception `HandlerMappingIntrospector`.
