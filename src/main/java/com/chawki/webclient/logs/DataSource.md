Le problème vient du fait que Spring Boot **ignore votre `@Primary`** et l'auto-configuration MyBatis préfère HSQLDB.  Cela est dû à une mauvaise configuration de votre fichier `application.properties` ou `application.yml`.

-----

### Explication du problème

Même si vous avez explicitement désigné une source de données comme **`@Primary`**, le starter **MyBatis** (qui utilise **`mybatis-spring-boot-starter`**) a une logique d'auto-configuration par défaut qui le pousse à **préférer les bases de données embarquées** comme HSQLDB s'il en trouve une sur le classpath.

De plus, l'utilisation de **`@ConfigurationProperties`** nécessite que vous ayez défini correctement les propriétés de connexion pour vos deux sources de données Oracle. Si ces propriétés sont absentes ou mal orthographiées, le processus de configuration échoue et Spring Boot se rabat sur l'option la plus sûre : la base de données embarquée HSQLDB.

-----

### Solution

Pour résoudre ce problème, il vous faut désactiver l'auto-configuration de la source de données embarquée par le starter MyBatis et correctement configurer vos deux sources de données Oracle.

1.  **Exclure l'auto-configuration de MyBatis pour HSQLDB** : Dans votre classe principale `main`, vous devez exclure l'auto-configuration de la source de données de MyBatis pour forcer Spring Boot à utiliser vos configurations manuelles.

    ```java
    @SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, MybatisAutoConfiguration.class})
    public class MonProjetApplication {
        // ...
    }
    ```

    Cette exclusion vous donne un contrôle total sur la configuration de la source de données.

2.  **Configuration des propriétés** : Vous devez définir explicitement les propriétés de connexion pour vos deux sources de données dans votre fichier `application.yml` (ou `application.properties`).

    ```yaml
    # application.yml
    component:
      datasource:
        aas:
          url: jdbc:oracle:thin:@//localhost:1521/primarydb
          username: user_aas
          password: password_aas
          driver-class-name: oracle.jdbc.OracleDriver
        isi:
          url: jdbc:oracle:thin:@//localhost:1521/secondarydb
          username: user_isi
          password: password_isi
          driver-class-name: oracle.jdbc.OracleDriver
    ```

En suivant ces étapes, Spring Boot et MyBatis utiliseront vos configurations Oracle personnalisées et votre application ne se connectera plus par erreur à HSQLDB.
""""""""""""""""""""""""""""""
Dans ce cas, la configuration de votre starter MyBatis personnalisé est la cause du problème. Même si vous avez configuré vos sources de données Oracle, votre starter applique sa propre logique qui priorise le `DataSource` HSQLDB. Pour y remédier, vous devez ajuster votre configuration en deux points.

### 1\. Désactiver l'Auto-configuration de HSQLDB

Votre starter MyBatis personnalisé doit avoir une fonctionnalité qui cherche et configure automatiquement un `DataSource` HSQLDB si aucune source de données n'est spécifiée. Il faut explicitement désactiver cette fonctionnalité en utilisant l'annotation **`@EnableAutoConfiguration`** ou en configurant la propriété dans le fichier `application.yml`.

Vous pouvez ajouter la ligne suivante dans votre fichier `application.yml` :

```yaml
spring:
  datasource:
    # Désactiver l'auto-configuration de la base de données embarquée HSQL
    type: com.zaxxer.hikari.HikariDataSource # Ou le type de votre choix
    # ...
```

-----

### 2\. Configurer Explicitment le `DataSource` par défaut de MyBatis

Ensuite, vous devez indiquer à MyBatis d'utiliser votre `DataSource` primaire. Bien que l'annotation `@Primary` soit censée le faire, il est possible que la logique interne de votre starter maison l'ignore. Dans ce cas, il faut lier explicitement le `DataSource` à MyBatis.

Vous pouvez le faire en créant un `SqlSessionFactory` et en le liant à votre `datasourceAas` qui est annoté avec `@Primary`.

```java
@Configuration
public class MyBatisConfig {

    @Bean
    public SqlSessionFactory sqlSessionFactory(@Qualifier("datasourceAas") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        return factoryBean.getObject();
    }
}
```

L'annotation **`@Qualifier("datasourceAas")`** force l'injection de votre `DataSource` Oracle, et **c'est cette configuration qui va être utilisée par MyBatis**.
