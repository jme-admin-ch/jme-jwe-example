package ch.admin.bit.jme.jwe.web.ui;

import lombok.Builder;

@Builder
public record FrontendAuthConfigurationDto(String authority, String clientId, String applicationUrl) {
}
