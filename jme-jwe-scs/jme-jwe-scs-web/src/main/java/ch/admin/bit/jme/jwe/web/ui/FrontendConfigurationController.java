package ch.admin.bit.jme.jwe.web.ui;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves configuration information for the frontend. Lives under {@code /ui-api} on purpose:
 * the JWE filter only includes {@code /api/**}, so this endpoint stays unencrypted — the
 * frontend needs it before it can authenticate or encrypt anything.
 */
@RestController
@RequestMapping("/ui-api/configuration")
@RequiredArgsConstructor
public class FrontendConfigurationController {

    private final FrontendConfigProperties frontendConfigProperties;
    private final BuildProperties buildProperties;

    @GetMapping("/auth")
    public FrontendAuthConfigurationDto getConfiguration() {
        return FrontendAuthConfigurationDto.builder()
                .authority(frontendConfigProperties.getAuthority())
                .clientId(frontendConfigProperties.getClientId())
                .applicationUrl(frontendConfigProperties.getApplicationUrl())
                .build();
    }

    @GetMapping("/version")
    public String getVersion() {
        return buildProperties.getVersion();
    }
}
