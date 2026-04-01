Parfait. Avec des fichiers séparés et un `mvn clean` fait, si le diagnostic donne encore `Annotations: []`, cela pointe vers **une seule cause possible** : `@Retention(RetentionPolicy.CLASS)` sur `@org.mapstruct.Mapper`.

Vérifions cela immédiatement. Ajoute ce test :

```java
@Test
@DisplayName("DIAGNOSTIC 3 — vérifie la rétention de @Mapper")
void diagnostic3() {
    Retention retention = org.mapstruct.Mapper.class
            .getAnnotation(Retention.class);

    System.out.println("@Mapper @Retention : " + 
        (retention != null ? retention.value() : "non trouvée (défaut = CLASS)"));

    // Vérifie si @Mapper est visible par réflexion (RUNTIME uniquement)
    System.out.println("@Mapper visible par réflexion : " + 
        ConcreteMapperAnnotated.class.isAnnotationPresent(org.mapstruct.Mapper.class));
}
```

**Si le résultat est :**
```
@Mapper @Retention : CLASS
@Mapper visible par réflexion : false
```

Alors le problème est confirmé : **`CachingMetadataReaderFactory` dans la version de Spring Core que tu utilises ne lit pas les annotations `@Retention(CLASS)` via ASM**. Ce comportement varie selon la version — certaines versions de Spring lisent toutes les annotations bytecode, d'autres filtrent sur `RUNTIME`.

Dans ce cas la solution est de **ne plus utiliser `AnnotationMetadata.hasAnnotation()`** mais de lire le bytecode directement via `ClassVisitor` ASM. Ajoute la dépendance ASM si elle n'est pas déjà transitive via Spring :

```xml
<!-- Normalement déjà transitif via spring-core -->
<dependency>
    <groupId>org.ow2.asm</groupId>
    <artifactId>asm</artifactId>
    <scope>test</scope>
</dependency>
```

Et remplace `MapstructTypeFilter` par cette implémentation qui lit le bytecode ASM **directement** sans passer par `AnnotationMetadata` :

```java
package com.company.starter.mapstruct.filter;

import com.company.starter.mapstruct.MapstructMapper;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.InterfaceTypeFilter;
import org.springframework.core.type.filter.TypeFilter;

import java.io.IOException;
import java.io.InputStream;

public class MapstructTypeFilter implements TypeFilter {

    // Descripteur ASM de @Mapper — format interne JVM
    private static final String MAPPER_DESCRIPTOR = "Lorg/mapstruct/Mapper;";

    private final TypeFilter byInterface = new InterfaceTypeFilter(MapstructMapper.class);

    @Override
    public boolean match(MetadataReader metadataReader,
                         MetadataReaderFactory metadataReaderFactory) throws IOException {
        return byInterface.match(metadataReader, metadataReaderFactory)
            || hasMapperAnnotationInBytecode(metadataReader, metadataReaderFactory);
    }

    /**
     * Lit directement le bytecode via ASM — fonctionne quelle que soit
     * la @Retention de l'annotation (@CLASS ou @RUNTIME).
     */
    private boolean hasMapperAnnotationInBytecode(MetadataReader metadataReader,
                                                   MetadataReaderFactory metadataReaderFactory)
            throws IOException {

        // Cas 1 : annotation directe sur la classe/interface
        if (hasMapperAnnotation(metadataReader.getResource().getInputStream())) {
            return true;
        }

        // Cas 2 : remonte les super-interfaces récursivement
        return hasMapperAnnotationOnSuperInterfaces(
                metadataReader.getClassMetadata().getInterfaceNames(),
                metadataReaderFactory
        );
    }

    /**
     * Lit le bytecode via un ClassVisitor ASM et cherche MAPPER_DESCRIPTOR
     * dans toutes les annotations (visible=true ET visible=false).
     */
    private boolean hasMapperAnnotation(InputStream classInputStream) throws IOException {
        try (InputStream is = classInputStream) {
            MapperAnnotationDetector detector = new MapperAnnotationDetector();
            new ClassReader(is).accept(detector, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
            return detector.isMapperFound();
        }
    }

    private boolean hasMapperAnnotationOnSuperInterfaces(String[] interfaceNames,
                                                          MetadataReaderFactory factory)
            throws IOException {
        for (String interfaceName : interfaceNames) {
            try {
                MetadataReader ifaceReader = factory.getMetadataReader(interfaceName);

                if (hasMapperAnnotation(ifaceReader.getResource().getInputStream())) {
                    return true;
                }

                String[] superInterfaces = ifaceReader.getClassMetadata().getInterfaceNames();
                if (superInterfaces.length > 0
                        && hasMapperAnnotationOnSuperInterfaces(superInterfaces, factory)) {
                    return true;
                }
            } catch (IOException ignored) {
                // Interface non trouvée dans le classpath (ex: java.io.Serializable)
            }
        }
        return false;
    }

    /**
     * ClassVisitor ASM minimal — s'arrête dès que @Mapper est trouvé.
     * Visite les annotations visible=false (@Retention CLASS)
     * ET visible=true (@Retention RUNTIME).
     */
    private static class MapperAnnotationDetector extends ClassVisitor {

        private boolean mapperFound = false;

        MapperAnnotationDetector() {
            super(Opcodes.ASM9);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (MAPPER_DESCRIPTOR.equals(descriptor)) {
                mapperFound = true;
            }
            return null; // pas besoin de visiter le contenu de l'annotation
        }

        boolean isMapperFound() {
            return mapperFound;
        }
    }
}
```

---

### Pourquoi ça fonctionne

| Mécanisme | `@Retention(CLASS)` | `@Retention(RUNTIME)` |
|---|---|---|
| Réflexion Java | ❌ invisible | ✅ visible |
| `AnnotationMetadata.hasAnnotation()` Spring | ⚠️ selon version | ✅ visible |
| `ClassVisitor.visitAnnotation()` ASM direct | ✅ **toujours visible** | ✅ visible |

`visitAnnotation` est appelé pour **toutes** les annotations présentes dans le bytecode, qu'elles soient `visible=true` (RUNTIME) ou `visible=false` (CLASS) — c'est la distinction que Spring `AnnotationMetadata` ne gère pas de façon fiable selon la version.
