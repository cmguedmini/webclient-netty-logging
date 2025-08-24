Voici la commande pour installer jackson-coreutils version 2.0 dans votre repository Maven local :

## Commande Maven install:install-file

```bash
mvn install:install-file \
  -Dfile=path/to/jackson-coreutils-2.0.jar \
  -DgroupId=com.github.fge \
  -DartifactId=jackson-coreutils \
  -Dversion=2.0 \
  -Dpackaging=jar
```

## Paramètres expliqués

- `-Dfile=` : Chemin vers le fichier JAR à installer
- `-DgroupId=` : Group ID de la dépendance (pour jackson-coreutils c'est `com.github.fge`)
- `-DartifactId=` : Artifact ID (`jackson-coreutils`)
- `-Dversion=` : Version à installer (`2.0`)
- `-Dpackaging=` : Type de packaging (`jar`)

## Si vous avez aussi le POM

```bash
mvn install:install-file \
  -Dfile=path/to/jackson-coreutils-2.0.jar \
  -DpomFile=path/to/jackson-coreutils-2.0.pom
```

## Si vous avez les sources

```bash
mvn install:install-file \
  -Dfile=path/to/jackson-coreutils-2.0.jar \
  -DgroupId=com.github.fge \
  -DartifactId=jackson-coreutils \
  -Dversion=2.0 \
  -Dpackaging=jar \
  -Dsources=path/to/jackson-coreutils-2.0-sources.jar
```

## Alternative : Téléchargement automatique depuis Maven Central

Si la dépendance existe sur Maven Central, vous pouvez simplement l'ajouter dans votre `pom.xml` :

```xml
<dependency>
    <groupId>com.github.fge</groupId>
    <artifactId>jackson-coreutils</artifactId>
    <version>2.0</version>
</dependency>
```

Puis exécuter :
```bash
mvn dependency:resolve
```

## Vérification de l'installation

Après installation, vous pouvez vérifier avec :

```bash
# Vérifier que la dépendance est dans le repo local
ls ~/.m2/repository/com/github/fge/jackson-coreutils/2.0/

# Ou utiliser la commande dependency
mvn dependency:get -DgroupId=com.github.fge -DartifactId=jackson-coreutils -Dversion=2.0
```

**Note** : Assurez-vous d'avoir le fichier JAR disponible localement avant d'exécuter la commande `install:install-file`.
