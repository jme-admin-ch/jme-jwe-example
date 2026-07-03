import {inject} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {OAuthService} from 'angular-oauth2-oidc';
import {firstValueFrom} from 'rxjs';
import {CONTEXT_PATH} from '../shared/app-config';

interface FrontendAuthConfiguration {
	authority: string;
	clientId: string;
	applicationUrl: string;
}

/**
 * Loads the OIDC configuration from the backend (unencrypted, /ui-api is outside the JWE
 * included paths) and logs the user in via the authorization code flow with PKCE against the
 * OAuth mock server before the application starts.
 */
export const initializeAuth = (): Promise<boolean> => {
	const http = inject(HttpClient);
	const oauthService = inject(OAuthService);
	return firstValueFrom(http.get<FrontendAuthConfiguration>(`${CONTEXT_PATH}/ui-api/configuration/auth`)).then(config => {
		oauthService.configure({
			issuer: config.authority,
			clientId: config.clientId,
			redirectUri: document.baseURI,
			postLogoutRedirectUri: document.baseURI,
			responseType: 'code',
			scope: 'openid profile email',
			// The OAuth mock server runs on plain http://localhost
			requireHttps: false
		});
		return oauthService.loadDiscoveryDocumentAndLogin();
	});
};
