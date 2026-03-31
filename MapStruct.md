Le problème est clair. `InterfaceTypeFilter` ajoute une **double contrainte** :

```
isInterface() == true  AND  AssignableTypeFilter.match() == true
```

Donc :
- `ConcreteMapperAnnotated` → **rejetée** par `byInterface` (pas une interface) ✓ normal, mais `byAnnotation` aurait dû suffire → le problème vient d'ailleurs
- `MapperAnnotatedOnly` → **rejetée** par `byInterface` (n'implémente pas `MapstructMapper`), et `byAnnotation` devrait matcher… 

En relisant `AnnotationTypeFilter` avec `checkInherited=true` : pour les **interfaces**, Spring utilise `getInterfaces()` et non `getSuperclass()`, ce qui peut rater la détection selon la version. Mais surtout, le vrai problème pour `ChildOfAnnotatedMapper` et les autres est que **`byInterface` retourne `false`** et que **`byAnnotation` ne remonte pas correctement** dans certains cas avec le reader ASM.

Voici les corrections :

---

### Correction 1 — `MapstructTypeFilter.java`

```java
public class MapstructTypeFilter implements TypeFilter {

    private final TypeFilter byInterface = new InterfaceTypeFilter(MapstructMapper.class);

    // checkInherited=false pour les interfaces : AnnotationTypeFilter
    // utilise getSuperclass() pour remonter la hiérarchie, ce qui ne
    // fonctionne pas pour les interfaces (getSuperclass() retourne null).
    // On délègue la détection par héritage à byInterfaceInherited ci-dessous.
    private final TypeFilter byAnnotation = new AnnotationTypeFilter(
            Mapper.class,
            false,  // checkInherited — voir explication ci-dessus
            true    // considerMetaAnnotations
    );

    // Détecte les interfaces qui héritent d'une interface annotée @Mapper.
    // AssignableTypeFilter (sans la contrainte isInterface) remonte
    // correctement toute la hiérarchie via le bytecode ASM.
    private final TypeFilter byInterfaceInherited = new AssignableTypeFilter(Mapper.class) {
        @Override
        public boolean match(MetadataReader metadataReader,
                             MetadataReaderFactory metadataReaderFactory) throws IOException {
            // On vérifie uniquement l'annotation directe sur les super-interfaces
            // via la hiérarchie ASM, sans exiger isInterface()
            ClassMetadata meta = metadataReader.getClassMetadata();
            return checkSuperInterfaces(meta.getInterfaceNames(), metadataReaderFactory);
        }

        private boolean checkSuperInterfaces(String[] interfaceNames,
                                             MetadataReaderFactory factory) throws IOException {
            for (String iface : interfaceNames) {
                try {
                    MetadataReader ifaceReader = factory.getMetadataReader(iface);
                    AnnotationMetadata ifaceMeta = ifaceReader.getAnnotationMetadata();
                    if (ifaceMeta.hasAnnotation(Mapper.class.getName())) {
                        return true;
                    }
                    // Récursion sur les super-interfaces
                    String[] superIfaces = ifaceReader.getClassMetadata().getInterfaceNames();
                    if (superIfaces.length > 0
                            && checkSuperInterfaces(superIfaces, factory)) {
                        return true;
                    }
                } catch (IOException ignored) {
                    // Interface non trouvée dans le classpath (ex: JDK), on ignore
                }
            }
            return false;
        }
    };

    @Override
    public boolean match(MetadataReader metadataReader,
                         MetadataReaderFactory metadataReaderFactory) throws IOException {
        return byInterface.match(metadataReader, metadataReaderFactory)
            || byAnnotation.match(metadataReader, metadataReaderFactory)
            || byInterfaceInherited.match(metadataReader, metadataReaderFactory);
    }
}
```

---

### Correction 2 — Les tests

Les trois tests qui échouaient + le test d'héritage nécessitent des ajustements dans les fixtures et les assertions :

```java
@Nested
@DisplayName("Cas positifs — le filtre doit accepter")
class ShouldMatch {

    @Test
    @DisplayName("une interface qui implémente MapstructMapper")
    void interfaceImplementingMapstructMapper() throws Exception {
        assertThat(matches(ValidMapstructMapperInterface.class)).isTrue();
    }

    @Test
    @DisplayName("une interface annotée @Mapper uniquement (sans MapstructMapper)")
    void interfaceAnnotatedWithMapperOnly() throws Exception {
        // byAnnotation avec checkInherited=false détecte l'annotation directe
        assertThat(matches(MapperAnnotatedOnly.class)).isTrue();
    }

    @Test
    @DisplayName("une interface qui implémente MapstructMapper ET porte @Mapper")
    void interfaceWithBothConditions() throws Exception {
        assertThat(matches(MapperAnnotatedAndInterface.class)).isTrue();
    }

    @Test
    @DisplayName("une classe concrète annotée @Mapper")
    void concreteClassAnnotatedWithMapper() throws Exception {
        // byAnnotation (checkInherited=false) détecte @Mapper directement sur la classe
        // byInterface échoue (pas une interface) — c'est le OR qui sauve ce cas
        assertThat(matches(ConcreteMapperAnnotated.class)).isTrue();
    }

    @Test
    @DisplayName("une interface héritant d'une interface annotée @Mapper")
    void interfaceInheritingFromAnnotatedParent() throws Exception {
        // byAnnotation avec checkInherited=false ne remonte pas les interfaces parentes
        // → c'est byInterfaceInherited qui prend le relais
        assertThat(matches(ChildOfAnnotatedMapper.class)).isTrue();
    }
}
```

---

### Tableau récapitulatif des filtres après correction

| Fixture | `byInterface` | `byAnnotation` | `byInterfaceInherited` | Résultat |
|---|---|---|---|---|
| `ValidMapstructMapperInterface` | ✅ | ❌ | ❌ | ✅ |
| `MapperAnnotatedOnly` | ❌ | ✅ | ❌ | ✅ |
| `MapperAnnotatedAndInterface` | ✅ | ✅ | ❌ | ✅ |
| `ConcreteMapperAnnotated` | ❌ (pas interface) | ✅ | ❌ | ✅ |
| `ChildOfAnnotatedMapper` | ❌ | ❌ (`checkInherited=false`) | ✅ | ✅ |
| `RandomInterface` | ❌ | ❌ | ❌ | ❌ |
| `RandomClass` | ❌ | ❌ | ❌ | ❌ |

La racine du problème était que `AnnotationTypeFilter(checkInherited=true)` remonte via `getSuperclass()` — ce qui fonctionne pour les classes mais **retourne `null` pour les interfaces** en bytecode ASM. Le troisième filtre `byInterfaceInherited` comble ce gap en parcourant explicitement `getInterfaceNames()` de façon récursive.
