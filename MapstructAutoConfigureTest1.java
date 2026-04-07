package lu.x.starter.mapstruct;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import static org.assertj.core.api.Assertions.assertThat;

class MapstructAutoConfigureTest {

    private MapstructAutoConfigure autoConfigure;
    private DefaultListableBeanFactory beanFactory;

    @BeforeEach
    void setUp() {
        autoConfigure = new MapstructAutoConfigure();
        beanFactory = new DefaultListableBeanFactory();
    }

    @Test
    void shouldRegisterValidMappersAndFilterInvalidOnes() {
        // 1. Exécution du post-processeur
        autoConfigure.postProcessBeanFactory(beanFactory);

        // 2. Test d'inclusion : Le mapper valide dans le bon package doit être enregistré
        // Le nom du bean dépend de votre implémentation (souvent userMapper ou UserMapperImpl)
        assertThat(beanFactory.getBeanDefinitionNames())
                .as("Le scanner doit trouver UserMapper dans lu.x.**.mapstruct.**")
                .anyMatch(name -> name.toLowerCase().contains("usermapper"));

        // 3. Test de filtrage (Exclusion) :
        // On vérifie que les interfaces qui ne respectent pas les critères sont absentes
        assertThat(beanFactory.containsBeanDefinition("invalidMapper"))
                .as("Une interface n'étendant pas MapstructMapper doit être filtrée")
                .isFalse();

        assertThat(beanFactory.containsBeanDefinition("externalMapper"))
                .as("Un mapper hors du package lu.x.**.mapstruct.** doit être ignoré")
                .isFalse();
    }

    @Test
    void shouldConfigureBeanDefinitionCorrecty() {
        autoConfigure.postProcessBeanFactory(beanFactory);

        // On récupère la définition du bean pour vérifier sa configuration
        String beanName = beanFactory.getBeanNamesForType(UserMapper.class)[0];
        BeanDefinition definition = beanFactory.getBeanDefinition(beanName);

        // Vérifie que le bean est bien configuré pour utiliser notre Helper ou Factory
        // Selon votre code, cela peut être le BeanClassName ou la FactoryMethod
        assertThat(definition.getScope()).isEqualTo(BeanDefinition.SCOPE_SINGLETON);
        assertThat(definition.isLazyInit()).isFalse();
    }
}

// --- Interfaces de test pour valider le filtrage ---

/**
 * CAS VALIDE : Dans le bon package et étend la bonne interface.
 */
package lu.x.app.mapstruct; 
interface UserMapper extends MapstructMapper<Object, Object> {}

/**
 * CAS INVALIDE 1 : Dans le bon package mais n'étend PAS MapstructMapper.
 */
package lu.x.app.mapstruct;
interface InvalidMapper {}

/**
 * CAS INVALIDE 2 : Étend MapstructMapper mais est HORS du package lu.x.
 */
package com.other.mapstruct;
interface ExternalMapper extends MapstructMapper<Object, Object> {}
