package com.company.starter.mapstruct;

import com.company.app.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class MapstructAutoConfigureTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            // On enregistre ton auto-configuration
            .withConfiguration(AutoConfigurations.of(MapstructAutoConfigure.class));

    @Test
    void shouldCreateMapperBeanWithDependencies() {
        this.contextRunner
                .withUserConfiguration(TestAppConfig.class)
                .run(context -> {
                    // 1. Vérifier que le bean est présent dans le contexte
                    assertThat(context).hasBean("userMapper");

                    // 2. Récupérer le bean
                    UserMapper mapper = context.getBean(UserMapper.class);

                    // 3. Vérifier qu'il n'est pas nul et qu'il est bien du type attendu
                    assertThat(mapper).isNotNull();
                    
                    // 4. Test fonctionnel (si UserMapperImpl est généré dans le classpath de test)
                    // Si UserMapperImpl n'existe pas, MapstructHelper doit avoir créé un Proxy CGLIB
                    // (ton fallback), ce qui prouve que le Helper fonctionne !
                    assertThat(mapper.getClass().getName()).contains("Cglib"); 
                    // Note: Si l'impl existe, remplace par .contains("Impl")
                });
    }

    @Configuration
    static class TestAppConfig {
        // On simule un bean dont le mapper pourrait avoir besoin
        @Bean
        public String someDependency() {
            return "test-dep";
        }
    }
}
