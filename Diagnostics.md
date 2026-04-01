C'est très probablement **la cause racine** de tous les échecs depuis le début.

`CachingMetadataReaderFactory` résout les classes via leur **nom binaire**. Pour une classe interne, le nom binaire utilise `$` :

```
// Ce que fixture.getName() retourne pour une classe interne :
com.company.starter.mapstruct.filter.MapstructTypeFilterTest$MapperAnnotatedOnly

// Le fichier .class correspondant :
MapstructTypeFilterTest$MapperAnnotatedOnly.class
```

Le reader ASM tente de lire ce `.class` — mais selon le classloader utilisé, **il peut lire le `.class` de la classe englobante** (`MapstructTypeFilterTest.class`) et non celui de la classe interne, ce qui explique que les annotations et interfaces lues sont incorrectes ou vides.

**La solution est de sortir les fixtures maintenant.** Lance d'abord le test de diagnostic que je t'ai fourni pour confirmer, puis crée les fichiers séparés exactement comme suit :

---

### `fixture/MapperAnnotatedOnly.java`
```java
package com.company.starter.mapstruct.filter.fixture;

import org.mapstruct.Mapper;

@Mapper
public interface MapperAnnotatedOnly {}
```

### `fixture/ConcreteMapperAnnotated.java`
```java
package com.company.starter.mapstruct.filter.fixture;

import org.mapstruct.Mapper;

@Mapper
public class ConcreteMapperAnnotated {}
```

### `fixture/ParentAnnotatedMapper.java`
```java
package com.company.starter.mapstruct.filter.fixture;

import org.mapstruct.Mapper;

@Mapper
public interface ParentAnnotatedMapper {}
```

### `fixture/ChildOfAnnotatedMapper.java`
```java
package com.company.starter.mapstruct.filter.fixture;

public interface ChildOfAnnotatedMapper extends ParentAnnotatedMapper {}
```

### `fixture/ValidMapstructMapperInterface.java`
```java
package com.company.starter.mapstruct.filter.fixture;

import com.company.starter.mapstruct.MapstructMapper;

public interface ValidMapstructMapperInterface extends MapstructMapper {}
```

### `fixture/MapperAnnotatedAndInterface.java`
```java
package com.company.starter.mapstruct.filter.fixture;

import com.company.starter.mapstruct.MapstructMapper;
import org.mapstruct.Mapper;

@Mapper
public interface MapperAnnotatedAndInterface extends MapstructMapper {}
```

### `fixture/RandomInterface.java`
```java
package com.company.starter.mapstruct.filter.fixture;

public interface RandomInterface {}
```

### `fixture/RandomClass.java`
```java
package com.company.starter.mapstruct.filter.fixture;

public class RandomClass {}
```

### `fixture/ClassWithUnrelatedAnnotation.java`
```java
package com.company.starter.mapstruct.filter.fixture;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.CLASS)
@interface UnrelatedAnnotation {}

@UnrelatedAnnotation
public class ClassWithUnrelatedAnnotation {}
```

---

### `MapstructTypeFilterTest.java` — imports à mettre à jour

```java
import com.company.starter.mapstruct.filter.fixture.ChildOfAnnotatedMapper;
import com.company.starter.mapstruct.filter.fixture.ClassWithUnrelatedAnnotation;
import com.company.starter.mapstruct.filter.fixture.ConcreteMapperAnnotated;
import com.company.starter.mapstruct.filter.fixture.MapperAnnotatedAndInterface;
import com.company.starter.mapstruct.filter.fixture.MapperAnnotatedOnly;
import com.company.starter.mapstruct.filter.fixture.RandomClass;
import com.company.starter.mapstruct.filter.fixture.RandomInterface;
import com.company.starter.mapstruct.filter.fixture.ValidMapstructMapperInterface;
```

Supprimer toutes les classes internes du fichier de test. Le `setUp()` reste identique :

```java
@BeforeEach
void setUp() {
    filter = new MapstructTypeFilter();
    readerFactory = new CachingMetadataReaderFactory(
            MapstructTypeFilterTest.class.getClassLoader()
    );
}
```

---

### Structure finale attendue

```
src/test/java/com/company/starter/mapstruct/
└── filter/
    ├── MapstructTypeFilterTest.java
    └── fixture/
        ├── ValidMapstructMapperInterface.java
        ├── MapperAnnotatedOnly.java
        ├── MapperAnnotatedAndInterface.java
        ├── ConcreteMapperAnnotated.java
        ├── ParentAnnotatedMapper.java
        ├── ChildOfAnnotatedMapper.java
        ├── RandomInterface.java
        ├── RandomClass.java
        └── ClassWithUnrelatedAnnotation.java
```

Une fois les fichiers créés, **fais un `mvn clean test`** (et non juste `test`) pour forcer la recompilation de toutes les fixtures — les `.class` précédents des classes internes pourraient encore traîner dans `target/`.




Le diagnostic est sans ambiguïté : **`Annotations: []` sur toutes les fixtures**, même `MapperAnnotatedOnly` qui porte `@Mapper` directement.

Cela confirme que le problème n'est **pas dans `MapstructTypeFilter`** — c'est un problème de **lecture ASM des classes internes**. Le reader lit bien la bonne classe (on voit `isInterface` correct et les interfaces correctes), mais **les annotations des classes internes ne sont pas visibles via ASM** dans ce contexte.

La solution est donc bien de sortir les fixtures. Fais un **`mvn clean compile test-compile`** après avoir créé les fichiers séparés, puis relance le diagnostic. Le résultat attendu sera :

```
MapperAnnotatedOnly:
* isInterface: true
* Annotations: [org.mapstruct.Mapper]
* hasAnnotation(@Mapper): true

ConcreteMapperAnnotated:
* isInterface: false  
* Annotations: [org.mapstruct.Mapper]
* hasAnnotation(@Mapper): true

ChildOfAnnotatedMapper:
* isInterface: true
* Annotations: []
* Interfaces: [com.company.starter.mapstruct.filter.fixture.ParentAnnotatedMapper]
* hasAnnotation(@Mapper): false  ← normal, c'est le parent qui porte @Mapper
```

Ce dernier point est important : pour `ChildOfAnnotatedMapper`, `hasAnnotation` sera toujours `false` — c'est normal et attendu. C'est la méthode `hasMappperAnnotationOnSuperInterfaces()` dans `MapstructTypeFilter` qui doit prendre le relais en lisant `ParentAnnotatedMapper` et en y trouvant `@Mapper`.

**Partage le nouveau résultat du diagnostic après le `mvn clean`** — on validera que les annotations sont bien lues avant de toucher quoi que ce soit d'autre dans le filtre.
