import {Injectable, inject} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {CONTEXT_PATH} from '../shared/app-config';
import {Person, PublicInfo} from './person.model';

/**
 * Plain Angular HttpClient calls — the jEAP JWE client interceptor transparently encrypts the
 * requests and decrypts the responses for everything matching the backend's included paths
 * (/api/**), while /api/public/** stays plain because it is on the backend's allowlist.
 */
@Injectable({providedIn: 'root'})
export class PersonService {
	private readonly http = inject(HttpClient);

	/**
	 * Encrypted GET: the interceptor adds the JWE-Response-Key header and Accept: application/jose,
	 * and decrypts the encrypted response back into a typed JSON body.
	 */
	getPersons(): Observable<Person[]> {
		return this.http.get<Person[]>(`${CONTEXT_PATH}/api/persons`);
	}

	/**
	 * Encrypted POST: the interceptor encrypts the JSON body as a compact JWE before it leaves
	 * the browser.
	 */
	createPerson(person: Person): Observable<Person> {
		return this.http.post<Person>(`${CONTEXT_PATH}/api/persons`, person);
	}

	/**
	 * Unencrypted request: /api/public/** is excluded from encryption by the allowlist on both
	 * sides, so this is a regular JSON request — but still authenticated with the Bearer token.
	 */
	getPublicInfo(): Observable<PublicInfo> {
		return this.http.get<PublicInfo>(`${CONTEXT_PATH}/api/public/info`);
	}
}
