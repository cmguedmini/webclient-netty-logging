# WebClient Logging Test avec Wiretap

Ce projet Spring Boot 3 démontre l'utilisation du **wiretap** pour capturer les logs détaillés des requêtes et réponses WebClient, y compris en cas d'exceptions et de timeouts.

## 🎯 Objectif

Tester la génération des logs WebClient avec wiretap activé pour s'assurer de pouvoir récupérer :
- Headers des requêtes entrantes et sortantes
- Body des requêtes et réponses
- Logs même en cas d'exception ou timeout

## 🏗️ Architecture

### Configuration WebClient
- **Wiretap activé** avec `AdvancedByteBufFormat.TEXTUAL`
- Configuration des timeouts (connect, read, write)
- Logs détaillés au niveau DEBUG

### Endpoints de Test
- `GET/POST /api/test/success` - Réponse normale
- `GET/POST /api/test/error` - Déclenche une RuntimeException
- `GET/POST /api/test/timeout` - Simule un timeout (sleep 10s)

### Tests JUnit
- **testSuccessEndpoint** - Test de succès GET/POST
- **testErrorEndpoint** - Test d'exception RuntimeException
- **testTimeoutEndpoint** - Test de timeout

## 🚀 Exécution

### Prérequis
- Java 17+
- Maven 3.6+

### Lancer les tests
```bash
mvn clean test
```

### Lancer l'application
```bash
mvn spring-boot:run
```

## 📋 Tests Disponibles

### 1. Test Success (GET/POST)
- Appel réussi avec headers personnalisés
- Vérification de la réponse JSON
- Logs des requêtes/réponses complets

### 2. Test RuntimeException (GET/POST)
- Simulation d'erreur serveur (500)
- Capture des logs même en cas d'erreur
- Vérification de la gestion d'exception

### 3. Test Timeout (GET/POST)
- Timeout configuré à 2 secondes
- Serveur répond après 10 secondes
- Logs de la requête envoyée malgré le timeout

## 🔍 Logs Attendus

Avec wiretap activé, vous devriez voir dans les logs :

```
DEBUG reactor.netty.http.client.HttpClient - [id: 0x...] REGISTERED
DEBUG reactor.netty.http.client.HttpClient - [id: 0x...] CONNECT: localhost/127.0.0.1:8080
DEBUG reactor.netty.http.client.HttpClient - [id: 0x...] WRITE: 
GET /api/test/success HTTP/1.1
X-Test-Header: success-test
X-Request-ID: req-1234567890
Host: localhost:8080
```

## ⚙️ Configuration

### application.properties
```properties
# Activation des logs WebClient
logging.level.reactor.netty.http.client=DEBUG
logging.level.reactor.netty.http.server=DEBUG
logging.level.org.springframework.web.reactive.function.client=DEBUG

# Configuration timeouts
webclient.timeout.connect=5000
webclient.timeout.read=3000
```

### WebClientConfiguration
- HttpClient avec wiretap configuré
- Timeouts personnalisables
- Format de log textuel pour lisibilité

## 🧪 Scénarios de Test

1. **Success Flow** : Requête → Response 200 → Logs complets
2. **Error Flow** : Requête → Exception 500 → Logs + Exception
3. **Timeout Flow** : Requête → Timeout → Logs + TimeoutException

## 📊 Vérifications

- ✅ Headers de requête loggés
- ✅ Body de requête loggé (POST)
- ✅ Réponse complète loggée
- ✅ Exceptions capturées et loggées
- ✅ Timeouts gérés avec logs

## 🔧 Personnalisation

Pour modifier les timeouts :
```properties
webclient.timeout.connect=3000
webclient.timeout.read=2000
```

Pour changer le niveau de log :
```properties
logging.level.reactor.netty=TRACE
```