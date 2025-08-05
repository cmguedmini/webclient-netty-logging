#!/bin/bash

# test-environments.sh
# Script pour tester la sécurité des endpoints Actuator dans différents environnements

echo "=== TESTING MULTI-ENVIRONMENT ACTUATOR SECURITY ==="

# Couleurs pour les messages
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Fonction pour faire un test HTTP
test_endpoint() {
    local url=$1
    local auth=$2
    local expected_status=$3
    local description=$4
    
    if [ -n "$auth" ]; then
        response=$(curl -s -w "%{http_code}" -u "$auth" "$url" -o /dev/null)
    else
        response=$(curl -s -w "%{http_code}" "$url" -o /dev/null)
    fi
    
    if [ "$response" -eq "$expected_status" ]; then
        echo -e "${GREEN}✓${NC} $description - Status: $response (Expected: $expected_status)"
    else
        echo -e "${RED}✗${NC} $description - Status: $response (Expected: $expected_status)"
    fi
}

# Test pour l'environnement UAT (port 8081)
test_uat_environment() {
    echo -e "\n${YELLOW}=== TESTING UAT ENVIRONMENT (Port 8081) ===${NC}"
    echo "In UAT: All actuator endpoints should be PUBLIC"
    
    UAT_BASE="http://localhost:8081"
    
    # Tests sans authentification (doivent passer)
    test_endpoint "$UAT_BASE/actuator/health" "" 200 "UAT: Health endpoint (no auth)"
    test_endpoint "$UAT_BASE/actuator/info" "" 200 "UAT: Info endpoint (no auth)"
    test_endpoint "$UAT_BASE/actuator/metrics" "" 200 "UAT: Metrics endpoint (no auth)"
    test_endpoint "$UAT_BASE/actuator/env" "" 200 "UAT: Env endpoint (no auth)"
    test_endpoint "$UAT_BASE/actuator/security-info" "" 200 "UAT: Custom security-info endpoint (no auth)"
    
    # Test endpoint protégé de l'application
    test_endpoint "$UAT_BASE/api/protected" "" 401 "UAT: Protected API endpoint (no auth - should fail)"
    test_endpoint "$UAT_BASE/api/protected" "admin:admin123" 200 "UAT: Protected API endpoint (with auth)"
}

# Test pour l'environnement PROD (port 8082)
test_prod_environment() {
    echo -e "\n${YELLOW}=== TESTING PROD ENVIRONMENT (Port 8082) ===${NC}"
    echo "In PROD: Actuator endpoints require ROLE_ACTUATOR (except health/info)"
    
    PROD_BASE="http://localhost:8082"
    
    # Tests des endpoints publics (doivent passer sans auth)
    test_endpoint "$PROD_BASE/actuator/health" "" 200 "PROD: Health endpoint (no auth)"
    test_endpoint "$PROD_BASE/actuator/info" "" 200 "PROD: Info endpoint (no auth)"
    
    # Tests des endpoints sécurisés sans auth (doivent échouer)
    test_endpoint "$PROD_BASE/actuator/metrics" "" 401 "PROD: Metrics endpoint (no auth - should fail)"
    test_endpoint "$PROD_BASE/actuator/env" "" 401 "PROD: Env endpoint (no auth - should fail)"
    
    # Tests des endpoints sécurisés avec utilisateur normal (doivent échouer)
    test_endpoint "$PROD_BASE/actuator/metrics" "user:user123" 403 "PROD: Metrics endpoint (user role - should fail)"
    
    # Tests des endpoints sécurisés avec rôle ACTUATOR (doivent passer)
    test_endpoint "$PROD_BASE/actuator/metrics" "actuator:actuator123" 200 "PROD: Metrics endpoint (actuator role)"
    test_endpoint "$PROD_BASE/actuator/security-info" "actuator:actuator123" 200 "PROD: Custom security-info (actuator role)"
    
    # Tests avec admin (doit avoir accès aussi)
    test_endpoint "$PROD_BASE/actuator/metrics" "admin:admin123" 200 "PROD: Metrics endpoint (admin role)"
}

# Test pour l'environnement DEV (port 8080)
test_dev_environment() {
    echo -e "\n${YELLOW}=== TESTING DEV ENVIRONMENT (Port 8080) ===${NC}"
    echo "In DEV: Actuator endpoints require ROLE_ADMIN"
    
    DEV_BASE="http://localhost:8080"
    
    # Test endpoint public
    test_endpoint "$DEV_BASE/actuator/health" "" 200 "DEV: Health endpoint (no auth)"
    
    # Tests des endpoints sécurisés sans auth (doivent échouer)
    test_endpoint "$DEV_BASE/actuator/info" "" 401 "DEV: Info endpoint (no auth - should fail)"
    test_endpoint "$DEV_BASE/actuator/metrics" "" 401 "DEV: Metrics endpoint (no auth - should fail)"
    
    # Tests avec utilisateur normal (doivent échouer)
    test_endpoint "$DEV_BASE/actuator/info" "user:user123" 403 "DEV: Info endpoint (user role - should fail)"
    
    # Tests avec utilisateur actuator (doivent échouer car besoin ADMIN)
    test_endpoint "$DEV_BASE/actuator/info" "actuator:actuator123" 403 "DEV: Info endpoint (actuator role - should fail)"
    
    # Tests avec admin (doivent passer)
    test_endpoint "$DEV_BASE/actuator/info" "admin:admin123" 200 "DEV: Info endpoint (admin role)"
    test_endpoint "$DEV_BASE/actuator/metrics" "admin:admin123" 200 "DEV: Metrics endpoint (admin role)"
}

# Test des endpoints de l'application
test_application_endpoints() {
    echo -e "\n${YELLOW}=== TESTING APPLICATION ENDPOINTS ===${NC}"
    
    for port in 8080 8081 8082; do
        BASE="http://localhost:$port"
        echo -e "\nTesting port $port:"
        
        # Endpoint public
        test_endpoint "$BASE/api/public/health" "" 200 "Public health endpoint"
        
        # Endpoint protégé
        test_endpoint "$BASE/api/protected" "" 401 "Protected endpoint (no auth - should fail)"
        test_endpoint "$BASE/api/protected" "admin:admin123" 200 "Protected endpoint (admin auth)"
        
        # Test avec token JWT
        echo "Getting JWT token..."
        TOKEN=$(curl -s -X POST "$BASE/api/login" \
            -H "Content-Type: application/json" \
            -d '{"username":"admin","password":"admin123"}' | \
            grep -o '"token":"[^"]*"' | \
            sed 's/"token":"\([^"]*\)"/\1/')
        
        if [ -n "$TOKEN" ]; then
            response=$(curl -s -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "$BASE/api/protected" -o /dev/null)
            if [ "$response" -eq 200 ]; then
                echo -e "${GREEN}✓${NC} JWT authentication works - Status: $response"
            else
                echo -e "${RED}✗${NC} JWT authentication failed - Status: $response"
            fi
        fi
    done
}

# Fonction principale
main() {
    echo "Starting environment-specific security tests..."
    echo "Make sure to start the application with different profiles:"
    echo "  - DEV:  mvn spring-boot:run -Dspring-boot.run.profiles=dev"
    echo "  - UAT:  mvn spring-boot:run -Dspring-boot.run.profiles=uat"
    echo "  - PROD: mvn spring-boot:run -Dspring-boot.run.profiles=prod"
    echo
    
    # Vérifier quels ports sont actifs
    echo "Checking which environments are running..."
    
    DEV_RUNNING=false
    UAT_RUNNING=false
    PROD_RUNNING=false
    
    if curl -s -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
        DEV_RUNNING=true
        echo -e "${GREEN}✓${NC} DEV environment detected on port 8080"
    fi
    
    if curl -s -f http://localhost:8081/actuator/health > /dev/null 2>&1; then
        UAT_RUNNING=true
        echo -e "${GREEN}✓${NC} UAT environment detected on port 8081"
    fi
    
    if curl -s -f http://localhost:8082/actuator/health > /dev/null 2>&1; then
        PROD_RUNNING=true
        echo -e "${GREEN}✓${NC} PROD environment detected on port 8082"
    fi
    
    if [ "$DEV_RUNNING" = false ] && [ "$UAT_RUNNING" = false ] && [ "$PROD_RUNNING" = false ]; then
        echo -e "${RED}✗${NC} No environments detected. Please start the application first."
        exit 1
    fi
    
    # Exécuter les tests selon les environnements détectés
    if [ "$DEV_RUNNING" = true ]; then
        test_dev_environment
    fi
    
    if [ "$UAT_RUNNING" = true ]; then
        test_uat_environment
    fi
    
    if [ "$PROD_RUNNING" = true ]; then
        test_prod_environment
    fi
    
    # Test des endpoints d'application
    test_application_endpoints
    
    echo -e "\n${YELLOW}=== TEST SUMMARY ===${NC}"
    echo "Tests completed for detected environments."
    echo "Check the results above to verify security configuration."
}

# Fonction pour tester avec différents utilisateurs
test_user_roles() {
    echo -e "\n${YELLOW}=== TESTING USER ROLES ===${NC}"
    
    local base_url=$1
    local env_name=$2
    
    echo "Testing user roles for $env_name environment ($base_url)"
    
    # Test avec différents utilisateurs
    users=("admin:admin123:ADMIN,ACTUATOR" "user:user123:USER" "actuator:actuator123:ACTUATOR")
    
    for user_info in "${users[@]}"; do
        IFS=':' read -r username password roles <<< "$user_info"
        echo -e "\n--- Testing user: $username (roles: $roles) ---"
        
        # Test login
        login_response=$(curl -s -X POST "$base_url/api/login" \
            -H "Content-Type: application/json" \
            -d "{\"username\":\"$username\",\"password\":\"$password\"}")
        
        if echo "$login_response" | grep -q "token"; then
            echo -e "${GREEN}✓${NC} Login successful for $username"
            
            # Test protected endpoint
            test_endpoint "$base_url/api/protected" "$username:$password" 200 "Protected API access for $username"
            
            # Test environment info
            test_endpoint "$base_url/api/environment" "$username:$password" 200 "Environment info for $username"
        else
            echo -e "${RED}✗${NC} Login failed for $username"
        fi
    done
}

# Fonction pour générer un rapport détaillé
generate_security_report() {
    echo -e "\n${YELLOW}=== SECURITY CONFIGURATION REPORT ===${NC}"
    
    for port in 8080 8081 8082; do
        if curl -s -f "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
            env_info=$(curl -s "http://localhost:$port/api/environment" -u admin:admin123)
            profile=$(echo "$env_info" | grep -o '"activeProfile":"[^"]*"' | sed 's/"activeProfile":"\([^"]*\)"/\1/')
            
            echo -e "\n--- Environment: $profile (Port: $port) ---"
            echo "Security Policy:"
            
            case $profile in
                "uat")
                    echo "  • All actuator endpoints: PUBLIC ACCESS"
                    echo "  • Application endpoints: AUTHENTICATED ACCESS"
                    echo "  • Recommended for: Testing and Quality Assurance"
                    ;;
                "prod")
                    echo "  • Health/Info endpoints: PUBLIC ACCESS"
                    echo "  • Other actuator endpoints: ROLE_ACTUATOR required"
                    echo "  • Application endpoints: AUTHENTICATED ACCESS"
                    echo "  • Recommended for: Production deployment"
                    ;;
                "dev")
                    echo "  • Health endpoint: PUBLIC ACCESS"
                    echo "  • Other actuator endpoints: ROLE_ADMIN required"
                    echo "  • Application endpoints: AUTHENTICATED ACCESS"
                    echo "  • Recommended for: Development and debugging"
                    ;;
            esac
        fi
    done
}

# Fonction pour tester la charge et la performance
test_performance() {
    echo -e "\n${YELLOW}=== PERFORMANCE TESTS ===${NC}"
    
    for port in 8080 8081 8082; do
        if curl -s -f "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
            echo -e "\nTesting performance on port $port:"
            
            # Test de charge simple sur l'endpoint health
            start_time=$(date +%s%N)
            for i in {1..10}; do
                curl -s "http://localhost:$port/actuator/health" > /dev/null
            done
            end_time=$(date +%s%N)
            
            duration=$(((end_time - start_time) / 1000000))
            avg_time=$((duration / 10))
            
            echo "  • 10 requests to /health: ${avg_time}ms average"
            
            if [ $avg_time -lt 100 ]; then
                echo -e "  ${GREEN}✓${NC} Performance: Excellent"
            elif [ $avg_time -lt 500 ]; then
                echo -e "  ${YELLOW}!${NC} Performance: Good"
            else
                echo -e "  ${RED}!${NC} Performance: Needs attention"
            fi
        fi
    done
}

# Exécution du script
main "$@"

# Tests supplémentaires si demandés
if [ "$1" = "--detailed" ]; then
    for port in 8080 8081 8082; do
        if curl -s -f "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
            test_user_roles "http://localhost:$port" "port-$port"
        fi
    done
    generate_security_report
fi

if [ "$1" = "--performance" ]; then
    test_performance
fi

echo -e "\n${GREEN}Security testing completed!${NC}"
echo "Run with --detailed for comprehensive role testing"
echo "Run with --performance for basic performance metrics"
