package ch.admin.bit.jme.jwe.web.config;

import ch.admin.bit.jme.jwe.web.filter.SpaWebFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
public class WebSecurityConfig {

    /**
     * Serves the Angular frontend without authentication: everything that is not {@code /api/**}
     * or {@code /ui-api/**} (static resources, SPA routes, the JWE discovery endpoints) is public.
     * Requests to {@code /api/**} fall through to the OAuth2 resource server chain contributed by
     * the jeap security starter, and {@code /ui-api/**} is handled by the dedicated frontend
     * configuration chain (or the resource server chain for anything it does not permit).
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 11)
    public SecurityFilterChain uiSecurityFilterChain(HttpSecurity http) {
        RequestMatcher apiMatcher = new OrRequestMatcher(
                PathPatternRequestMatcher.pathPattern("/api/**"),
                PathPatternRequestMatcher.pathPattern("/ui-api/**"));

        http.securityMatcher(new NegatedRequestMatcher(apiMatcher))
                .authorizeHttpRequests(authorizeHttpRequests ->
                        authorizeHttpRequests.anyRequest().permitAll())
                .addFilterAfter(new SpaWebFilter(), AnonymousAuthenticationFilter.class);

        http.headers(headers ->
                headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));

        return http.build();
    }
}
