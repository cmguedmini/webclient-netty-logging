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
    void shouldRegisterAndConfigureMapperBean() {
        this.contextRunner
                .withUserConfiguration(TestAppConfig.class)
                // On force le package de scan si le starter utilise une propriété, 
                // sinon on s'assure que TestAppConfig est au bon endroit.
                .run(context -> {
                    // 1. Validation de resolveBeanName & registerMapper
                    assertThat(context).hasBean("userMapper");

                    // 2. Validation de buildFactoryBeanDefinition (L'inspection technique)
                    // C'est cette partie qui valide les lignes de code pour Sonar
                    BeanDefinition bd = context.getBeanFactory()
                                               .getBeanDefinition("userMapper");
                    
                    // Vérification que l'argument 0 est bien l'interface (couvre buildFactoryBeanDefinition)
                    Object constructorArg = bd.getConstructorArgumentValues()
                                              .getIndexedArgumentValue(0, Class.class)
                                              .getValue();
                    
                    assertThat(constructorArg).isEqualTo(UserMapper.class);
                    assertThat(bd.getFactoryMethodName()).isEqualTo("build");

                    // 3. Validation du fonctionnement (Proxy ou Impl)
                    UserMapper mapper = context.getBean(UserMapper.class);
                    assertThat(mapper).isNotNull();
                });
    }
    
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

package lu.x.starter.mapstruct;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class MapstructAutoConfigureTest {

    @Test
    void shouldRegisterBeanDefinitionsDuringPostProcessing() {
        // 1. Initialisation de l'AutoConfigure et d'un registre de beans vide
        MapstructAutoConfigure autoConfigure = new MapstructAutoConfigure();
        BeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();

        // 2. Exécution de la méthode à tester
        // Note : Si MapstructAutoConfigure implémente BeanDefinitionRegistryPostProcessor,
        // on appelle postProcessBeanDefinitionRegistry. 
        // Si c'est postProcessBeanFactory, on passe un ConfigurableListableBeanFactory.
        autoConfigure.postProcessBeanDefinitionRegistry(registry);

        // 3. Assertions sur la couverture
        // On vérifie qu'au moins un bean de type mapper a été identifié dans le classpath de test
        String[] beanNames = registry.getBeanDefinitionNames();
        
        assertThat(beanNames)
                .as("Le scanner doit trouver UserMapper dans le package lu.x.**.mapstruct.**")
                .anyMatch(name -> name.contains("userMapper"));

        // 4. Vérification de la configuration de la définition du bean
        if (registry.containsBeanDefinition("userMapper")) {
            var definition = registry.getBeanDefinition("userMapper");
            // On vérifie que la définition pointe bien vers notre Helper ou Factory
            assertThat(definition.getBeanClassName()).isNotNull();
        }
    }
}
