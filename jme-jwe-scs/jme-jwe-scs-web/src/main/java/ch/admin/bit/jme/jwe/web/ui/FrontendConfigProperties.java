package ch.admin.bit.jme.jwe.web.ui;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * OIDC configuration served to the Angular frontend so that authority, client id and redirect
 * URL are configured on the backend per environment instead of being baked into the frontend
 * build.
 */
@Configuration
@ConfigurationProperties(prefix = "frontend.auth")
@Validated
@Getter
@Setter
public class FrontendConfigProperties {

    /**
     * Issuer URL of the OAuth2 authorization server (the OAuth mock server in this example).
     */
    @NotBlank
    private String authority;

    /**
     * OAuth2 client id the frontend uses for the authorization code flow with PKCE.
     */
    @NotBlank
    private String clientId;
}
