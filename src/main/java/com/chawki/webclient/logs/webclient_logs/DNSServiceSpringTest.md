**Points clés des tests ajustés :**

1. **Comportement avec `unless = "#result == null"`** :
   - Un `Optional.empty()` **n'est PAS null**, donc il **SERA mis en cache** ✅
   - Seul un vrai `null` (si la méthode plantait et retournait null) ne serait pas caché
   - Test `testCache_EmptyOptionalIsCached()` confirme ce comportement

2. **Détection du bug ClassCastException** :
   - Test `testCache_OptionalTypeInCache()` : Vérifie explicitement si le cache contient une `String` au lieu d'un `Optional`
   - Si c'est le cas, le test échoue avec un message clair expliquant le bug
   - Test `testHostname_Localhost_WithShortTimeout()` : Inclut un try-catch pour détecter la `ClassCastException` lors de la récupération du cache

3. **Diagnostic complet** :
   - Affiche le type exact de l'objet en cache
   - Détecte si Ehcache a "déballé" l'Optional en String
   - Messages clairs pour identifier le problème

**Ce que les tests vont révéler :**

Si vous avez le bug, vous verrez :
```
Type en cache: java.lang.String
❌ BUG CONFIRMÉ: Le cache contient une String ('localhost') au lieu d'un Optional<String>
```

Si tout fonctionne correctement :
```
Type en cache: java.util.Optional
✓ Validation réussie: Type en cache = Optional<String>
✓ Pas de risque de ClassCastException
```

Les tests sont maintenant alignés avec votre implémentation réelle et détecteront précisément le problème ! 🎯
