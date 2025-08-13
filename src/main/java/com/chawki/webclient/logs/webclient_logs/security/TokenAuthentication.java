// TokenAuthentication - Version corrigée et immutable
public class TokenAuthentication extends AbstractAuthenticationToken {
    
    private final String token;
    private final Object principal;
    private final boolean authenticated;

    /**
     * Constructeur pour un token non authentifié (avant validation)
     */
    public TokenAuthentication(String token) {
        super(Collections.emptyList()); // Pas d'autorités pour un token non authentifié
        this.token = token;
        this.principal = null;
        this.authenticated = false;
    }
    
    /**
     * Constructeur pour un token authentifié (après validation réussie)
     */
    public TokenAuthentication(String token, Object principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities != null ? authorities : Collections.emptyList());
        this.token = token;
        this.principal = principal;
        this.authenticated = true;
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }
    
    public String getToken() {
        return token;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    /**
     * IMPORTANT: Cette méthode doit lever une exception pour préserver l'immutabilité
     */
    @Override
    public void setAuthenticated(boolean authenticated) throws IllegalArgumentException {
        if (authenticated) {
            throw new IllegalArgumentException(
                "Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead");
        }
        // Si authenticated = false, on ne fait rien car l'objet est déjà dans cet état
    }

    @Override
    public String toString() {
        return "TokenAuthentication{" +
                "token='" + (token != null ? token.substring(0, Math.min(token.length(), 10)) + "..." : "null") + '\'' +
                ", principal=" + principal +
                ", authenticated=" + authenticated +
                ", authorities=" + getAuthorities() +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        
        TokenAuthentication that = (TokenAuthentication) obj;
        return authenticated == that.authenticated &&
                Objects.equals(token, that.token) &&
                Objects.equals(principal, that.principal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), token, principal, authenticated);
    }
}