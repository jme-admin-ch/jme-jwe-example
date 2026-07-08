import {Injectable, inject} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {ObHttpApiInterceptorEvents} from '@oblique/oblique';
import {Observable} from 'rxjs';
import {CONTEXT_PATH} from '../shared/app-config';
import {DecryptedJwe} from './decrypt.model';

/**
 * Demo-only decrypt call. /api/demo/** lies within the JWE filter's included paths, so this
 * request is itself transparently encrypted by the jEAP JWE interceptor: the pasted JWE travels
 * wrapped in an encrypted request body, and the response with the plaintext comes back encrypted
 * with the request-local response key — the decrypted content never crosses the wire in plaintext.
 */
@Injectable({providedIn: 'root'})
export class DecryptService {
	private readonly http = inject(HttpClient);
	private readonly interceptorEvents = inject(ObHttpApiInterceptorEvents);

	/**
	 * Response JWEs (alg "dir") additionally need the JWE-Response-Key header value of the request
	 * they belong to, so the backend can recover the request-local CEK from the envelope.
	 */
	decrypt(jwe: string, responseKeyEnvelope?: string): Observable<DecryptedJwe> {
		// Protocol failures (e.g. a response JWE pasted without its envelope) are part of the
		// demo and displayed inline — suppress Oblique's global error toast for this call.
		this.interceptorEvents.deactivateNotificationOnNextAPICalls(1);
		return this.http.post<DecryptedJwe>(`${CONTEXT_PATH}/api/demo/decrypt`, {jwe, responseKeyEnvelope: responseKeyEnvelope ?? null});
	}
}
