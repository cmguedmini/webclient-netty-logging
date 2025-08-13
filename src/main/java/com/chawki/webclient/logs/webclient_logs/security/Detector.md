Voici plusieurs façons de détecter les instances `AuthenticationManager` dans votre contexte Spring :## Comment utiliser ces outils

### 1. **Détection automatique au démarrage**
Les composants s'exécutent automatiquement et loggent les résultats dans la console :

```bash
=== DETECTING AUTHENTICATION MANAGERS ===
1. Searching for AuthenticationManager beans...
  Found: authenticationManager (MultiProviderAuthenticationManager)
2. Searching in Security Configurations...
  Found: authenticationManager in SecurityConfig.securityConfig (MultiProviderAuthenticationManager)
=== DETECTION COMPLETE - Found 2 AuthenticationManager instances ===
```

### 2. **Inspection via API REST**
Vous pouvez interroger via des endpoints REST :

```bash
# Détecter les AuthenticationManager
GET /api/debug/auth-managers/detect

# Analyser en détail
GET /api/debug/auth-managers/analyze

# Résumé rapide
GET /api/debug/auth-managers/summary
```

### 3. **Inspection programmatique**
Dans votre code, vous pouvez injecter le détecteur :

```java
@Autowired
private AuthenticationManagerDetector detector;

public void checkAuthManagers() {
    detector.detectAuthenticationManagers();
    List<AuthManagerInfo> managers = detector.getDetectedManagers();
    
    for (AuthManagerInfo info : managers) {
        System.out.println("Found: " + info.getName() + " (" + info.getType() + ")");
    }
}
```

### 4. **Configuration pour activer les logs**
Ajoutez dans votre `application.yml` :

```yaml
logging:
  level:
    # Vos packages pour voir les détections
    your.package.name: DEBUG
    # Pour voir les détails Spring Security (optionnel)
    org.springframework.security: DEBUG
```

Ces outils vous permettront de :
- ✅ Identifier tous les `AuthenticationManager` dans votre contexte
- ✅ Voir d'où ils proviennent (beans, configurations, composants)
- ✅ Détecter les doublons ou conflits
- ✅ Analyser leur configuration interne
- ✅ Obtenir des recommandations pour optimiser votre configuration