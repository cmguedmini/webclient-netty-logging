@Component
public class DeprecatedPropertiesValidator implements SmartInitializingSingleton {

    private static final Logger logger = LoggerFactory.getLogger(DeprecatedPropertiesValidator.class);

    private final ConfigurableEnvironment environment;
    private final DeprecatedPropertiesConfigLoader configLoader;

    public DeprecatedPropertiesValidator(ConfigurableEnvironment environment,
                                         DeprecatedPropertiesConfigLoader configLoader) {
        this.environment = environment;
        this.configLoader = configLoader;
    }

    @Override
    public void afterSingletonsInstantiated() {
        List<String> deprecatedKeys = configLoader.getDeprecatedKeys();
        List<String> deprecatedPrefixes = configLoader.getDeprecatedWildcards();
        String guideUrl = configLoader.getGuideUrl();

        Set<String> allPropertyNames = new HashSet<>();
        for (PropertySource<?> source : environment.getPropertySources()) {
            if (source.getSource() instanceof Map<?, ?> map) {
                for (Object key : map.keySet()) {
                    allPropertyNames.add(key.toString());
                }
            }
        }

        for (String key : deprecatedKeys) {
            if (environment.containsProperty(key)) {
                String value = environment.getProperty(key);
                logger.warn("⚠️ La propriété dépréciée '{}' est utilisée avec la valeur '{}'. Veuillez la supprimer. Consultez le guide de migration : {}", key, value, guideUrl);
            }
        }

        for (String prefix : deprecatedPrefixes) {
            for (String propName : allPropertyNames) {
                if (propName.startsWith(prefix)) {
                    String value = environment.getProperty(propName);
                    logger.warn("⚠️ La propriété dépréciée '{}' (préfixe '{}') est utilisée avec la valeur '{}'. Veuillez la supprimer. Consultez le guide de migration : {}", propName, prefix, value, guideUrl);
                }
            }
        }
    }
}
----
@Test
void shouldLogWarningsForWildcardProperties() {
    // Mock du logger
    Logger logger = (Logger) LoggerFactory.getLogger(DeprecatedPropertiesValidator.class);
    Appender<ILoggingEvent> mockAppender = mock(Appender.class);
    when(mockAppender.getName()).thenReturn("MOCK");
    logger.addAppender(mockAppender);
    logger.setLevel(Level.WARN);

    // Mock de l'environnement
    ConfigurableEnvironment environment = mock(ConfigurableEnvironment.class);
    Map<String, Object> properties = Map.of(
        "jef.core.timeout", "5000",
        "code.log.test", "true"
    );
    MapPropertySource propertySource = new MapPropertySource("test", properties);
    when(environment.getPropertySources()).thenReturn(new org.springframework.core.env.MutablePropertySources());
    environment.getPropertySources().addLast(propertySource);

    for (String key : properties.keySet()) {
        when(environment.containsProperty(key)).thenReturn(true);
        when(environment.getProperty(key)).thenReturn(properties.get(key).toString());
    }

    // Mock du config loader
    DeprecatedPropertiesConfigLoader configLoader = mock(DeprecatedPropertiesConfigLoader.class);
    when(configLoader.getDeprecatedKeys()).thenReturn(List.of("app.old-property"));
    when(configLoader.getDeprecatedWildcards()).thenReturn(List.of("jef.core.", "code.log."));
    when(configLoader.getGuideUrl()).thenReturn("https://ton-site.com/guide-migration");

    // Test du validator
    DeprecatedPropertiesValidator validator = new DeprecatedPropertiesValidator(environment, configLoader);
    validator.afterSingletonsInstantiated();

    ArgumentCaptor<ILoggingEvent> captor = ArgumentCaptor.forClass(ILoggingEvent.class);
    verify(mockAppender, atLeastOnce()).doAppend(captor.capture());

    List<ILoggingEvent> logs = captor.getAllValues();
    assertTrue(logs.stream().anyMatch(log -> log.getFormattedMessage().contains("jef.core.timeout")));
    assertTrue(logs.stream().anyMatch(log -> log.getFormattedMessage().contains("code.log.test")));
}
