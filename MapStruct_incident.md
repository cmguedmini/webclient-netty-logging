## 📝 Récapitulatif : Erreur d'Instanciation MapStruct avec Spring 6

### 🛑 Cause de l'Erreur

L'erreur est due à un changement de **rigueur** dans la manière dont **Spring Framework 6 (utilisé par Spring Boot 3)** valide les **méthodes de fabrique (Factory Methods)** lors du traitement des `BeanDefinition` par votre `BeanFactoryPostProcessor` (BFP).

| Élément | Comportement |
| :--- | :--- |
| **Erreur observée** | `No matching factory method found on class WorldlineMapper: factory method 'build(Class)'` |
| **Votre Code (BFP)** | `BeanDefinitionBuilder.genericBeanDefinition(mapperClass).setFactoryMethod("build")...` |
| **Problème de fond** | Votre code disait à Spring : "Pour créer un *bean* de type `mapperClass` (l'interface MapStruct, ex: `WorldlineMapper`), utilise la méthode de fabrique **statique** `build` **sur cette même classe** (`WorldlineMapper.class`)." |
| **Conséquence Spring 6** | Spring 6 vérifie l'existence de la méthode `public static <T> T build(Class<T>)` sur l'interface `WorldlineMapper` et, ne la trouvant pas (car elle est dans votre `MapStructHelper`), l'instanciation échoue. |
| **Ancien comportement (Spring 5.x)** | Spring 5.x était plus tolérant, ou la résolution de la méthode était reportée, ce qui permettait au Helper de fonctionner. |

-----

### ✅ Solution Finale

La solution consiste à modifier la `BeanDefinition` dans votre `BeanFactoryPostProcessor` pour indiquer clairement à Spring que la **classe de l'usine (Factory Class)** est votre `MapStructHelper`, tandis que le **type de *bean* retourné** est l'interface MapStruct.

#### Ancien Code (Problématique)

```java
// Spring pense que 'mapperClass' est l'usine
BeanDefinitionBuilder.genericBeanDefinition(mapperClass) 
    .setFactoryMethod("build")
    .addConstructorArgValue(mapperClass); 
```

#### Nouveau Code (Corrigé pour Spring 6)

Ce code suppose que `build(Class)` est une méthode **statique** dans votre `MapStructHelper`.

```java
// Spring sait que 'MapStructHelper.class' est l'usine, mais produit un bean de type 'mapperClass'
BeanDefinitionBuilder beanBuilder = BeanDefinitionBuilder
    // 1. Définir la classe de l'usine (où se trouve la méthode statique 'build')
    .genericBeanDefinition(MapStructHelper.class) 
    
    // 2. Indiquer la méthode de fabrique statique
    .setFactoryMethod("build")           
    
    // 3. Spécifier le type de bean injecté/final produit
    .setTargetType(mapperClass) 
    
    // 4. Ajouter l'argument nécessaire à la méthode 'build(Class)'
    .addConstructorArgValue(mapperClass); 

beanFactory.registerBeanDefinition(mapperClass.getSimpleName(), beanBuilder.getBeanDefinition());
```

**Conclusion :** La correction garantit que Spring 6 recherche la méthode de fabrique **statique** `build` au bon endroit (`MapStructHelper`) tout en conservant le type de *bean* final attendu (`WorldlineMapper`).
