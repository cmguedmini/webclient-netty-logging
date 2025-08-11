// Solution élégante : Utilisation de @ConfigurationProperties avec des objets wrapper

// 1. Structure YAML modifiée (plus flexible)
/*
jef:
  security:
     actuator:
        endpoints:     
             - url: "/actuator/health"
               permit: false
               roles: ["ADMIN"]
             - url: "/actuator/info"
               permit: true
               roles: ["USER"]
             - url: "/actuator/metrics"
               permit: false
               roles: ["ADMIN", "MONITOR"]
*/

// 2. Classes de configuration
@ConfigurationProperties(prefix = "jef.security.actuator")
@Component
public class JEFSecurityProperties {
    
    private List<EndpointSecurity> endpoints = new ArrayList<>();
    
    public List<EndpointSecurity> getEndpoints() {
        return endpoints;
    }
    
    public void setEndpoints(List<EndpointSecurity> endpoints) {
        this.endpoints = endpoints;
    }
    
    // Classe interne pour représenter chaque endpoint
    public static class EndpointSecurity {
        private String url;
        private boolean permit;
        private List<String> roles = new ArrayList<>();
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public boolean isPermit() { return permit; }
        public void setPermit(boolean permit) { this.permit = permit; }
        
        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
    }
}

// 3. Code d'utilisation (très simple et clean)
List<JEFSecurityProperties.EndpointSecurity> endpointSecurities = jefSecurityProperties.getEndpoints();
for (JEFSecurityProperties.EndpointSecurity endpointSecurity : endpointSecurities) {
    String endpoint = endpointSecurity.getUrl(); // URL exacte du YAML
    configureEndpointSecurity(authz, endpoint, endpointSecurity);
}