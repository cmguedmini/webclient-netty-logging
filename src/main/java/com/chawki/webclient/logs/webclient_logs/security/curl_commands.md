# Commandes cURL pour Valider les Tests de Sécurité

## Configuration Préalable

Pour tester les endpoints, vous devez d'abord configurer votre `application.yml` ou `application.properties` avec les niveaux de sécurité correspondants :

```yaml
jef:
  security:
    default-security:
      permit: false
      roles: ["USER", "ADMIN"]
    endpoints:
      "/api/public/**":
        permit: true
        roles: []
      "/api/admin/**":
        permit: false
        roles: ["ADMIN"]
      "/api/management/**":
        permit: false
        roles: ["ADMIN", "MANAGER", "SUPERVISOR"]
      "/api/protected/**":
        permit: false
        roles: []
```

## 1. Test Endpoint avec PermitAll (Accès Public)

**Test : Endpoint `/api/public/health` accessible sans authentification**

```bash
curl -X GET http://localhost:8080/api/public/health \
  -H "Content-Type: application/json" \
  -v
```

**Résultat attendu :** 
- Status: `200 OK`
- Réponse JSON avec le statut de santé
- Aucune authentification requise

## 2. Test Endpoint avec Authenticated (Authentification Requise)

**Test : Endpoint `/api/protected` nécessitant une authentification**

### Sans authentification (doit échouer)
```bash
curl -X GET http://localhost:8080/api/protected \
  -H "Content-Type: application/json" \
  -v
```

### Avec authentification Basic Auth
```bash
curl -X GET http://localhost:8080/api/protected \
  -H "Content-Type: application/json" \
  -u "user:user123" \
  -v
```

**Résultats attendus :**
- Sans auth: Status `401 Unauthorized`
- Avec auth: Status `200 OK` + données utilisateur

## 3. Test Endpoint avec Rôle Unique (ADMIN uniquement)

**Test : Endpoint `/api/admin/users` accessible uniquement au rôle ADMIN**

### Avec utilisateur USER (doit échouer)
```bash
curl -X GET http://localhost:8080/api/admin/users \
  -H "Content-Type: application/json" \
  -u "user:user123" \
  -v
```

### Avec utilisateur ADMIN (doit réussir)
```bash
curl -X GET http://localhost:8080/api/admin/users \
  -H "Content-Type: application/json" \
  -u "admin:admin123" \
  -v
```

**Résultats attendus :**
- Utilisateur USER: Status `403 Forbidden`
- Utilisateur ADMIN: Status `200 OK` + liste des utilisateurs

## 4. Test Endpoint avec Multiples Rôles (ADMIN, MANAGER, SUPERVISOR)

**Test : Endpoint `/api/management/report` accessible aux rôles ADMIN, MANAGER, SUPERVISOR**

### Obtenir un token JWT d'abord
```bash
# Connexion pour obtenir le token
TOKEN=$(curl -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  -s | jq -r '.token')
```

### Tester avec le token JWT
```bash
curl -X GET http://localhost:8080/api/management/report \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -v
```

### Test avec un utilisateur non autorisé
```bash
curl -X GET http://localhost:8080/api/management/report \
  -H "Content-Type: application/json" \
  -u "user:user123" \
  -v
```

**Résultats attendus :**
- Avec token ADMIN: Status `200 OK`
- Avec USER: Status `403 Forbidden`

## 5. Test Configuration de Sécurité par Défaut

**Test : Endpoint non configuré spécifiquement (utilise la sécurité par défaut)**

### Créer un endpoint de test non configuré
```bash
# Test d'un endpoint qui n'est pas dans la configuration
curl -X GET http://localhost:8080/api/user/profile \
  -H "Content-Type: application/json" \
  -u "user:user123" \
  -v
```

### Test sans authentification (doit échouer avec la config par défaut)
```bash
curl -X GET http://localhost:8080/api/user/profile \
  -H "Content-Type: application/json" \
  -v
```

**Résultats attendus :**
- Sans auth: Status `401 Unauthorized`
- Avec auth USER: Status `200 OK` (car USER est dans les rôles par défaut)

---

## Script Complet de Test

Voici un script bash complet pour exécuter tous les tests :

```bash
#!/bin/bash

BASE_URL="http://localhost:8080"
echo "=== Tests de Sécurité des Endpoints ==="

echo "1. Test Endpoint Public..."
curl -s -w "Status: %{http_code}\n" -X GET $BASE_URL/api/public/health

echo -e "\n2. Test Endpoint Protégé sans auth..."
curl -s -w "Status: %{http_code}\n" -X GET $BASE_URL/api/protected

echo -e "\n3. Test Endpoint Protégé avec auth..."
curl -s -w "Status: %{http_code}\n" -X GET $BASE_URL/api/protected -u "user:user123"

echo -e "\n4. Test Endpoint Admin avec USER (doit échouer)..."
curl -s -w "Status: %{http_code}\n" -X GET $BASE_URL/api/admin/users -u "user:user123"

echo -e "\n5. Test Endpoint Admin avec ADMIN..."
curl -s -w "Status: %{http_code}\n" -X GET $BASE_URL/api/admin/users -u "admin:admin123"

echo -e "\n=== Tests Terminés ==="
```

## Notes Importantes

1. **Port** : Assurez-vous que votre application fonctionne sur le port 8080
2. **Profil** : Ces tests fonctionnent indépendamment du profil d'environnement
3. **Configuration** : Adaptez les endpoints selon votre configuration JEF Security
4. **Utilisateurs** : Utilisez les utilisateurs définis dans `CustomUserDetailsService`
5. **Headers** : Ajoutez `-v` pour voir les détails des requêtes/réponses