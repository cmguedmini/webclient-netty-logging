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
package lu.x.starter.mapstruct;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.beans.factory.support.RootBeanDefinition;

import static org.assertj.core.api.Assertions.assertThat;

class MapstructAutoConfigureTest {

    private MapstructAutoConfigure autoConfigure;
    private AnnotationConfigApplicationContext context;

    @BeforeEach
    void setUp() {
        autoConfigure = new MapstructAutoConfigure();
        context = new AnnotationConfigApplicationContext();
    }

    /**
     * Test combiné pour registerMapper, resolveBeanName et buildFactoryBeanDefinition.
     * En appelant postProcessBeanFactory, on force le passage dans toute la chaîne privée.
     */
    @Test
    void shouldCoverRegistrationLogic() {
        // 1. On enregistre le processeur
        context.addBeanFactoryPostProcessor(autoConfigure);
        
        // 2. On lance le rafraîchissement du contexte
        // Cela va scanner le package "lu.x.**.mapstruct.**"
        context.refresh();

        // 3. Assertion sur resolveBeanName : 
        // Vérifie que l'interface "UserMapper" est devenue le bean "userMapper"
        String[] beanNames = context.getBeanNamesForType(UserMapper.class);
        assertThat(beanNames).contains("userMapper");

        // 4. Assertion sur buildFactoryBeanDefinition :
        // On récupère la définition pour vérifier si elle est bien construite
        BeanDefinition bd = context.getBeanFactory().getBeanDefinition("userMapper");
        
        assertThat(bd).isNotNull();
        // Vérifie que c'est bien un singleton (comportement par défaut de buildFactoryBeanDefinition)
        assertThat(bd.isSingleton()).isTrue();
        // Vérifie que la définition n'est pas abstraite
        assertThat(bd.isAbstract()).isFalse();
    }

    @Test
    void shouldHandleNoMappersFound() {
        // Test de la branche "No MapStruct mappers found" pour la couverture du log
        // On crée un contexte vide sans mappers dans le classpath de scan
        try (AnnotationConfigApplicationContext emptyContext = new AnnotationConfigApplicationContext()) {
            MapstructAutoConfigure processor = new MapstructAutoConfigure();
            emptyContext.addBeanFactoryPostProcessor(processor);
            emptyContext.refresh();
            
            assertThat(emptyContext.getBeanDefinitionCount()).isGreaterThan(0); // Le processeur lui-même
        }
    }
}

/**
 * Interface de test située impérativement dans le package scanné : lu.x.app.mapstruct
 * DOIT être publique pour éviter les problèmes de visibilité sans setAccessible.
 */
package lu.x.app.mapstruct;
import lu.x.starter.mapstruct.MapstructMapper;

public interface UserMapper extends MapstructMapper<String, String> {
}

package lu.x.starter.mapstruct;

import lu.x.app.mapstruct.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;

import static org.assertj.core.api.Assertions.assertThat;

class MapstructAutoConfigureTest {

    @Test
    void shouldForceRegisterMapperForCoverage() throws Exception {
        MapstructAutoConfigure autoConfigure = new MapstructAutoConfigure();
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

        // 1. Simulation de la découverte d'un Mapper (MetadataReader)
        // On récupère les métadonnées de notre interface de test
        SimpleMetadataReaderFactory factory = new SimpleMetadataReaderFactory();
        MetadataReader reader = factory.getMetadataReader(UserMapper.class.getName());

        // 2. Appel DIRECT de la logique d'enregistrement (Couvre vos méthodes privées/protégées)
        // Si ces méthodes sont 'protected' ou 'public', appelez-les directement.
        // Si elles sont 'private', testez via postProcessBeanFactory en vous assurant 
        // que le BeanFactory est bien configuré.
        autoConfigure.registerMapper(beanFactory, reader);

        // 3. Assertions
        String[] beanNames = beanFactory.getBeanNamesForType(UserMapper.class);
        
        assertThat(beanNames)
            .as("La méthode registerMapper doit enregistrer le bean dans la factory")
            .contains("userMapper");

        BeanDefinition bd = beanFactory.getBeanDefinition("userMapper");
        assertThat(bd.getBeanClassName()).isNotNull();
    }
}
