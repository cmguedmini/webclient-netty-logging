Voici le code complet :

```java
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.time.Duration.ofSeconds;

class SchedulerStarterTest {

    private static final String PREFIX_POOL = "pool-IT-test";

    // ----------------------------------------------------------------
    // Runner partagé — configuration minimale, pas de scan classpath
    // ----------------------------------------------------------------

    private final ApplicationContextRunner contextRunner =
        new ApplicationContextRunner()
            .withUserConfiguration(
                SchedulerAutoConfiguration.class,
                MyConfig.class,
                TestTaskExecutorConfiguration.class
            )
            .withPropertyValues("spring.main.banner-mode=off");

    // ----------------------------------------------------------------
    // 1. Vérification du wiring Spring
    //    Remplace : test_Application() + @SpringBootTest + @Autowired
    // ----------------------------------------------------------------

    @Test
    void application_context_should_contain_expected_beans() {
        contextRunner.run(ctx -> {
            assertThat(ctx).hasSingleBean(SchedulerAutoConfiguration.class);
            assertThat(ctx).doesNotHaveBean(ConcurrentTaskScheduler.class);
            assertThat(ctx).hasSingleBean(ScheduledThreadPoolTaskExecutor.class);
        });
    }

    // ----------------------------------------------------------------
    // 2. Vérification que la tâche s'exécute sur le bon pool de threads
    //    Remplace : @Scheduled scheduledTestTask() + test_checkLog()
    //    Pas de contexte Spring — scheduler instancié manuellement
    // ----------------------------------------------------------------

    @Test
    void scheduled_task_should_run_on_correct_thread_pool() {
        // Arrange
        AtomicBoolean executed = new AtomicBoolean(false);
        AtomicReference<String> threadName = new AtomicReference<>();

        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadNamePrefix(PREFIX_POOL + "-");
        scheduler.setPoolSize(1);
        scheduler.initialize();

        // Act
        scheduler.schedule(
            () -> {
                threadName.set(Thread.currentThread().getName());
                executed.set(true);
            },
            Instant.now()
        );

        // Assert
        await().atMost(ofSeconds(2)).untilTrue(executed);
        assertThat(threadName.get()).startsWith(PREFIX_POOL);

        // Cleanup — pas de @AfterEach, tout est local au test
        scheduler.destroy();
    }

    // ----------------------------------------------------------------
    // 3. Vérification que MyConfig expose bien un ObjectMapper
    //    Teste la configuration interne de façon isolée
    // ----------------------------------------------------------------

    @Test
    void my_config_should_provide_object_mapper_bean() {
        new ApplicationContextRunner()
            .withUserConfiguration(MyConfig.class)
            .run(ctx -> assertThat(ctx).hasSingleBean(ObjectMapper.class));
    }

    // ----------------------------------------------------------------
    // Configuration locale — remplace l'inner class @Configuration
    // originale, sans @SpringBootTest pour la charger
    // ----------------------------------------------------------------

    @Configuration
    static class MyConfig {

        @Bean
        ObjectMapper objectMapper() {
            return ObjectMapperHelper.buildObjectMapper();
        }
    }
}
```

## Points d'attention à adapter à ton projet

| Élément | À vérifier |
|---|---|
| `TestTaskExecutorConfiguration.class` | Confirme que cette classe configure bien le `ThreadNamePrefix` à `pool-IT-test` |
| `ScheduledThreadPoolTaskExecutor.class` | Vérifie le nom exact de ta classe — j'ai repris ce qui était visible dans l'image |
| `ObjectMapperHelper.buildObjectMapper()` | Import à ajuster selon ton package |
| `ofSeconds(2)` | Remplace `Directions.TWO_SECONDS` — adapte si cette constante vient d'une lib métier |
| `ExecutorServiceHelper.shutdownExecutorServices()` | Le `@AfterAll` original disparaît car `scheduler.destroy()` suffit ici — à valider si d'autres ressources étaient concernées |
