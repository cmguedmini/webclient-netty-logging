Je vais vous aider à résoudre ce problème courant avec les projets multi-modules Maven et Spring Boot.

## Causes possibles du problème

1. **Exécution depuis le mauvais répertoire** : Vous exécutez probablement la commande depuis le répertoire racine (parent) au lieu du module contenant la classe principale.

2. **Configuration manquante dans le plugin Spring Boot** : La propriété `<start-class>` doit être configurée dans le plugin `spring-boot-maven-plugin`, pas seulement comme propriété Maven.

3. **Module par défaut non configuré** : Maven ne sait pas quel module exécuter quand la commande est lancée depuis la racine.

## Solutions

### Solution 1 : Exécuter depuis le bon module
```bash
# Au lieu de :
mvn spring-boot:run -Dspring.profiles.active=local -P local

# Naviguez vers le module contenant votre application :
cd mon-module-application
mvn spring-boot:run -Dspring.profiles.active=local -P local

# Ou depuis la racine, spécifiez le module :
mvn spring-boot:run -pl mon-module-application -Dspring.profiles.active=local -P local
```

### Solution 2 : Configuration correcte du pom.xml du module application

Dans le `pom.xml` de votre module contenant la classe principale :

```xml
<project>
    <!-- ... autres configurations ... -->
    
    <properties>
        <start-class>com.example.MonApplication</start-class>
    </properties>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <mainClass>${start-class}</mainClass>
                    <!-- ou directement -->
                    <!-- <mainClass>com.example.MonApplication</mainClass> -->
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>repackage</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

### Solution 3 : Configuration dans le pom parent (alternative)

Si vous voulez pouvoir exécuter depuis la racine, configurez le pom parent :

```xml
<project>
    <!-- pom parent -->
    <packaging>pom</packaging>
    
    <modules>
        <module>module-core</module>
        <module>module-web</module> <!-- module avec la classe principale -->
        <module>module-data</module>
    </modules>
    
    <properties>
        <!-- Spécifiez le module par défaut -->
        <spring-boot.run.mainClass>com.example.MonApplication</spring-boot.run.mainClass>
    </properties>
    
    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <version>${spring-boot.version}</version>
                    <configuration>
                        <mainClass>${spring-boot.run.mainClass}</mainClass>
                    </configuration>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

### Solution 4 : Utilisation des profils Maven

Si vous utilisez des profils, assurez-vous que la configuration est correcte :

```xml
<profiles>
    <profile>
        <id>local</id>
        <properties>
            <spring.profiles.active>local</spring.profiles.active>
            <start-class>com.example.MonApplication</start-class>
        </properties>
        <build>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <configuration>
                        <mainClass>${start-class}</mainClass>
                    </configuration>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

## Vérifications à faire

1. **Vérifiez que votre classe principale existe** et contient `@SpringBootApplication`
2. **Confirmez le chemin complet** de votre classe principale (package + nom de classe)
3. **Assurez-vous que le module** contenant la classe principale a bien le plugin Spring Boot configuré
4. **Vérifiez la structure** de votre projet multi-modules

## Commande de diagnostic

Pour identifier quel module contient votre application :
```bash
find . -name "*.java" -exec grep -l "@SpringBootApplication" {} \;
```

La solution la plus courante est d'exécuter la commande depuis le bon module ou d'utiliser l'option `-pl` pour spécifier le module cible.
