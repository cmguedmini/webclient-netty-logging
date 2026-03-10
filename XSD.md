## Analyse du problème

Le problème vient du fait que `classpath:src/main/resources/global.xjb` n'est **pas** la bonne façon de référencer un fichier depuis un autre module Maven. Le plugin `jaxb2-maven-plugin` ne résout pas les chemins `classpath:` de cette manière — il cherche physiquement le fichier sur le système de fichiers du projet courant.

## Solutions

### ✅ Solution 1 : Utiliser le mécanisme de `remote-resources-plugin` (recommandée)

Packager le `global.xjb` dans le JAR de l'utilitaire, puis l'extraire avant la génération.

**Dans le module utilitaire — `pom.xml` :**
```xml
<build>
    <resources>
        <resource>
            <directory>src/main/resources</directory>
            <!-- Le .xjb sera inclus dans le JAR -->
        </resource>
    </resources>
</build>
```

**Dans le pom parent — déclarer la dépendance + extraction :**
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-dependency-plugin</artifactId>
    <executions>
        <execution>
            <id>unpack-global-xjb</id>
            <phase>generate-sources</phase>
            <goals>
                <goal>unpack</goal>
            </goals>
            <configuration>
                <artifactItems>
                    <artifactItem>
                        <groupId>com.exemple</groupId>
                        <artifactId>votre-utilitaire</artifactId>
                        <version>${utilitaire.version}</version>
                        <type>jar</type>
                        <overWrite>true</overWrite>
                        <outputDirectory>${project.build.directory}/xjb</outputDirectory>
                        <includes>global.xjb</includes>
                    </artifactItem>
                </artifactItems>
            </configuration>
        </execution>
    </executions>
</plugin>

<!-- Ensuite le plugin jaxb2 référence le fichier extrait -->
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>jaxb2-maven-plugin</artifactId>
    <version>4.1</version>
    <configuration>
        <xjbSources>
            <xjbSource>${project.build.directory}/xjb/global.xjb</xjbSource>
        </xjbSources>
    </configuration>
</plugin>
```

---

### ✅ Solution 2 : Partager via un module de ressources partagées

Créer un module Maven dédié (`shared-resources`) contenant uniquement le `global.xjb`, et l'inclure comme dépendance avec le même mécanisme `unpack` ci-dessus.

---

### ❌ Pourquoi `classpath:src/main/resources/global.xjb` ne fonctionne pas

| Raison | Détail |
|---|---|
| Le plugin résout les chemins en **filesystem** | Il appelle `new File(path).exists()` — pas de résolution classpath |
| `src/main/resources` n'est pas dans le classpath | C'est `target/classes` qui l'est, après compilation |
| Le JAR de l'utilitaire n'est pas décompressé | Maven ne rend pas les ressources des dépendances accessibles directement |

---

## Recommandation

La **Solution 1** avec `maven-dependency-plugin:unpack` est la plus propre et maintenable. Elle garantit que la version du `global.xjb` est alignée avec celle de l'utilitaire déclarée dans le pom parent, sans duplication de fichier dans chaque projet tiers.
