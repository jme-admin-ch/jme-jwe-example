package ch.admin.bit.jme.jwe.web.ui;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Configuration
public class FrontendConfigurationControllerSecurityConfig {

    /**
     * Permits unauthenticated GET access to the frontend configuration endpoints — the frontend
     * has to be able to read the auth configuration before the user can log in.
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 12)
    SecurityFilterChain configSecurityFilterChain(HttpSecurity http) {
        http.securityMatcher(PathPatternRequestMatcher.withDefaults()
                .matcher(HttpMethod.GET, "/ui-api/configuration/**"));
        http.authorizeHttpRequests(authorizeHttpRequests ->
                authorizeHttpRequests.anyRequest().permitAll());
        return http.build();
    }
}
