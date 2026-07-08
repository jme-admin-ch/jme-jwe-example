import {TestBed} from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {HttpErrorResponse} from '@angular/common/http';
import {MatButtonModule} from '@angular/material/button';
import {MatCardModule} from '@angular/material/card';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {TranslateModule} from '@ngx-translate/core';
import {of, throwError} from 'rxjs';
import {Decrypt} from './decrypt';
import {DecryptService} from './decrypt.service';
import {DecryptedJwe} from './decrypt.model';

describe('Decrypt', () => {
	const decrypted: DecryptedJwe = {
		header: {alg: 'RSA-OAEP-256', enc: 'A256GCM', kid: 'jme-jwe-scs-key:2', cty: 'application/json'},
		payloadText: '{"firstName":"Henriette","age":42,"active":true}',
		payloadBase64: null
	};
	let decryptService: {decrypt: jest.Mock};

	beforeEach(async () => {
		decryptService = {decrypt: jest.fn(() => of(decrypted))};
		await TestBed.configureTestingModule({
			declarations: [Decrypt],
			imports: [ReactiveFormsModule, MatCardModule, MatButtonModule, MatFormFieldModule, MatInputModule, TranslateModule.forRoot()],
			providers: [{provide: DecryptService, useValue: decryptService}]
		}).compileComponents();
	});

	function createComponent(): Decrypt {
		return TestBed.createComponent(Decrypt).componentInstance;
	}

	it('should split the pasted JWE into its five colored segments', () => {
		const component = createComponent();
		component.jweControl.setValue('eyJhbGciOi.encKey.iv.cipher.tag');
		expect(component.jweParts()).toEqual([
			{text: 'eyJhbGciOi', type: 'header'},
			{text: 'encKey', type: 'encryptedKey'},
			{text: 'iv', type: 'iv'},
			{text: 'cipher', type: 'ciphertext'},
			{text: 'tag', type: 'tag'}
		]);
	});

	it('should decrypt the trimmed JWE via the demo endpoint and expose the result', () => {
		const component = createComponent();
		component.jweControl.setValue('  ey.a.b.c.d  ');
		component.decrypt();
		expect(decryptService.decrypt).toHaveBeenCalledWith('ey.a.b.c.d', undefined);
		expect(component.result()).toEqual(decrypted);
		expect(component.error()).toBeNull();
	});

	it('should pass the JWE-Response-Key envelope along for response JWEs', () => {
		const component = createComponent();
		component.jweControl.setValue('ey.response.b.c.d');
		component.responseKeyControl.setValue('ey.envelope.b.c.d');
		component.decrypt();
		expect(decryptService.decrypt).toHaveBeenCalledWith('ey.response.b.c.d', 'ey.envelope.b.c.d');
	});

	it('should tokenize the decrypted JSON payload for syntax coloring', () => {
		const component = createComponent();
		component.jweControl.setValue('ey.a.b.c.d');
		component.decrypt();
		const tokens = component.payloadTokens();
		expect(tokens.find(token => token.text === '"firstName"')?.type).toBe('key');
		expect(tokens.find(token => token.text === '"Henriette"')?.type).toBe('string');
		expect(tokens.find(token => token.text === '42')?.type).toBe('number');
		expect(tokens.find(token => token.text === 'true')?.type).toBe('literal');
		expect(component.headerTokens().find(token => token.text === '"jme-jwe-scs-key:2"')?.type).toBe('string');
	});

	it('should not call the endpoint while the input is empty', () => {
		const component = createComponent();
		component.decrypt();
		expect(decryptService.decrypt).not.toHaveBeenCalled();
	});

	it('should ask for the response key envelope when a response JWE is pasted without it', () => {
		// The JWE interceptor requests with responseType "text", so the error body arrives unparsed
		decryptService.decrypt.mockReturnValue(
			throwError(
				() =>
					new HttpErrorResponse({
						status: 400,
						error: '{"reason":"RESPONSE_KEY_REQUIRED","message":"Supply the JWE-Response-Key header","header":{"alg":"dir","enc":"A256GCM"}}'
					})
			)
		);
		const component = createComponent();
		component.jweControl.setValue('ey.a.b.c.d');
		component.decrypt();
		expect(component.result()).toBeNull();
		expect(component.error()?.reason).toBe('RESPONSE_KEY_REQUIRED');
		expect(component.needsResponseKey()).toBe(true);
		expect(component.errorHeaderTokens().find(token => token.text === '"dir"')?.type).toBe('string');
	});
});
