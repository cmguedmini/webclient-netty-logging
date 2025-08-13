package com.chawki.webclient.logs.webclient_logs.security;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 1. Composant qui détecte automatiquement au démarrage
@Component
public class AuthenticationManagerDetector implements ApplicationListener<ContextRefreshedEvent> {
    
    private static final Logger log = LoggerFactory.getLogger(AuthenticationManagerDetector.class);
    
    @Autowired
    private ApplicationContext applicationContext;
    
    private List<AuthManagerInfo> detectedManagers = new ArrayList<>();
    
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (event.getApplicationContext().equals(applicationContext)) {
            detectAuthenticationManagers();
            logDetectedManagers();
        }
    }
    
    public void detectAuthenticationManagers() {
        detectedManagers.clear();
        
        log.info("=== DETECTING AUTHENTICATION MANAGERS ===");
        
        // 1. Détecter via les beans du contexte
        detectFromBeans();
        
        // 2. Détecter via les configurations de sécurité
        detectFromSecurityConfigurations();
        
        // 3. Détecter les AuthenticationManager cachés dans d'autres composants
        detectFromComponents();
        
        log.info("=== DETECTION COMPLETE - Found {} AuthenticationManager instances ===", detectedManagers.size());
    }
    
    private void detectFromBeans() {
        log.info("1. Searching for AuthenticationManager beans...");
        
        Map<String, AuthenticationManager> authManagerBeans = applicationContext.getBeansOfType(AuthenticationManager.class);
        
        for (Map.Entry<String, AuthenticationManager> entry : authManagerBeans.entrySet()) {
            String beanName = entry.getKey();
            AuthenticationManager manager = entry.getValue();
            
            AuthManagerInfo info = new AuthManagerInfo(
                beanName,
                manager.getClass().getSimpleName(),
                "Bean Context",
                manager,
                getManagerDetails(manager)
            );
            
            detectedManagers.add(info);
            log.info("  Found: {} ({})", beanName, manager.getClass().getSimpleName());
        }
    }
    
    private void detectFromSecurityConfigurations() {
        log.info("2. Searching in Security Configurations...");
        
        // Chercher les configurations de sécurité
        Map<String, Object> securityConfigs = applicationContext.getBeansWithAnnotation(
            org.springframework.context.annotation.Configuration.class);
        
        for (Map.Entry<String, Object> entry : securityConfigs.entrySet()) {
            Object config = entry.getValue();
            if (config.getClass().getSimpleName().contains("Security")) {
                searchAuthManagerInObject(config, "SecurityConfig." + entry.getKey());
            }
        }
    }
    
    private void detectFromComponents() {
        log.info("3. Searching in Components and Services...");
        
        // Chercher dans tous les composants
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        
        for (String beanName : beanNames) {
            try {
                Object bean = applicationContext.getBean(beanName);
                if (bean != null && !bean.getClass().getPackage().getName().startsWith("org.springframework")) {
                    searchAuthManagerInObject(bean, "Component." + beanName);
                }
            } catch (Exception e) {
                // Ignorer les beans qui ne peuvent pas être instanciés
            }
        }
    }
    
    private void searchAuthManagerInObject(Object obj, String source) {
        if (obj == null) return;
        
        Field[] fields = obj.getClass().getDeclaredFields();
        
        for (Field field : fields) {
            if (AuthenticationManager.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    AuthenticationManager manager = (AuthenticationManager) field.get(obj);
                    
                    if (manager != null && !isAlreadyDetected(manager)) {
                        AuthManagerInfo info = new AuthManagerInfo(
                            field.getName(),
                            manager.getClass().getSimpleName(),
                            source,
                            manager,
                            getManagerDetails(manager)
                        );
                        
                        detectedManagers.add(info);
                        log.info("  Found: {} in {} ({})", field.getName(), source, manager.getClass().getSimpleName());
                    }
                } catch (Exception e) {
                    log.debug("Could not access field {} in {}: {}", field.getName(), source, e.getMessage());
                }
            }
        }
    }
    
    private boolean isAlreadyDetected(AuthenticationManager manager) {
        return detectedManagers.stream()
                .anyMatch(info -> info.getInstance() == manager);
    }
    
    private Map<String, Object> getManagerDetails(AuthenticationManager manager) {
        Map<String, Object> details = new HashMap<>();
        details.put("className", manager.getClass().getName());
        details.put("hashCode", manager.hashCode());
        
        if (manager instanceof ProviderManager) {
            ProviderManager pm = (ProviderManager) manager;
            details.put("providersCount", pm.getProviders().size());
            details.put("providers", pm.getProviders().stream()
                    .map(p -> p.getClass().getSimpleName())
                    .toList());
        } else if (manager instanceof MultiProviderAuthenticationManager) {
            // Votre implémentation personnalisée
            details.put("type", "Custom MultiProvider");
            try {
                Field providersField = manager.getClass().getDeclaredField("providers");
                providersField.setAccessible(true);
                @SuppressWarnings("unchecked")
                List<?> providers = (List<?>) providersField.get(manager);
                details.put("providersCount", providers.size());
                details.put("providers", providers.stream()
                        .map(p -> p.getClass().getSimpleName())
                        .toList());
            } catch (Exception e) {
                details.put("providersError", e.getMessage());
            }
        }
        
        return details;
    }
    
    private void logDetectedManagers() {
        log.info("=== AUTHENTICATION MANAGERS SUMMARY ===");
        for (int i = 0; i < detectedManagers.size(); i++) {
            AuthManagerInfo info = detectedManagers.get(i);
            log.info("{}. {} ({})", i + 1, info.getName(), info.getType());
            log.info("   Source: {}", info.getSource());
            log.info("   Details: {}", info.getDetails());
        }
        log.info("=======================================");
    }
    
    public List<AuthManagerInfo> getDetectedManagers() {
        return new ArrayList<>(detectedManagers);
    }
    
    // Classe interne pour stocker les informations
    public static class AuthManagerInfo {
        private final String name;
        private final String type;
        private final String source;
        private final AuthenticationManager instance;
        private final Map<String, Object> details;
        
        public AuthManagerInfo(String name, String type, String source, 
                              AuthenticationManager instance, Map<String, Object> details) {
            this.name = name;
            this.type = type;
            this.source = source;
            this.instance = instance;
            this.details = details;
        }
        
        // Getters
        public String getName() { return name; }
        public String getType() { return type; }
        public String getSource() { return source; }
        public AuthenticationManager getInstance() { return instance; }
        public Map<String, Object> details() { return details; }
        public Map<String, Object> getDetails() { return new HashMap<>(details); }
    }
}

// 2. Service pour l'analyse approfondie
@Service
public class AuthenticationManagerAnalyzer {
    
    private static final Logger log = LoggerFactory.getLogger(AuthenticationManagerAnalyzer.class);
    
    @Autowired
    private AuthenticationManagerDetector detector;
    
    @Autowired
    private ApplicationContext applicationContext;
    
    public Map<String, Object> analyzeAuthenticationManagers() {
        Map<String, Object> analysis = new HashMap<>();
        
        List<AuthenticationManagerDetector.AuthManagerInfo> managers = detector.getDetectedManagers();
        
        analysis.put("totalCount", managers.size());
        analysis.put("managers", managers.stream().map(this::convertToAnalysisInfo).toList());
        analysis.put("duplicateInstances", findDuplicateInstances(managers));
        analysis.put("recommendations", generateRecommendations(managers));
        
        return analysis;
    }
    
    private Map<String, Object> convertToAnalysisInfo(AuthenticationManagerDetector.AuthManagerInfo info) {
        Map<String, Object> analysisInfo = new HashMap<>();
        analysisInfo.put("name", info.getName());
        analysisInfo.put("type", info.getType());
        analysisInfo.put("source", info.getSource());
        analysisInfo.put("details", info.getDetails());
        analysisInfo.put("isCustom", isCustomImplementation(info.getInstance()));
        analysisInfo.put("isSpringDefault", isSpringDefaultImplementation(info.getInstance()));
        
        return analysisInfo;
    }
    
    private List<String> findDuplicateInstances(List<AuthenticationManagerDetector.AuthManagerInfo> managers) {
        List<String> duplicates = new ArrayList<>();
        Map<Integer, List<String>> instanceMap = new HashMap<>();
        
        for (AuthenticationManagerDetector.AuthManagerInfo info : managers) {
            int hashCode = info.getInstance().hashCode();
            instanceMap.computeIfAbsent(hashCode, k -> new ArrayList<>()).add(info.getName());
        }
        
        instanceMap.forEach((hash, names) -> {
            if (names.size() > 1) {
                duplicates.add("Same instance found in: " + String.join(", ", names));
            }
        });
        
        return duplicates;
    }
    
    private List<String> generateRecommendations(List<AuthenticationManagerDetector.AuthManagerInfo> managers) {
        List<String> recommendations = new ArrayList<>();
        
        if (managers.size() > 2) {
            recommendations.add("Vous avez " + managers.size() + " AuthenticationManager instances. Considérez la consolidation.");
        }
        
        boolean hasCustom = managers.stream().anyMatch(info -> isCustomImplementation(info.getInstance()));
        boolean hasDefault = managers.stream().anyMatch(info -> isSpringDefaultImplementation(info.getInstance()));
        
        if (hasCustom && hasDefault) {
            recommendations.add("Vous mélangez des implémentations custom et Spring par défaut. Vérifiez la cohérence.");
        }
        
        if (managers.isEmpty()) {
            recommendations.add("Aucun AuthenticationManager détecté. Vérifiez votre configuration de sécurité.");
        }
        
        return recommendations;
    }
    
    private boolean isCustomImplementation(AuthenticationManager manager) {
        return manager instanceof MultiProviderAuthenticationManager || 
               !manager.getClass().getPackage().getName().startsWith("org.springframework");
    }
    
    private boolean isSpringDefaultImplementation(AuthenticationManager manager) {
        return manager instanceof ProviderManager;
    }
    
    public void logAnalysis() {
        Map<String, Object> analysis = analyzeAuthenticationManagers();
        
        log.info("=== AUTHENTICATION MANAGER ANALYSIS ===");
        log.info("Total AuthenticationManagers found: {}", analysis.get("totalCount"));
        
        @SuppressWarnings("unchecked")
        List<String> duplicates = (List<String>) analysis.get("duplicateInstances");
        if (!duplicates.isEmpty()) {
            log.warn("Duplicate instances detected:");
            duplicates.forEach(dup -> log.warn("  - {}", dup));
        }
        
        @SuppressWarnings("unchecked")
        List<String> recommendations = (List<String>) analysis.get("recommendations");
        if (!recommendations.isEmpty()) {
            log.info("Recommendations:");
            recommendations.forEach(rec -> log.info("  - {}", rec));
        }
        
        log.info("=======================================");
    }
}

// 3. Contrôleur REST pour l'inspection via API
@RestController
@RequestMapping("/api/debug/auth-managers")
@PreAuthorize("hasRole('ADMIN')")
public class AuthenticationManagerInspectionController {
    
    @Autowired
    private AuthenticationManagerDetector detector;
    
    @Autowired
    private AuthenticationManagerAnalyzer analyzer;
    
    @GetMapping("/detect")
    public ResponseEntity<Map<String, Object>> detectAuthenticationManagers() {
        detector.detectAuthenticationManagers();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Detection completed");
        response.put("managers", detector.getDetectedManagers().stream()
                .map(info -> Map.of(
                        "name", info.getName(),
                        "type", info.getType(),
                        "source", info.getSource(),
                        "details", info.getDetails()
                ))
                .toList());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeAuthenticationManagers() {
        Map<String, Object> analysis = analyzer.analyzeAuthenticationManagers();
        return ResponseEntity.ok(analysis);
    }
    
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getAuthManagerSummary() {
        List<AuthenticationManagerDetector.AuthManagerInfo> managers = detector.getDetectedManagers();
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalCount", managers.size());
        summary.put("types", managers.stream()
                .map(AuthenticationManagerDetector.AuthManagerInfo::getType)
                .distinct()
                .toList());
        summary.put("sources", managers.stream()
                .map(AuthenticationManagerDetector.AuthManagerInfo::getSource)
                .distinct()
                .toList());
        
        return ResponseEntity.ok(summary);
    }
}

// 4. Component d'initialisation pour démarrage automatique
@Component
public class AuthenticationManagerStartupAnalyzer {
    
    private static final Logger log = LoggerFactory.getLogger(AuthenticationManagerStartupAnalyzer.class);
    
    @Autowired
    private AuthenticationManagerAnalyzer analyzer;
    
    @EventListener
    public void handleApplicationReady(ApplicationReadyEvent event) {
        log.info("Application ready - Running AuthenticationManager analysis...");
        analyzer.logAnalysis();
    }
}