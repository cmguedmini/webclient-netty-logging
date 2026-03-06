Voici comment on peut rédiger un paragraphe clair et professionnel en **Asciidoc** pour la documentation destinée aux équipes de développement :  

***

[idc-configuration]
=== Configuration des attributs `idc` dans `application.yml`

L’espace de configuration `idc` permet de centraliser des métadonnées techniques sur l’environnement d’exécution de l’application (type de déploiement, instance, namespace, etc.). Ces paramètres sont automatiquement injectés au démarrage selon le contexte d’exécution (Kubernetes, serveur physique ou batch).  

.Exemple de configuration
[source,yaml]
----
idc:
  environment: test1
  leg: ${idc.it.instance}
  it:
    application-name: ${APP_NAME}
    environment: ...
    node: ${NODE_NAME}
    namespace: ${POD_NAMESPACE}
    cmdbname: ${POD_NAMESPACE}.${APP_NAME}
    tier: ...
    ecosystem: ...
    instance: ${POD_NAME}
----

.Règles d’affectation des attributs
- `leg` : référence l’attribut `instance`.  
  - Sur Kubernetes → correspond au *nom du pod* (`POD_NAME`)  
  - Sur serveur physique → correspond au *numéro d’instance*  
  - Pour un batch → valeur composée de la concaténation `run_id + job_name`  
- `node` :  
  - Sur Kubernetes → valeur de `NODE_NAME`  
  - Sur serveur physique → adresse IP du serveur  
- `namespace` :  
  - Sur Kubernetes → valeur de `POD_NAMESPACE`  
  - Sur serveur physique → nom d’hôte (*hostname*)  
- `cmdbname` : construit dynamiquement sous la forme `${namespace}.${application-name}`  

Cette configuration assure une identification homogène des applications et de leurs instances quel que soit leur mode de déploiement, facilitant ainsi la supervision, le diagnostic et le traçage inter-environnements.

***

Souhaitez-vous que je reformate ce paragraphe pour qu’il s’intègre dans une documentation AsciiDoc plus large (ex. sous un guide "Configuration du framework") ?
