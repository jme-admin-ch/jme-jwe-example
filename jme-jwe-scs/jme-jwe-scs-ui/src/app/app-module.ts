import {LOCALE_ID, NgModule, provideAppInitializer, provideBrowserGlobalErrorListeners, provideZoneChangeDetection} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';

import {AppRoutingModule} from './app-routing-module';
import {App} from './app';
import {ObButtonModule, ObExternalLinkModule, ObHttpApiInterceptor, ObMasterLayoutModule, provideObliqueConfiguration} from '@oblique/oblique';
import {registerLocaleData} from '@angular/common';
import localeDECH from '@angular/common/locales/de-CH';
import localeFRCH from '@angular/common/locales/fr-CH';
import localeITCH from '@angular/common/locales/it-CH';
import {TranslateModule} from '@ngx-translate/core';
import {HTTP_INTERCEPTORS, provideHttpClient, withInterceptors, withInterceptorsFromDi} from '@angular/common/http';
import {jeapJweInterceptor, provideJeapJweClient} from '@jeap/jeap-jwe-client';
import {OAuthModule} from 'angular-oauth2-oidc';
import {ReactiveFormsModule} from '@angular/forms';
import {Home} from './home/home';
import {MatButtonModule} from '@angular/material/button';
import {MatCardModule} from '@angular/material/card';
import {MatIconModule} from '@angular/material/icon';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {initializeAuth} from './auth/auth-initializer';
import {CONTEXT_PATH} from './shared/app-config';

registerLocaleData(localeDECH);
registerLocaleData(localeFRCH);
registerLocaleData(localeITCH);

@NgModule({
	declarations: [App, Home],
	imports: [
		BrowserModule,
		AppRoutingModule,
		ObMasterLayoutModule,
		ObButtonModule,
		TranslateModule,
		ReactiveFormsModule,
		MatButtonModule,
		MatCardModule,
		MatIconModule,
		MatFormFieldModule,
		MatInputModule,
		ObExternalLinkModule,
		// Attaches the OAuth2 Bearer token to all API requests
		OAuthModule.forRoot({
			resourceServer: {
				allowedUrls: [`${CONTEXT_PATH}/api`],
				sendAccessToken: true
			}
		})
	],
	providers: [
		provideBrowserGlobalErrorListeners(),
		provideZoneChangeDetection({eventCoalescing: true}),
		{provide: LOCALE_ID, useValue: 'de-CH'},
		provideObliqueConfiguration({
			accessibilityStatement: {
				applicationName: 'JWE Example',
				conformity: 'none',
				createdOn: new Date('2026-07-03'),
				applicationOperator: 'Federal Office of Information Technology, Systems and Telecommunication FOITT',
				contact: [{email: 'jeap-community@bit.admin.ch'}]
			},
			hasLanguageInUrl: false
		}),
		// Transparent JWE encryption: the backend publishes its JWE configuration (JWKS path,
		// included/excluded paths) at /.well-known/jwe-configuration, the interceptor mirrors it.
		provideJeapJweClient({
			origin: globalThis.location.origin,
			jweConfigPath: `${CONTEXT_PATH}/.well-known/jwe-configuration`
		}),
		{provide: HTTP_INTERCEPTORS, useClass: ObHttpApiInterceptor, multi: true},
		provideHttpClient(withInterceptors([jeapJweInterceptor]), withInterceptorsFromDi()),
		provideAppInitializer(initializeAuth)
	],
	bootstrap: [App]
})
export class AppModule {}
