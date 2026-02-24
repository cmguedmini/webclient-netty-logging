Solution avec les ancres YAML (`&shared_include` / `*shared_include`).

Voici pourquoi c’est la meilleure option pour le contexte (migration Spring Boot 2.7 → 3.5.x, rétrocompatibilité, configuration Actuator) :

## Comparaison des solutions

| Solution | Avantages | Inconvénients | Score |
|----------|-----------|---------------|-------|
| **Ancre YAML** (`&shared_include`) | ✅ Liste YAML lisible<br>✅ Pas de duplication<br>✅ Pas de changement de format<br>✅ Fonctionne parfaitement avec Spring Boot | ❌ Spécifique YAML (pas portable vers properties) | **⭐⭐⭐⭐⭐** |
| Comma-separated | ✅ Universel (YAML + properties)<br>✅ Simple | ❌ Moins lisible<br>❌ Perte du format liste | ⭐⭐⭐⭐ |
| Propriété intermédiaire | ✅ Centralisée<br>✅ Flexible | ❌ Plus de propriétés<br>❌ Complexe pour simple cas | ⭐⭐⭐ |

## Pourquoi l’ancre YAML est parfaite :

1. **Maintenir la rétrocompatibilité** : Les configs restent en format liste YAML, comme dans l’ancienne version.
2. **Lisibilité maximale** : Les développeurs voient directement les valeurs sous forme de liste.
3. **Zéro duplication** : Une seule source de vérité.
4. **Spring Boot natif** : Les ancres sont supportées depuis toujours par SnakeYAML + Spring Boot. [docs.spring](https://docs.spring.io/spring-boot/reference/features/external-config.html)
5. **Migration propre** : Pas de refactoring massif, juste ajouter `&` et `*`.

## Configuration finale recommandée

```yaml
management:
  httpexchanges:
    recording:
      include: &shared_include
        - PRINCIPAL
        - REMOTE_ADDRESS
        - REQUEST_HEADERS
        # ... vos autres valeurs

  trace:
    http:
      include: *shared_include
```

**C’est élégant, maintenable, et résout le problème à la racine sans contournement technique.** 🎯
