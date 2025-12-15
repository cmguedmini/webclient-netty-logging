
// src/test/java/com/example/security/TestUsersConfig.java
package com.example.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
public class TestUsersConfig {

    @Bean
    public UserDetailsService userDetailsService() {
        var alice = User.withUsername("alice").password("password").roles().build();
        var manager = User.withUsername("manager").password("password").roles("MANAGER").build(); // ROLE_MANAGER
        var gtx = User.withUsername("gtx").password("password").roles("gtx_tx_mge_cb").build();  // ROLE_gtx_tx_mge_cb
        return new InMemoryUserDetailsManager(alice, manager, gtx);
    }

    @Bean
    @SuppressWarnings("deprecation")
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance(); // OK en test
    }
}
