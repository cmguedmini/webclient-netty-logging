Guide de Configuration : Écosystème jef.fusta
Ce module utilise un mécanisme de Fail-Fast au démarrage pour garantir que chaque serveur déclaré dans la configuration possède une implémentation valide dans le code.

1. Déclaration d'un Service (@XXXService)
Pour qu'un bean soit reconnu comme un serveur valide, il doit porter l'annotation @XXXService. L'attribut name est le pivot de la liaison.

Java

@Service
@XXXService(name = "order-service") 
public class OrderService implements MyInterface { ... }
2. Enregistrement dans la Configuration
Le système réconcilie les beans avec la liste définie dans votre application.yml.

Cas A : Configuration Manuelle (Multi-serveurs)
Vous listez explicitement les noms des services à activer.

YAML

jef:
  fusta:
    server:
      multiXXXServer:
        - "order-service"
        - "inventory-service"
Cas B : Configuration par défaut (Fallback)
Si la liste multiXXXServer n'est pas mentionnée, le système cherche par défaut un bean dont le nom correspond à votre spring.application.name.

YAML

spring:
  application:
    name: "my-app-default"
# Le système cherchera alors @XXXService(name = "my-app-default")
🚨 Diagnostic des erreurs de démarrage
Si une incohérence est détectée, l'application refuse de démarrer. Grâce au FailureAnalyzer, un bloc d'erreur simplifié s'affiche dans la console :

Exemple de message d'erreur :
Description:
La configuration 'jef.fusta.server.multiXXXServer' contient des noms qui n'existent pas dans le code : [order-service]

Action:

Vérifiez que la classe est annotée avec @XXXService(name = "order-service").

Vérifiez la casse et l'orthographe dans votre fichier .yml.

Assurez-vous que le composant est bien scanné par Spring (présence de @Service ou @Component).

🛠 Flux Logique de Validation
Voici le processus interne de validation exécuté lors de l'initialisation :

Collecte : Récupération de tous les beans annotés @XXXService.

Résolution : Extraction de la liste des noms (multiXXXServer ou spring.application.name).

Comparaison : Vérification que chaque nom configuré possède un bean correspondant.

Fail-Fast : Si un nom manque, levée d'une ServerMappingException interceptée par le FailureAnalyzer.
