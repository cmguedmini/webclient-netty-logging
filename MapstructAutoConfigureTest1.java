package lu.x.starter.mapstruct;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.beans.factory.config.BeanDefinition;

import static org.assertj.core.api.Assertions.assertThat;

class MapstructAutoConfigureTest {

    @Test
    void shouldRegisterAndFilterMappers() {
        // 1. Création d'un contexte Spring minimal
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            
            // 2. Enregistrement manuel du post-processor à tester
            MapstructAutoConfigure autoConfigure = new MapstructAutoConfigure();
            context.addBeanFactoryPostProcessor(autoConfigure);

            // 3. Démarrage du contexte (déclenche postProcessBeanFactory)
            context.refresh();

            // 4. Test d'inclusion : Recherche par le type de l'interface de test
            String[] beanNames = context.getBeanNamesForType(UserMapper.class);
            
            assertThat(beanNames)
                .as("Le bean pour UserMapper devrait être enregistré")
                .isNotEmpty();

            String beanName = beanNames[0];
            BeanDefinition definition = context.getBeanFactory().getBeanDefinition(beanName);
            
            assertThat(definition).isNotNull();
            assertThat(definition.isSingleton()).isTrue();

            // 5. Test de filtrage : L'interface invalide ne doit pas être un bean
            assertThat(context.containsBeanDefinition("invalidMapper")).isFalse();
        }
    }
}

// --- IMPORTANT : Ces interfaces doivent être dans src/test/java 
// dans le package lu.x.app.mapstruct pour être scannées ---
